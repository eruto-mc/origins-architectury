package net.erutobusiness.shiftingorigins.mixin;

import net.erutobusiness.shiftingorigins.LavaSwim;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 溶岩に潜っている間、<b>画面を覆う炎を描かない</b>。
 *
 * <p>⚠ 溶岩の霧を消しても（{@code client/CameraMixin}）、⚠⚠ <b>燃えている炎の絵は別に描かれる</b>。
 * ⚠ ブレイズボーンは火で傷まないが<b>燃えてはいる</b>ので、画面は炎で塞がったままになる。
 *
 * <p>⚠⚠ <b>クライアント側でだけ答えを変える。</b>
 * ⚠ サーバ側は本当のことを答えるので、<b>燃えている扱いそのものは1つも変わらない</b>
 * （ダメージ・消火・他の MOD の判定に触らない）。
 *
 * <p>⚠ <b>溶岩に浸かっている間だけ</b>に絞る。地上で燃えているときは今までどおり炎が出る。
 */
@Mixin(Entity.class)
public class FireOverlayMixin {

  @Inject(method = "isOnFire", at = @At("HEAD"), cancellable = true)
  private void shiftingorigins$hideFlamesInLava(CallbackInfoReturnable<Boolean> cir) {
    Entity self = (Entity) (Object) this;
    if (!self.level().isClientSide) {
      return;
    }
    if (LavaSwim.isActive(self) && LavaSwim.inLava(self)) {
      cir.setReturnValue(false);
    }
  }
}
