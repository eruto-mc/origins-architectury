package net.erutobusiness.shiftingorigins.mixin;

import java.util.List;
import net.erutobusiness.shiftingorigins.LibrarianEnchanting;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 司書が本を焼くとき、抽選から1つ捨てない（2026-09-09・あなたの決定）。
 *
 * <h2>⚠⚠ バニラは本のときだけ1つ捨てている</h2>
 *
 * <p>{@code EnchantmentMenu.getEnchantmentList} の中身（1.20.1 の本体を逆アセンブルして確認）:
 *
 * <pre>
 *   this.random.setSeed(this.enchantmentSeed.get() + slot);
 *   List&lt;EnchantmentInstance&gt; list = EnchantmentHelper.selectEnchantment(this.random, stack, cost, false);
 *   if (stack.is(Items.BOOK) &amp;&amp; list.size() &gt; 1) {
 *     list.remove(this.random.nextInt(list.size()));   // ← ここ
 *   }
 *   return list;
 * </pre>
 *
 * <p>⚠ 本の付与は<b>あとで他の道具へ移せる</b>ので、バニラは本の取り分を1つ減らしている。
 * ⚠ 司書はそれを減らさない。
 *
 * <h2>⚠ 乱数の並びは変えない</h2>
 *
 * <p>{@code random.nextInt(list.size())} は<b>引数</b>なので、{@code @Redirect} で
 * {@code remove} を飛ばしても<b>乱数は必ず消費される</b>。
 * ⚠⚠ <b>これが要る</b>——消費しないと、この後に続く抽選がずれて
 * <b>予告と結果が食い違う</b>（当部は一度そのずれを踏んでいる）。
 *
 * <h2>⚠ Easy Magic も同じ道を通る</h2>
 *
 * <p>Easy Magic の {@code ModEnchantmentMenu} は自前の {@code slotsChanged} を持つが、
 * ⚠ <b>予告を作る {@code createEnchantmentInstance} は
 * {@code EnchantmentMenuAccessor.callGetEnchantmentList} でここを呼び</b>、
 * ⚠ <b>{@code clickMenuButton} は {@code super} を呼ぶ</b>（どちらも実物のバイト列で確認）。
 * ⇒ <b>予告も結果もここを通る。</b>
 */
@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuBookMixin {

  @Redirect(method = "getEnchantmentList",
      at = @At(value = "INVOKE", target = "Ljava/util/List;remove(I)Ljava/lang/Object;"))
  private Object shiftingorigins$keepBookEnchantment(List<EnchantmentInstance> list, int index) {
    if (LibrarianEnchanting.keepsBookEnchantment((EnchantmentMenu) (Object) this)) {
      return null;
    }
    return list.remove(index);
  }
}
