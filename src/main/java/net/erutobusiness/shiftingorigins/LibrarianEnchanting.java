package net.erutobusiness.shiftingorigins;

import dev.limonblaze.originsclasses.common.registry.OriginsClassesPowers;
import dev.limonblaze.originsclasses.util.CommonUtils;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.enchanting.EnchantmentLevelSetEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 司書の「エンチャントの心得」を、どのエンチャントテーブルでも働かせる。
 *
 * <p>⚠⚠ <b>上流の仕掛けは、この世界では最初から一度も走っていなかった。</b>
 * 上流は2段構え——{@code EnchantmentMenu.slotsChanged} が
 * <b>エンチャントする人の UUID を道具の NBT へ書き</b>、
 * {@code EnchantmentLevelSetEvent} が<b>それを読んで</b>人を引き当てる
 * （イベントが人を持っていないため）。
 *
 * <p>⚠ <b>Easy Magic の {@code ModEnchantmentMenu} が {@code slotsChanged} を自分で宣言し、
 * Forge のコードを1つも呼んでいない</b>（定数プールに forge を含む参照が0件・実物で確認）。
 * ⚠⚠ そして {@code EnchantmentLevelSetEvent} を発火しているのは Forge 全体で
 * {@code EnchantmentMenu.java:123} の<b>1か所だけ</b>なので、
 * ⚠ <b>印が書かれないどころか、イベントそのものが飛ばない。</b>
 *
 * <p><b>やり方</b>: ⚠ <b>特定の MOD を名指ししない。</b>
 * 開いている画面が {@code EnchantmentMenu}（差し替え版もこれを継承する）なら、
 * その {@code costs} を毎tick見て、<b>まだ手を入れていない値なら</b>書き換える。
 *
 * <p>⚠⚠ <b>なぜ mixin にしないか</b>: 相手の MOD のクラスを {@code @Mixin(targets=…)} で
 * 狙うと、⚠ <b>コンパイル時にその MOD が依存に必要</b>になる（2026-09-06 にビルドが落ちた）。
 * ⚠ Easy Magic を依存へ足すと、<b>その MOD を抜いた日にビルドが落ちる</b>。
 * ⚠ こちらは vanilla の型しか触らないので、台を差し替える MOD が変わっても壊れない。
 *
 * <p>⚠ <b>実際に付くエンチャントも正しく上がる。</b> 押した時点の {@code costs} を
 * {@code EnchantmentMenu.clickMenuButton} が読むので、書き換えた値がそのまま使われる
 * （Easy Magic はこのメソッドを上流へ委ねている・定数プールで確認）。
 *
 * <p>⚠⚠ <b>予告も作り直す（2026-09-06 追加）。</b> それまでは<b>必要レベルだけ上がって、
 * 予告は上げる前の数字のまま</b>出ていた（台は costs を決めた直後に予告を作り、
 * こちらはその後で書き換えるため）。⚠ <b>部員から「40 の内容がよくなってるように見えない」
 * と報告が来て分かった。</b> 作り直しは {@link #retellClues} を見ること。
 */
public final class LibrarianEnchanting {

  private LibrarianEnchanting() {
  }

  /** ⚠ バニラの {@code EnchantmentMenu.stillValid} と同じ範囲（8ブロック＝64を平方で見る）。 */
  private static final double REACH_SQR = 64.0D;

  /**
   * ⚠ 上流の経路（イベント）が走った時刻を人ごとに覚える。
   *
   * <p>⚠⚠ <b>二重に掛けないための印。</b> Easy Magic を外した日や、
   * 上流の台に戻した日には<b>イベントが飛ぶ</b>ので、そのときは向こうに任せる。
   */
  private static final Map<UUID, Long> EVENT_SEEN = new HashMap<>();

  /**
   * こちらが書き換えた直後の {@code costs} の写し。
   *
   * <p>⚠⚠ <b>これが無いと毎tick掛け続けて青天井になる。</b>
   * 台が計算し直すと値が変わるので、<b>写しと違うときだけ</b>手を入れる。
   */
  private static final Map<UUID, int[]> LAST_APPLIED = new HashMap<>();

  /**
   * いまその画面を開いている人。
   *
   * <p>⚠⚠ <b>{@code EnchantmentMenu.getEnchantmentList} の中では人が引けない</b>——
   * vanilla のメニューは人を持たないし、Easy Magic が持っている {@code player} は
   * ⚠ <b>あちらの private</b>。だから<b>毎tickここで控えておく</b>。
   *
   * <p>⚠ {@link java.util.WeakHashMap} で持つ——画面が閉じれば勝手に消える。
   */
  private static final Map<EnchantmentMenu, ServerPlayer> OPENERS = new java.util.WeakHashMap<>();

  /** 上流の経路が生きている世界のための口（バニラの台・Apotheosis など）。 */
  @SubscribeEvent(priority = EventPriority.LOWEST)
  public static void onEnchantmentLevel(final EnchantmentLevelSetEvent event) {

    if (!ShiftingOrigins.Config.LIBRARIAN_ENCHANTING.get()
        || !(event.getLevel() instanceof ServerLevel level)) {
      return;
    }
    // ⚠ 上流が印を書けているなら、そちらが同じことをする。触らない。
    final ItemStack stack = event.getItem();
    if (stack.hasTag() && CommonUtils.getOriginsClassesTag(stack.getTag())
        .contains(CommonUtils.ENCHANTER, Tag.TAG_INT_ARRAY)) {
      return;
    }
    final ServerPlayer player = enchanterAt(level, event.getPos());
    if (player == null) {
      return;
    }
    EVENT_SEEN.put(player.getUUID(), level.getGameTime());
    event.setEnchantLevel(modify(player, event.getEnchantLevel()));
  }

  /**
   * イベントが飛ばない台のための口。
   *
   * <p>⚠ 開いている画面を見るだけなので、⚠ <b>どの MOD が台を差し替えていても通る</b>。
   */
  @SubscribeEvent
  public static void onPlayerTick(final TickEvent.PlayerTickEvent event) {

    if (event.phase != TickEvent.Phase.END
        || !ShiftingOrigins.Config.LIBRARIAN_ENCHANTING.get()
        || !(event.player instanceof ServerPlayer player)) {
      return;
    }
    if (!(player.containerMenu instanceof EnchantmentMenu menu)) {
      LAST_APPLIED.remove(player.getUUID());
      return;
    }
    // ⚠ 「この画面を開いているのは誰か」を控える（`getEnchantmentList` の中で引くため）
    OPENERS.put(menu, player);
    // ⚠ 上流の経路が同じ画面を握っているなら、こちらは何もしない（二重に掛けない）
    final Long seen = EVENT_SEEN.get(player.getUUID());
    if (seen != null && player.level().getGameTime() - seen < 40L) {
      return;
    }
    final int[] costs = menu.costs;
    if (costs == null || Arrays.equals(costs, LAST_APPLIED.get(player.getUUID()))) {
      return;
    }
    boolean touched = false;
    for (int i = 0; i < costs.length; i++) {
      if (costs[i] > 0) {
        final int raised = modify(player, costs[i]);
        if (raised != costs[i]) {
          costs[i] = raised;
          touched = true;
        }
      }
    }
    if (touched) {
      retellClues(menu, costs);
      LAST_APPLIED.put(player.getUUID(), costs.clone());
    }
  }

  /**
   * 画面に出る「予告」を、上げた後の数字で作り直す。
   *
   * <p>⚠⚠ <b>これが無いと、部員には強くなったことが見えない。</b> 台は
   * <b>costs を決める → 予告を作る → 送る</b> の順で動き（Easy Magic の
   * {@code slotsChanged} を逆アセンブルして確認: {@code updateLevels} → {@code createClues}
   * → {@code sendEnchantingData}）、⚠ <b>こちらが costs を上げるのはその後</b>。
   * ⚠ だから<b>必要レベルだけ 40 になり、予告は 30 のときのまま</b>出ていた。
   * ⚠ 部員の報告「40 の内容がよくなってるようには見えない」はこれ。
   *
   * <p>⚠ <b>作り方はバニラの {@code getEnchantmentList} を1命令ずつ写した</b>
   * （79 バイトの中身を逆アセンブルした）——⚠ <b>種を {@code 種 + 段} で置き直し、
   * {@code selectEnchantment} を呼び、本なら1つ抜き、残りから1つ選ぶ</b>。
   * ⚠⚠ <b>押したときに実際に走るのはこのバニラの経路</b>（Easy Magic は再抽選のときだけ
   * 自分で処理し、エンチャント本体は上流へ渡している）。⚠ <b>予告と結果がずれない。</b>
   *
   * <p>⚠ 乱数は<b>自前のものを同じ種で回す</b>。台の {@code random} は private なので触れず、
   * ⚠ 種を置き直す作りのおかげで<b>前の状態に依らず同じ並びになる</b>。
   *
   * <p>⚠ 書き込む先はバニラの欄（{@code enchantClue} / {@code levelClue}）。
   * ⚠ Easy Magic も<b>同じ欄へ書いている</b>ことを確認済み（`f_39447_` / `f_39448_`）。
   */
  private static void retellClues(final EnchantmentMenu menu, final int[] costs) {

    final ItemStack stack = menu.getSlot(0).getItem();
    if (stack.isEmpty()) {
      return;
    }
    for (int i = 0; i < costs.length; i++) {
      if (costs[i] <= 0) {
        continue;
      }
      final RandomSource rng = RandomSource.create();
      rng.setSeed(menu.getEnchantmentSeed() + i);
      final List<EnchantmentInstance> list =
          EnchantmentHelper.selectEnchantment(rng, stack, costs[i], false);
      if (stack.is(Items.BOOK) && list.size() > 1) {
        // ⚠⚠ **司書は捨てない。** ⚠ ただし<b>乱数は必ず1回消費する</b>——
        //    消費しないと、この後の `list.get(rng.nextInt(...))` から先で
        //    ⚠ <b>予告と結果の乱数の並びがずれる</b>（当部は一度そのずれを踏んでいる）。
        final int victim = rng.nextInt(list.size());
        if (!keepsBookEnchantment(menu)) {
          list.remove(victim);
        }
      }
      if (list.isEmpty()) {
        continue;
      }
      final EnchantmentInstance picked = list.get(rng.nextInt(list.size()));
      menu.enchantClue[i] = BuiltInRegistries.ENCHANTMENT.getId(picked.enchantment);
      menu.levelClue[i] = picked.level;
    }
  }

  /** 退出したら覚えている分を捨てる。 */
  @SubscribeEvent
  public static void onLogout(final net.minecraftforge.event.entity.player.PlayerEvent
      .PlayerLoggedOutEvent event) {
    EVENT_SEEN.remove(event.getEntity().getUUID());
    LAST_APPLIED.remove(event.getEntity().getUUID());
  }

  /**
   * その画面で、本のエンチャントを1つ捨てずに済むか（＝開いているのが司書か）。
   *
   * <p>⚠⚠ <b>バニラは本のときだけ抽選から1つ捨てている</b>
   * （{@code EnchantmentMenu.getEnchantmentList} の
   * {@code if (stack.is(Items.BOOK) && list.size() > 1) list.remove(...)}。
   * 1.20.1 の本体を逆アセンブルして確かめた）。⚠ 司書はそれを捨てない。
   *
   * <p>⚠ 呼ぶのは2か所——{@link #retellClues}（予告）と
   * {@code mixin/EnchantmentMenuBookMixin}（結果）。⚠ <b>両方が同じ答えを使う</b>ので、
   * ⚠⚠ <b>予告と結果がずれない。</b>
   */
  public static boolean keepsBookEnchantment(final EnchantmentMenu menu) {

    if (!ShiftingOrigins.Config.LIBRARIAN_BOOK_KEEP.get()) {
      return false;
    }
    final ServerPlayer player = OPENERS.get(menu);
    return player != null && ClassPowers.isLibrarian(player);
  }

  /** ⚠ 倍率の計算は1か所だけ（2つの口で数字が割れないように）。 */
  private static int modify(final ServerPlayer player, final int level) {
    return Mth.floor(IPowerContainer.modify(
        player,
        OriginsClassesPowers.MODIFY_ENCHANTING_LEVEL.get(),
        level,
        cp -> cp.get().isActive(player)));
  }

  /**
   * その台を開いている人を1人だけ返す（イベントの側は座標しか持たないため）。
   *
   * <p>⚠ 条件は2つ——<b>エンチャントの画面を開いている</b>ことと、<b>台から8ブロック以内</b>。
   */
  private static ServerPlayer enchanterAt(final ServerLevel level, final BlockPos pos) {

    ServerPlayer best = null;
    double bestSqr = REACH_SQR;
    for (final ServerPlayer player : level.players()) {
      if (!(player.containerMenu instanceof EnchantmentMenu)) {
        continue;
      }
      final double d = player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D,
          pos.getZ() + 0.5D);
      if (d <= bestSqr) {
        bestSqr = d;
        best = player;
      }
    }
    return best;
  }
}
