package net.erutobusiness.shiftingorigins.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.erutobusiness.shiftingorigins.ClassPowers;
import net.erutobusiness.shiftingorigins.ShiftingOrigins;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 鍛冶屋は鍛冶型を消費しない（2026-09-06・あなたの案）。
 *
 * <p>⚠ 鍛冶型（ネザライトの型・装飾の型）は<b>使い切りの一点物</b>で、
 * ⚠ **鍛冶屋以外には意味の無い有利**になる。⚠ 「鍛冶屋の型は減らない」という1行で言える。
 *
 * <h2>⚠ ここは空いていた</h2>
 *
 * <p>⚠ バニラの並び（実物のバイトコード）:
 *
 * <pre>
 *   SmithingMenu.onTake の 27   shrinkStackInSlot(0)   ← 鍛冶型
 *                        32   shrinkStackInSlot(1)   ← 装備
 *                        37   shrinkStackInSlot(2)   ← 素材
 * </pre>
 *
 * <p>⚠⚠ <b>`onTake` を触っている MOD は0本</b>（citadel と polymorph は `createResult` と
 * `getRecipesFor`、letsdo-furniture は `isValidBlock` だけ）。
 * ⚠ 金床が5本に取り合われているのと正反対で、⚠ <b>ここなら黙って死なない。</b>
 *
 * <p>⚠ <b>`@Redirect` は使わない</b>——移動や描画のように他の MOD も触る所で
 * 1命令を独占すると、⚠ <b>後から入った側が起動時に落ちる</b>（2026-09-05 に実機で落とした）。
 * ⚠ 重ねられる {@code @WrapOperation} を使う。
 *
 * <p>⚠ <b>1つ目（`ordinal = 0`）だけ包む。</b> 装備と素材は今までどおり減る。
 */
@Mixin(SmithingMenu.class)
public abstract class SmithingMenuMixin {

  @WrapOperation(
      method = "onTake",
      at = @At(value = "INVOKE",
          target = "Lnet/minecraft/world/inventory/SmithingMenu;shrinkStackInSlot(I)V",
          ordinal = 0))
  private void shiftingorigins$keepTemplate(final SmithingMenu menu, final int slot,
      final Operation<Void> original, final Player player, final ItemStack result) {

    if (ShiftingOrigins.Config.BLACKSMITH_TOOLS.get()
        && player instanceof ServerPlayer sp && ClassPowers.isBlacksmith(sp)) {
      return;                     // ⚠ 鍛冶型を減らさない（装備と素材はこの後で減る）
    }
    original.call(menu, slot);
  }
}
