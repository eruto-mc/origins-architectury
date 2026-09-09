package net.erutobusiness.shiftingorigins.mixin;

import fuzs.easymagic.world.inventory.ModEnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 予告をもう一度送らせる口。
 *
 * <p>⚠⚠ <b>これが無いと、司書の予告は「上げる前の必要レベル」で出る。</b>
 * 台は<b>必要レベルを決める → 予告を作って送る</b>まで一息で走り、
 * ⚠ {@link net.erutobusiness.shiftingorigins.LibrarianEnchanting} が必要レベルを
 * 上げるのは<b>その次のtick</b>。⚠ だから上げ終わったところで<b>もう一度送らせる</b>。
 *
 * <p>⚠ 候補は {@code this.costs} を読んで作り直されるので、
 * ⚠ <b>上げた後の一覧</b>になる（実物の逆アセンブルで確認）。
 *
 * <p>⚠ Easy Magic が無い日は当たらない——そのとき台はこの型を実装しないので、
 * 呼び出し側の {@code instanceof} が<b>静かに外れる</b>。
 */
@Mixin(value = ModEnchantmentMenu.class, remap = false)
public interface EasyMagicSendAccessor {

  @Invoker("sendEnchantingData")
  void shiftingorigins$sendEnchantingData(ItemStack stack);
}
