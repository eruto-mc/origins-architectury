package net.erutobusiness.shiftingorigins.mixin;

import net.erutobusiness.shiftingorigins.LavaSwim;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.fluids.FluidType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 溶岩を「泳げる液体」と答えさせる（その power を持っている人にだけ）。
 *
 * <p>⚠ Forge は液体の型ごとに「泳げるか」を持っており、⚠ <b>溶岩は既定で偽</b>。
 * ⚠ ここが偽のままだと、泳ぐ姿勢にも入らないし、泳ぎの判定も一切通らない。
 *
 * <p>⚠ <b>相手ごとに答えを変えられる</b>のがこのメソッドの良い所——
 * ⚠⚠ <b>全員が溶岩を泳げるようにはしない。</b>
 */
@Mixin(FluidType.class)
public class FluidTypeMixin {

  // ⚠ `FluidType` は Forge のクラスで難読化されないので、⚠ **対応表を引かせない**
  //    （`remap = true` のままだと「canSwim の対応が無い」でビルドが落ちる）。
  @Inject(method = "canSwim", at = @At("HEAD"), cancellable = true, remap = false)
  private void shiftingorigins$lavaIsSwimmable(Entity entity, CallbackInfoReturnable<Boolean> cir) {
    if (entity == null || (Object) this != ForgeMod.LAVA_TYPE.get()) {
      return;
    }
    if (LavaSwim.isActive(entity)) {
      cir.setReturnValue(true);
    }
  }
}
