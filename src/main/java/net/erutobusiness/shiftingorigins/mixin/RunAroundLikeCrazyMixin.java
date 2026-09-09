package net.erutobusiness.shiftingorigins.mixin;

import net.erutobusiness.shiftingorigins.TamerLuck;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.RunAroundLikeCrazyGoal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 馬・ロバ・ラバの手なずけにも「2回振る」を届かせる（2026-09-09）。
 *
 * <h2>⚠⚠ 馬は右クリックで手なずけない</h2>
 *
 * <p>抽選は {@code RunAroundLikeCrazyGoal.tick} に在る（1.20.1 の本体を逆アセンブルして確かめた）:
 *
 * <pre>
 *   if (!horse.isTamed() &amp;&amp; horse.getRandom().nextInt(adjustedTickDelay(50)) == 0) {
 *     ...
 *     if (maxTemper &gt; 0 &amp;&amp; horse.getRandom().nextInt(maxTemper) &lt; temper) {
 *       horse.tameWithName(player);   // ← ここ
 *     }
 *     horse.modifyTemper(5);
 *     horse.ejectPassengers();
 *     horse.makeMad();
 *   }
 * </pre>
 *
 * <p>⚠ <b>振っているのは馬自身の乱数</b>なので、{@link TamerLuck} がそのまま届く。
 * ⚠ <b>乗っている人が調教師のときだけ</b>掛ける。
 *
 * <p>⚠ 暴れる判定（{@code nextInt(50) == 0}）も同じ窓に入るので、
 * ⚠ <b>決着が早くなる</b>。⚠ 「懐きやすい」と同じ向きなので害は無い。
 *
 * <p>⚠ この目標はバニラの型なので、⚠ <b>馬まわりの MOD に依存しない</b>。
 */
@Mixin(RunAroundLikeCrazyGoal.class)
public abstract class RunAroundLikeCrazyMixin {

  @Shadow
  @Final
  private AbstractHorse horse;

  @Inject(method = "tick", at = @At("HEAD"))
  private void shiftingorigins$tamerLuckOn(CallbackInfo ci) {
    final Player rider = shiftingorigins$rider();
    if (rider != null && TamerLuck.shouldHelp(this.horse, rider)) {
      TamerLuck.wrap(this.horse);
    }
  }

  @Inject(method = "tick", at = @At("RETURN"))
  private void shiftingorigins$tamerLuckOff(CallbackInfo ci) {
    TamerLuck.unwrap(this.horse);
  }

  /** 乗っている人。⚠ バニラの本体と同じ採り方（先頭の乗客が人かどうか）。 */
  private Player shiftingorigins$rider() {
    if (this.horse.getPassengers().isEmpty()) {
      return null;
    }
    final Entity first = this.horse.getPassengers().get(0);
    return first instanceof Player player ? player : null;
  }
}
