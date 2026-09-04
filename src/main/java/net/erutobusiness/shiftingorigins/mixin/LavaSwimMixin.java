package net.erutobusiness.shiftingorigins.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.erutobusiness.shiftingorigins.LavaSwim;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * ⚠⚠ <b>ここが泳ぎの本体。</b>{@code travel} の中で「水に入っているか」を真にして、
 * <b>水の分岐を通させる</b>。
 *
 * <p>⚠ 水の分岐には、加速・抵抗・浮力・上下の動きが全部そろっている。
 * ⚠ <b>手触りを自分で書かない</b>ので、水中と同じ操作感になる。
 *
 * <p>⚠ 溶岩の分岐（毎tick 速度を半分にする重い減衰と、浅い／深いで切り替わる浮力）は
 * <b>通らなくなる</b>。⚠⚠ <b>前の「表面でガクガクする」の原因はそこ</b>だった。
 *
 * <p>⚠ <b>{@code travel} の中だけ</b>を書き換える。落下ダメージや消火の判定など、
 * ⚠ <b>他の場所の「水に入っているか」は本当のことを答えたまま</b>にする
 * （溶岩で溺れない・水扱いで火が消える、のような副作用を出さないため）。
 */
@Mixin(LivingEntity.class)
public abstract class LavaSwimMixin {

  // ⚠⚠ **`@Redirect` は使えない**（2026-09-05 に実機で落ちた）。
  //    ⚠ `travel` の `isInWater()` は**他の MOD も横取りしている**（当部は ParCool ほか
  //      movement 系を多数入れている）。⚠ `@Redirect` は1命令につき1つしか取れないので、
  //      ⚠⚠ **後から来たほうが `(0/1) succeeded` で落ち、サーバが起動しない。**
  //    ⚠ MixinExtras の `@WrapOperation` は**重ねられる**（Apoli も同じ形を使っている）。
  @WrapOperation(method = "travel", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/world/entity/LivingEntity;isInWater()Z"))
  private boolean shiftingorigins$lavaCountsAsWater(LivingEntity self, Operation<Boolean> original) {
    if (original.call(self)) {
      return true;
    }
    return LavaSwim.isActive(self) && LavaSwim.inLava(self);
  }
}
