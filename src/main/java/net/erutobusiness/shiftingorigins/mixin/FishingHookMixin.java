package net.erutobusiness.shiftingorigins.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import net.erutobusiness.shiftingorigins.ShiftingOrigins;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 釣り人の「早合わせ」（2026-09-06）。
 *
 * <p>バニラは {@code FishingHook.catchingFish} の中で待ち時間をこう決めている
 * （実装を読んで確認）:
 *
 * <pre>
 *   timeUntilLured = Mth.nextInt(random, 100, 600);   ← 5〜30秒
 *   timeUntilLured -= lureSpeed * 20 * 5;             ← 誘いのエンチャント1段につき5秒
 * </pre>
 *
 * <p>⚠ <b>ここを縮めないと「入れ食い」が回らない。</b> 1回で2匹釣れるようになっても、
 * かかるまでの待ちが同じなら単位時間あたりの釣果はほとんど変わらない。
 * ⚠ <b>釣り人の能力2つが、同じ壁で止まっていた。</b>
 *
 * <p>⚠ <b>抽選した値そのものを縮める</b>（誘いのエンチャントのように定数を引くのではなく）。
 * 引き算だと、たまたま短い抽選が出たときに 0 以下へ張り付いて<b>待ちが消える</b>。
 * 割合なら「短いときはより短く、長いときもそれなり」で、当たり外れの形が変わらない。
 *
 * <p>⚠ <b>誘いのエンチャントとは別に掛かる</b>（バニラの引き算はそのまま残る）。
 *
 * <p>⚠ {@code @Redirect} は使わない——同じ呼び出しを他の釣りMODも横取りしうるので、
 * 重ねられる {@code @ModifyExpressionValue} にする（`LavaSwimMixin` の先例）。
 */
@Mixin(FishingHook.class)
public abstract class FishingHookMixin {

  @Shadow
  public abstract Player getPlayerOwner();

  @Unique
  private static final ResourceLocation shiftingorigins$QUICK_BITE =
      new ResourceLocation(ShiftingOrigins.MOD_ID, "quick_bite");

  // ⚠⚠ **`ordinal = 2` を外さない。** `catchingFish` の中の `Mth.nextInt` は3か所で、
  //   0 = nibble（かかってから逃げるまで。縮めると**難しくなる**）
  //   1 = timeUntilHooked（浮きが揺れる 1〜4秒）
  //   2 = timeUntilLured（⚠ **本命の 5〜30 秒**）
  //   ⚠ 指定を落とすと3つ全部に当たり、釣りが**速くなるどころか成立しなくなる**。
  @ModifyExpressionValue(
      method = "catchingFish",
      at = @At(value = "INVOKE",
          target = "Lnet/minecraft/util/Mth;nextInt(Lnet/minecraft/util/RandomSource;II)I",
          ordinal = 2))
  private int shiftingorigins$quickBite(final int rolled) {

    if (!(this.getPlayerOwner() instanceof ServerPlayer player)) {
      return rolled;
    }
    if (!IPowerContainer.get(player)
        .map(c -> c.hasPower(shiftingorigins$QUICK_BITE)).orElse(false)) {
      return rolled;
    }
    return Math.max(1, (int) (rolled * ShiftingOrigins.Config.QUICK_BITE_FACTOR.get()));
  }
}
