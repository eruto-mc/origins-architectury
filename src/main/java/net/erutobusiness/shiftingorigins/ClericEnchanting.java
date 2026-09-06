package net.erutobusiness.shiftingorigins;

import dev.limonblaze.originsclasses.common.registry.OriginsClassesPowers;
import dev.limonblaze.originsclasses.util.CommonUtils;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.enchanting.EnchantmentLevelSetEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 聖職者の「エンチャントの心得」を、どのエンチャント台でも働かせる（2026-09-06）。
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
 * <p>⚠ <b>候補の予告だけは元の段階のまま出ることがある。</b>
 * 予告（{@code enchantClue}）は台が {@code costs} を決めた直後に作られ、
 * こちらはその後で書き換えるため。⚠ <b>必要レベルの数字と、実際に付く物は正しい。</b>
 */
public final class ClericEnchanting {

  private ClericEnchanting() {
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

  /** 上流の経路が生きている世界のための口（バニラの台・Apotheosis など）。 */
  @SubscribeEvent(priority = EventPriority.LOWEST)
  public static void onEnchantmentLevel(final EnchantmentLevelSetEvent event) {

    if (!ShiftingOrigins.Config.CLERIC_ENCHANTING.get()
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
        || !ShiftingOrigins.Config.CLERIC_ENCHANTING.get()
        || !(event.player instanceof ServerPlayer player)) {
      return;
    }
    if (!(player.containerMenu instanceof EnchantmentMenu menu)) {
      LAST_APPLIED.remove(player.getUUID());
      return;
    }
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
      LAST_APPLIED.put(player.getUUID(), costs.clone());
    }
  }

  /** 退出したら覚えている分を捨てる。 */
  @SubscribeEvent
  public static void onLogout(final net.minecraftforge.event.entity.player.PlayerEvent
      .PlayerLoggedOutEvent event) {
    EVENT_SEEN.remove(event.getEntity().getUUID());
    LAST_APPLIED.remove(event.getEntity().getUUID());
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
