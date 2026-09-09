package net.erutobusiness.shiftingorigins.mixin;

import net.erutobusiness.shiftingorigins.MerchantPrice;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 商人が取引を開いたとき、需要で上がったぶんを打ち消す（2026-09-09）。
 *
 * <p>⚠ <b>ここしかない</b>——値段のうちプレイヤーごとに動くのは
 * {@code specialPriceDiff} だけで、それを決めるのがこのメソッド。
 * ⚠ Forge のイベントは無い（{@code Villager.java.patch} を全部読んで確かめた）。
 *
 * <p>⚠ <b>末尾に入る。</b> 上流が評判と村の英雄のぶんを入れ終えた後に足すので、
 * ⚠ <b>あちらの値引きを消さない</b>。
 *
 * <p>⚠ 触るのは<b>バニラの型だけ</b>なので、取引まわりの MOD が変わっても壊れない。
 */
@Mixin(Villager.class)
public abstract class VillagerPriceMixin {

  @Inject(method = "updateSpecialPrices", at = @At("TAIL"))
  private void shiftingorigins$steadyPrice(Player player, CallbackInfo ci) {
    MerchantPrice.cancelDemand((Villager) (Object) this, player);
  }
}
