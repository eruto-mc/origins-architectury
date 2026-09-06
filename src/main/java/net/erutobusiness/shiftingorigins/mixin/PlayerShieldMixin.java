package net.erutobusiness.shiftingorigins.mixin;

import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import net.erutobusiness.shiftingorigins.ShiftingOrigins;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 戦士の「盾を割られない」（2026-09-06）。
 *
 * <p>バニラは {@code Player.disableShield(boolean)} の中で
 *
 * <pre>
 *   f = 0.25 + 効率のエンチャント * 0.05
 *   斧で殴られたなら f += 0.75     ← ⚠ ほぼ確実に割れる
 *   乱数 &lt; f なら 盾を100tick 使えなくし、構えを解く
 * </pre>
 *
 * <p>⚠ <b>斧を持った相手には、ほぼ毎回盾を割られる</b>（0.25＋0.75＝1.0 を超える）。
 * この世界は略奪者や斧持ちが多く、盾で戦う組み立てがそこで崩れる。
 *
 * <p>⚠ 割る側ではなく<b>割られる側</b>で止める。{@code disableShield} は
 * 受けた本人に対して呼ばれるので、ここで能力を見て打ち切ればよい。
 *
 * <p>⚠ <b>盾の耐久は今までどおり減る。</b> 止めているのは「弾かれて構えが解ける」だけ。
 */
@Mixin(Player.class)
public abstract class PlayerShieldMixin {

  @Unique
  private static final ResourceLocation shiftingorigins$SHIELD_MASTER =
      new ResourceLocation(ShiftingOrigins.MOD_ID, "shield_master");

  @Inject(method = "disableShield", at = @At("HEAD"), cancellable = true)
  private void shiftingorigins$keepShieldUp(final boolean becauseOfAxe, final CallbackInfo ci) {

    if (!ShiftingOrigins.Config.SHIELD_MASTER.get()
        || !((Object) this instanceof ServerPlayer player)) {
      return;
    }
    if (IPowerContainer.get(player)
        .map(c -> c.hasPower(shiftingorigins$SHIELD_MASTER)).orElse(false)) {
      ci.cancel();
    }
  }
}
