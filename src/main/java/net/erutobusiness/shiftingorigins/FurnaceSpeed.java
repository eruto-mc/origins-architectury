package net.erutobusiness.shiftingorigins;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SmokerBlockEntity;

/**
 * 鍛冶屋の近くでは、かまどと溶鉱炉が2倍で動く（2026-09-06・あなたの決定「D-1」）。
 *
 * <p><b>なぜ炉なのか</b>: ⚠ <b>クラフトさせる能力は筋が悪い</b>（あなたの判断。
 * 剣を作るのは既に皆やっているので、そこを厚くしても効きが薄い）。
 * ⚠ 鉱石を焼くのは<b>掘ったあとに必ず通る道</b>なので、⚠ <b>遊ぶほど効く。</b>
 *
 * <p>⚠ <b>ここは空いていた</b>（実測）——当部の全 power のうち精錬に触るのは
 * 「燻製の心得」（料理人・燻製器の経験値）だけ。⚠ <b>かまどと溶鉱炉は誰も持っていない。</b>
 * ⚠ 溶鉱炉を速くする MOD も入っていない。
 *
 * <h2>⚠⚠ 燻製器は取らない</h2>
 *
 * <p>⚠ {@link SmokerBlockEntity} は {@link AbstractFurnaceBlockEntity} を継ぐので、
 * ⚠⚠ <b>何もしなければ料理人の台を鍛冶屋が奪う。</b> ⚠ 明示的に外す。
 * ⚠ 料理人の {@link CookSpeed} も<b>かまどと溶鉱炉を意図的に外して</b>あり、
 * ⚠ <b>2つの職業がちょうど噛み合う</b>（台所は料理人、炉は鍛冶屋）。
 *
 * <p>⚠ 速くする仕掛けは料理人と同じ——<b>tick をもう1回回す</b>
 * （{@code mixin/BoundTickingBlockEntityMixin}）。⚠ 新しい seam を増やさない。
 */
public final class FurnaceSpeed {

  private FurnaceSpeed() {
  }

  /** ⚠ 料理人の台と同じ 8 ブロック。⚠ 2つの能力で数字を割らない。 */
  private static final double RANGE = 8.0D;

  /**
   * その炉を速くしてよいか。
   *
   * <p>⚠ <b>近くに鍛冶屋が1人でも居れば効く</b>（本人の炉でなくてよい）。
   * ⚠ 料理人と同じ考え方で、⚠ <b>鍛冶屋が居る拠点の炉がまとめて速くなる。</b>
   */
  public static boolean shouldSpeedUp(final BlockEntity be) {

    if (!(be instanceof AbstractFurnaceBlockEntity)
        || be instanceof SmokerBlockEntity          // ⚠⚠ 燻製器は料理人のもの
        || be.getLevel() == null || be.getLevel().isClientSide) {
      return false;
    }
    if (!ShiftingOrigins.Config.BLACKSMITH_FURNACE.get()) {
      return false;
    }
    final ServerLevel level = (ServerLevel) be.getLevel();
    final double x = be.getBlockPos().getX() + 0.5D;
    final double y = be.getBlockPos().getY() + 0.5D;
    final double z = be.getBlockPos().getZ() + 0.5D;
    for (final ServerPlayer sp : level.players()) {
      if (sp.distanceToSqr(x, y, z) <= RANGE * RANGE && ClassPowers.isBlacksmith(sp)) {
        return true;
      }
    }
    return false;
  }
}
