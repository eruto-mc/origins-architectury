package net.erutobusiness.shiftingorigins.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.erutobusiness.shiftingorigins.LumberjackPlanks;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 産物の枠に置かれる直前の品を、当部の都合で差し替える口（2026-09-05）。
 *
 * <p>いまの用途は木こりの板だけ（{@link LumberjackPlanks} が中身を決める）。
 *
 * <p>⚠ <b>ここ1か所で 2×2 も作業台も覆える。</b>
 * {@code InventoryMenu.slotsChanged} も {@code CraftingMenu.slotsChanged} も、
 * Visual Workbench の {@code ModCraftingMenu} も、
 * 同じ静的メソッド {@code CraftingMenu.slotChangedCraftingGrid}（SRG {@code m_150546_}）を呼ぶ
 * （3つとも class のバイト列で確認）。
 *
 * <p>⚠⚠ <b>{@code assemble} の引数は {@code CraftingContainer} ではなく
 * {@code Container}</b>。{@code CraftingRecipe} は {@code assemble} を宣言し直しておらず、
 * {@code Recipe<C>} の消去後の形で呼ばれるため。⚠ 定数プールを読んで確かめた——
 * <b>書き間違えると起動時に「could not find any targets」で落ちる。</b>
 *
 * <p>⚠⚠ <b>{@code @Redirect} を使わない。</b> 1命令に1つしか取れないので、
 * 同じ呼び出しを別の MOD も横取りしていると {@code (0/1) succeeded} で落ちる
 * （2026-09-05 に {@code LavaSwimMixin} で実際に落ちた。build.gradle の注記）。
 * MixinExtras の {@code @ModifyExpressionValue} は重ねられる。
 *
 * <p>⚠ 差し替えるのは {@code assemble} の<b>戻り値</b>なので、
 * 産物の枠・{@code setRemoteSlot}・クライアントへ送る packet の3つが<b>同じ品</b>になる。
 * ⚠ 途中の局所変数を書き換える形にすると、この3つがずれる。
 */
@Mixin(CraftingMenu.class)
public abstract class CraftingResultMixin {

  @ModifyExpressionValue(
      method = "slotChangedCraftingGrid",
      at = @At(value = "INVOKE",
          target = "Lnet/minecraft/world/item/crafting/CraftingRecipe;"
              + "assemble(Lnet/minecraft/world/Container;Lnet/minecraft/core/RegistryAccess;)"
              + "Lnet/minecraft/world/item/ItemStack;"))
  private static ItemStack shiftingorigins$morePlanks(final ItemStack result,
      final AbstractContainerMenu menu, final Level level, final Player player,
      final CraftingContainer grid, final ResultContainer out) {

    return LumberjackPlanks.bonus(result, player, grid);
  }
}
