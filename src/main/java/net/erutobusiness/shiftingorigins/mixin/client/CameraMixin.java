package net.erutobusiness.shiftingorigins.mixin.client;

import io.github.edwinmindcraft.apoli.common.power.ModifyCameraSubmersionTypePower;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * {@code apoli:modify_camera_submersion} を <b>カメラの元の判定そのもの</b>へ届かせる。
 *
 * <p><b>なぜ要るか</b>: Apoli はこの power を3か所でしか読んでいない——
 * {@code FogRenderer.setupColor} と {@code setupFog} の<b>局所変数</b>、
 * それと {@code GameRenderer.getFov} の<b>その1か所の呼び出し</b>。
 * ⚠ <b>{@code Camera.getFluidInCamera()} の戻り値そのものは変えていない。</b>
 *
 * <p>⚠⚠ Iris / Oculus は {@code isEyeInWater} をこのメソッドから組み立てるので、
 * ⚠ <b>シェーダーを入れると、シェーダー側が自前の溶岩の霧を上から描いて全部打ち消す</b>。
 * 当部は Bliss を常用しているため、Apoli の3か所だけでは足りない。
 *
 * <p><b>やり方</b>: 元の判定に power を当てて、溶岩を「何にも浸かっていない」へ替える。
 * ⚠ これで<b>バニラの霧とシェーダーの両方が同時に</b>直る（Apoli の3か所は、
 * 同じ答えを2度当てるだけになるので害は無い）。
 *
 * <p>⚠ <b>power を新しく作っていない。</b>効かせる相手は datapack の
 * {@code world3:lava_sight}（{@code apoli:modify_camera_submersion}）で、
 * ⚠ <b>誰に効くかは今までどおり種族の定義が決める</b>。
 *
 * <p>⚠ <b>広く効く</b>点に注意——このメソッドは霧だけでなく、視野・音のこもり方など
 * 「目が液体の中に在るか」を見る処理すべての入口。⚠ <b>溶岩に潜っている間の見え方が
 * 丸ごと「外に居る」扱いになる</b>のは意図した動きだが、副作用が出たらここを疑う。
 *
 * <p>出どころ: Sakura Lava Vision（Fabric・MC 26.2）の作者が
 * 「FogData を広げるだけの MOD はシェーダーに上書きされる」「Iris はこのメソッドから
 * {@code isEyeInWater} を作る」と明記している。⚠ <b>当部が使えるのは仕組みだけ</b>で、
 * あちらの実装は Fabric 用。
 */
@Mixin(Camera.class)
@OnlyIn(Dist.CLIENT)
public abstract class CameraMixin {

  @Shadow
  public abstract Entity getEntity();

  @Inject(method = "getFluidInCamera", at = @At("RETURN"), cancellable = true)
  private void shiftingorigins$applyCameraSubmersionPower(CallbackInfoReturnable<FogType> cir) {
    Entity entity = this.getEntity();
    if (entity == null) {
      return;
    }
    ModifyCameraSubmersionTypePower.tryReplace(entity, cir.getReturnValue())
        .ifPresent(cir::setReturnValue);
  }
}
