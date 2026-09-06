package net.erutobusiness.shiftingorigins;

import dev.limonblaze.originsclasses.common.registry.OriginsClassesPowers;
import dev.limonblaze.originsclasses.util.CommonUtils;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.enchanting.EnchantmentLevelSetEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 聖職者の「エンチャントの心得」を、エンチャント台を差し替える MOD が入っていても働かせる
 * （2026-09-05）。
 *
 * <p>⚠⚠ <b>上流の仕掛けは2段になっている</b>:
 *
 * <ol>
 *   <li>{@code EnchantmentMenuMixin} が {@code EnchantmentMenu.slotsChanged} に差し込み、
 *       <b>エンチャントする人の UUID を、置かれた道具の NBT に書く</b>
 *       （{@code origins_classes.Enchanter}）</li>
 *   <li>{@code PowerEventHandler.onEnchantmentLevel} が {@code EnchantmentLevelSetEvent} で
 *       <b>その UUID を読んで</b>人を引き当て、{@code modify_enchanting_level} を掛ける</li>
 * </ol>
 *
 * <p>⚠ {@code EnchantmentLevelSetEvent} は<b>誰がエンチャントしているかを持っていない</b>
 * （持っているのは世界・座標・列・本棚の数・道具だけ）。だから①が要る。
 *
 * <p>⚠⚠ <b>Easy Magic の {@code ModEnchantmentMenu} が {@code slotsChanged}（SRG
 * {@code m_6199_}）を自分で宣言し、{@code EnchantmentMenu.m_6199_} を呼んでいない</b>
 * （定数プールを読んで確認。親を呼んでいるのは {@code <init>}・{@code m_6366_}・
 * {@code m_7648_} の3つだけ）。→ <b>①が一度も走らないので、②は毎回そのまま返る。</b>
 * ＝ 聖職者の能力は在るのに何も起きていなかった。
 *
 * <p><b>やり方</b>: ⚠ <b>Easy Magic の中へ手を入れない。</b>
 * 台の座標から<b>その台を開いている人</b>を引く。
 * ⚠ こうすると、エンチャント台を差し替える MOD が今後変わっても壊れない
 * （上流は Apotheosis 用に同じ差し込みをもう1本持っており、⚠ <b>MOD ごとに1本ずつ
 * 書き足す形</b>になっている。当部はその形を採らない）。
 *
 * <p>⚠ <b>二重に掛けない。</b> 道具に印が書かれていたら上流が働けているので、こちらは何もしない
 * （Easy Magic を外した日や、Apotheosis を入れた日に自動で引き下がる）。
 */
public final class ClericEnchanting {

  private ClericEnchanting() {
  }

  /** ⚠ バニラの {@code EnchantmentMenu.stillValid} と同じ範囲（8ブロック＝64を平方で見る）。 */
  private static final double REACH_SQR = 64.0D;

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
    event.setEnchantLevel(Mth.floor(IPowerContainer.modify(
        player,
        OriginsClassesPowers.MODIFY_ENCHANTING_LEVEL.get(),
        event.getEnchantLevel(),
        cp -> cp.get().isActive(player))));
  }

  /**
   * その台を開いている人を1人だけ返す。
   *
   * <p>⚠ 条件は2つ——<b>エンチャントの画面を開いている</b>ことと、
   * <b>台から8ブロック以内</b>にいること。⚠ 差し替えられた画面も
   * {@code EnchantmentMenu} を継承しているので、この判定は器の実装に依らない。
   *
   * <p>⚠ 2人が同じ台に届く所に居たら<b>近いほう</b>を採る。
   * ⚠ バニラの台は同時に1人しか開けない（開いた人が容器を握る）ので、
   * これで取り違えるのは「隣にもう1台在って両方開いている」ような場合だけ。
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
