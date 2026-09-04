package net.erutobusiness.shiftingorigins;

import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.ForgeMod;

/**
 * 溶岩を「水と同じように泳げる」ようにする判定。
 *
 * <p>差し込む場所は {@code mixin/FluidTypeMixin}・{@code mixin/LavaSwimMixin}・
 * {@code mixin/FireOverlayMixin}。ここは<b>判定だけ</b>を持つ。
 *
 * <h2>⚠⚠ 前の作りが「ガクガク」した理由（2026-09-05 に読んで確定）</h2>
 *
 * datapack は2つを組み合わせていた。⚠ <b>どちらも泳ぎではなかった</b>:
 *
 * <ul>
 *   <li>{@code apoli:modify_lava_speed} … AEA の {@code LAVA_SPEED} 属性を動かすが、
 *       AEA がそれを使うのは {@code travel} の中の <b>{@code 0.5} という定数</b>——
 *       ⚠ <b>速さではなく「毎tick 速度を半分にする減衰」</b>。加速はバニラの {@code 0.02} のまま</li>
 *   <li>{@code apoli:swimming} … 本体は何もしない印で、実体は Apoli の
 *       {@code PlayerEntityMixin}。⚠ <b>走っている間だけ</b>、毎tick
 *       {@code move(視線 / 4)} で<b>位置を直接ずらす</b>。
 *       ⚠⚠ 速度ではないので<b>階段状に進む</b>のがガクつきの正体</li>
 * </ul>
 *
 * <p>⚠ さらに表面では、バニラが浅い側と深い側で<b>別の分岐</b>を通る
 * （{@code getFluidHeight(LAVA) <= fluidJumpThreshold}）ので、
 * 上下すると毎tick 物理が切り替わっていた。
 *
 * <h2>やり方（既製品から借りた当て所）</h2>
 *
 * <p>{@code Swim In Lava}（CurseForge・1.20.1 Forge）の jar を読んで、
 * <b>3つの当て所</b>を採った。⚠ <b>コードは写していない</b>——判定が違うため
 * （あちらは {@code fireImmune()} か火炎耐性の効果で見る。
 * ⚠⚠ <b>ブレイズボーンの {@code origins:fire_immunity} は {@code apoli:invulnerability} なので
 * どちらにも当たらず、あの MOD をそのまま入れても効かない</b>）。
 *
 * <p>⚠⚠ 肝は {@code travel} の {@code isInWater()} を真にすること——
 * <b>水の分岐を通す</b>ので、加速・抵抗・浮力・上がり下がりが丸ごと水と同じになる。
 * ⚠ <b>泳ぎの手触りを自分で発明していない。</b>
 */
public final class LavaSwim {

  private LavaSwim() {
  }

  /** その人がいま溶岩を泳げるか。⚠ <b>power の条件まで見る。</b> */
  public static boolean isActive(Entity entity) {
    if (!ShiftingOrigins.Config.LAVA_SWIM_ENABLED.get() || !ShiftingOrigins.LAVA_SWIM.isPresent()) {
      return false;
    }
    return IPowerContainer.hasPower(entity, ShiftingOrigins.LAVA_SWIM.get());
  }

  /** いま溶岩に浸かっているか。⚠ 流体の型で見る（ブロックの種類で見ない）。 */
  public static boolean inLava(Entity entity) {
    return entity.isInFluidType(ForgeMod.LAVA_TYPE.get());
  }
}
