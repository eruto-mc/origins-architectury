package net.erutobusiness.shiftingorigins;

import dev.limonblaze.originsclasses.common.registry.OriginsClassesPowers;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.BonemealEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 木こりが苗木へ骨粉を使うと、1個で必ず木になる（2026-09-05・あなたの判断）。
 *
 * <p>⚠ <b>バニラの苗木</b>（{@code SaplingBlock}・実物を読んで確認）:
 *
 * <pre>
 *   isBonemealSuccess … level.random.nextFloat() &lt; 0.45   ← 骨粉1個につき45%
 *   performBonemeal   … advanceTree（STAGE 0→1、1なら木を生やす）
 * </pre>
 *
 * <p>つまり<b>成功2回ぶんが要り、平均およそ4.4個</b>かかる。ここでは木こりに限って
 * <b>その45%の抽選を通さず</b>、苗木でなくなるまで {@code performBonemeal} を回す。
 *
 * <p>⚠⚠ <b>なぜ既存の型で書けないか</b>: 農家の「施肥技術」が使う
 * {@code origins_classes:modify_bone_meal} は {@code ModifyValuePower} ＝
 * <b>骨粉の適用回数に倍率をかけるだけ</b>で、対象ブロックの条件を持たない
 * （{@code PowerEventHandler.onBoneMeal}）。「苗木のときだけ」が書けない。
 *
 * <p>⚠ 既製品も探した（2026-09-05・Modrinth と CurseForge）。Extended Bone Meal /
 * Sneaky Tree Growing / Universal Bone Meal / Fertilization はどれも
 * <b>全員に等しく当たる</b>作りで、職業で分ける口が無かった。
 *
 * <p><b>骨粉の消費と粒子はバニラに任せる</b>: {@code Result.ALLOW} を返すと
 * {@code ForgeEventFactory.onApplyBonemeal} が 1 を返して {@code stack.shrink(1)} し、
 * {@code BoneMealItem.useOn} が {@code levelEvent(1505)} で粒子と音を出す
 * （Forge の実装を読んで確認）。⚠ 自前で消費や粒子を書くと二重になる。
 *
 * <p>⚠ <b>1本も育たなかったときは何もしない</b>（{@code ALLOW} を返さない）。
 * 骨粉だけ消えて何も起きない、を避けるため。空が塞がっていて木になれない場合は、
 * 段階が1つ上がったところで止まり、そこまでで骨粉1個ぶんを消費する。
 */
public final class SaplingBonemeal {

  private SaplingBonemeal() {
  }

  @SubscribeEvent
  public static void onBonemeal(final BonemealEvent event) {

    if (!ShiftingOrigins.Config.SAPLING_BONEMEAL.get()
        || !(event.getEntity() instanceof ServerPlayer player)
        || !(event.getLevel() instanceof ServerLevel level)
        || !event.getBlock().is(BlockTags.SAPLINGS)) {
      return;
    }
    // ⚠⚠ **骨粉の回数を変える power を持っている人には手を出さない。**
    //   Origins Classes 側の処理は EventPriority.LOWEST ＝ こちらの後に走り、
    //   ⚠ **イベントに載っている「使った瞬間の状態」を持ったまま** performBonemeal を呼ぶ。
    //   こちらが先に木へ変えていると、その古い状態で苗木を書き戻してしまう。
    if (IPowerContainer.hasPower(player, OriginsClassesPowers.MODIFY_BONE_MEAL.get())
        || !ClassPowers.isLumberjack(player)) {
      return;
    }
    if (grow(level, event.getPos())) {
      event.setResult(Event.Result.ALLOW);
    }
  }

  /**
   * 苗木でなくなるまで育てる。1段でも進んだら true。
   *
   * <p>⚠ 抜け道を2つ置いてある。① 状態が変わらなくなったら止める
   * （木になる場所が無いと {@code advanceTree} は何もしないので、これが無いと回り続ける）。
   * ② 回数の上限（config）。⚠ {@code BlockState} は組み合わせごとに1つしか無いので、
   * {@code ==} で「変わっていない」が判定できる。
   *
   * <p>⚠ {@code SaplingBlock} 決め打ちにしない。{@code #minecraft:saplings} には
   * MOD の苗木も入っており（実物の jar で確認: 檸檬・楓・山査子・アボカド・林檎・松ほか）、
   * それらは {@code SaplingBlock} とは限らない。{@code BonemealableBlock} の口だけを使う。
   */
  private static boolean grow(final ServerLevel level, final BlockPos pos) {

    final int limit = ShiftingOrigins.Config.SAPLING_MAX_STEPS.get();
    boolean advanced = false;

    for (int step = 0; step < limit; step++) {
      final BlockState state = level.getBlockState(pos);
      if (!state.is(BlockTags.SAPLINGS)
          || !(state.getBlock() instanceof BonemealableBlock target)
          || !target.isValidBonemealTarget(level, pos, state, false)) {
        break;
      }
      target.performBonemeal(level, level.random, pos, state);
      if (level.getBlockState(pos) == state) {
        break;
      }
      advanced = true;
    }
    return advanced;
  }
}
