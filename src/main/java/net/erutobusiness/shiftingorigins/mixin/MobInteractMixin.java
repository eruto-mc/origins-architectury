package net.erutobusiness.shiftingorigins.mixin;

import net.erutobusiness.shiftingorigins.TamerLuck;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 調教師が右クリックしているあいだだけ、その生き物の乱数を「2回振る」ものに差し替える。
 *
 * <p>⚠ <b>当て所はバニラの {@code Mob} 1か所。</b> 手なずけを実装している MOD の
 * class を1つも名指ししないので、⚠ <b>MOD が増えても減っても壊れない</b>
 * （名指しすると、その MOD を抜いた日にビルドが落ちる。2026-09-06 に実際に落ちた）。
 *
 * <p>⚠ 窓は<b>この呼び出し1回ぶん</b>。⚠ {@code RETURN} で必ず戻す。
 *
 * <p>⚠ 馬はここを通らない（乗っているあいだに振る）ので、
 * {@link RunAroundLikeCrazyMixin} が同じ仕組みを当てる。
 */
@Mixin(Mob.class)
public abstract class MobInteractMixin {

  @Inject(method = "mobInteract", at = @At("HEAD"))
  private void shiftingorigins$tamerLuckOn(Player player, InteractionHand hand,
      CallbackInfoReturnable<InteractionResult> cir) {
    final Mob self = (Mob) (Object) this;
    if (TamerLuck.shouldHelp(self, player)) {
      TamerLuck.wrap(self);
    }
  }

  @Inject(method = "mobInteract", at = @At("RETURN"))
  private void shiftingorigins$tamerLuckOff(Player player, InteractionHand hand,
      CallbackInfoReturnable<InteractionResult> cir) {
    TamerLuck.unwrap((Mob) (Object) this);
  }
}
