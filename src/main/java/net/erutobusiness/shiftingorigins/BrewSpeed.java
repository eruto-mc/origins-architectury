package net.erutobusiness.shiftingorigins;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;

/**
 * 聖職者の近くでは、醸造台が2倍で動く（2026-09-09・あなたの決定）。
 *
 * <p><b>なぜ醸造なのか</b>: 聖職者の他の2つ（「大釜の仕上げ」と「分かち合う祈り」）は
 * <b>できたポーションの話</b>で、⚠ <b>作る側は誰も持っていなかった</b>。
 * ⚠ 同じ日に「エンチャントの心得」を司書へ移したので、
 * <b>聖職者はポーション一本の職業</b>になる。
 *
 * <h2>火を3つの職業で分ける</h2>
 *
 * <ul>
 *   <li>調理台・燻製器・焚火 … 料理人（{@link CookSpeed}）</li>
 *   <li>かまど・溶鉱炉 … 鍛冶屋（{@link FurnaceSpeed}）</li>
 *   <li><b>醸造台 … 聖職者（ここ）</b></li>
 * </ul>
 *
 * <p>⚠ 仕掛けは3つとも同じ——<b>tick をもう1回回す</b>
 * （{@code mixin/BoundTickingBlockEntityMixin}）。⚠ <b>新しい seam を増やさない。</b>
 *
 * <h2>⚠ 先に確かめたこと（2026-09-09）</h2>
 *
 * <p>醸造台のクラスを持つ jar を4本見つけたので、<b>4本とも中身を読んだ</b>。
 * ⚠ <b>進み方を変えているものは1本も無かった</b>:
 *
 * <ul>
 *   <li>Jade … 当たり 0（表示だけ）</li>
 *   <li>Pehkui … 「使える距離」の互換 mixin 2件。醸造とは無関係</li>
 *   <li>amendments … <b>見た目の更新だけ</b>（{@code sendBlockUpdated} と更新パケット）</li>
 *   <li>⚠⚠ canary … <b>醸造台を眠らせる</b>。⚠ ただし条件は {@code brewTime == 0}——
 *       <b>醸造していないときだけ</b>眠る。⚠ 醸造中は起きているので、
 *       もう1回回す道はふさがっていない。⚠ 眠っている台は {@code brewTime == 0}＝
 *       <b>速くする対象でもない</b></li>
 * </ul>
 *
 * <h2>⚠ 「勢い」の打ち消しは要らない</h2>
 *
 * <p>料理人の {@link CookSpeed#keepMomentum} は、混ぜ鉢のように
 * <b>勢いが0になると累計が0に戻る</b>台のための打ち消し。
 * ⚠ 醸造台は {@code brewTime} を1ずつ減らすだけで、そういう作りではないので要らない
 * （バニラの {@code serverTick} を読んで確かめた）。
 *
 * <p>⚠ <b>燃料（ブレイズパウダー）の減りは変わらない。</b> 燃料は醸造を始めるときに
 * 1つ減る作りで、{@code brewTime} の進みとは別。⚠ <b>速くなるのは時間だけ。</b>
 */
public final class BrewSpeed {

  private BrewSpeed() {
  }

  /** ⚠ 料理人・鍛冶屋の台と同じ 8 ブロック。⚠ 3つの能力で数字を割らない。 */
  private static final double RANGE = 8.0D;

  /**
   * その醸造台を速くしてよいか。
   *
   * <p>⚠ <b>近くに聖職者が1人でも居れば効く</b>（本人の台でなくてよい）。
   * ⚠ 料理人・鍛冶屋と同じ考え方で、⚠ <b>聖職者が居る拠点の醸造台がまとめて速くなる。</b>
   *
   * <p>⚠ 判定の順は「安い順」——毎tick・全ブロックエンティティで呼ばれるので、
   * クラスの照合を先に置き、プレイヤーの走査は最後にする。
   */
  public static boolean shouldSpeedUp(final BlockEntity be) {

    if (!(be instanceof BrewingStandBlockEntity)
        || be.getLevel() == null || be.getLevel().isClientSide) {
      return false;
    }
    if (!ShiftingOrigins.Config.CLERIC_BREW_SPEED.get()) {
      return false;
    }
    final ServerLevel level = (ServerLevel) be.getLevel();
    final double x = be.getBlockPos().getX() + 0.5D;
    final double y = be.getBlockPos().getY() + 0.5D;
    final double z = be.getBlockPos().getZ() + 0.5D;
    for (final ServerPlayer sp : level.players()) {
      if (sp.distanceToSqr(x, y, z) <= RANGE * RANGE && ClassPowers.isCleric(sp)) {
        return true;
      }
    }
    return false;
  }
}
