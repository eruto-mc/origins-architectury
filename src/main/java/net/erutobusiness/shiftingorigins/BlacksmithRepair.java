package net.erutobusiness.shiftingorigins;

import dev.limonblaze.originsclasses.common.registry.OriginsClassesPowers;
import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;

/**
 * 鍛冶屋の「効率的な修理」を、作業台でも働かせる（2026-09-05）。
 *
 * <p>⚠⚠ <b>木こりの「丸太を無駄にしない」とまったく同じ形の不具合。</b>
 * 上流の {@code RepairItemRecipeMixin} は
 *
 * <pre>
 *   if (container instanceof TransientCraftingContainer tr) { …power を掛ける… }
 *   return original;                                   ← 違えば元の値のまま
 * </pre>
 *
 * ⚠ <b>Visual Workbench</b> が作業台の器を {@code ForwardingCraftingContainer}
 * （親は {@code java.lang.Object}）へ差し替えているので、作業台では必ず外れる。
 * ⚠ 一方<b>持ち物の 2×2 では働く</b>（{@code InventoryMenu} は vanilla の器のまま）。
 * 道具2つを合わせる修理は 2×2 でもできるので、
 * ⚠ <b>「台では直りが悪く、持ち物では良い」</b>というちぐはぐな状態になっていた。
 *
 * <p><b>やり方</b>: 木こりと同じ seam（{@code CraftingMenu.slotChangedCraftingGrid} が
 * 組み上げた産物）で、<b>耐久の戻り分だけ</b>を計算し直す。
 *
 * <p>⚠ <b>バニラの式をそのまま写す</b>（{@code RepairItemRecipe.assemble} を読んで確認）:
 *
 * <pre>
 *   残り = (最大-傷1) + (最大-傷2) + 最大 * <b>5</b> / 100
 *   産物の傷 = max(最大 - 残り, 0)
 * </pre>
 *
 * ⚠ 上流の power が変えるのは<b>この 5 だけ</b>（{@code modify_combine_repair_durability}）。
 * 鍛冶屋の定義は {@code multiply_base 1.0} なので 5 → 10 になる。
 * ⚠ 数字を自分で決めない——power から採るので、datapack を変えたらこちらも追随する。
 *
 * <p>⚠ <b>二重に掛けない。</b> 器が vanilla のものなら上流が既に掛けているので、何もしない。
 */
public final class BlacksmithRepair {

  private BlacksmithRepair() {
  }

  /** バニラが使っている戻り分（百分率）。⚠ {@code RepairItemRecipe.assemble} の定数。 */
  private static final double VANILLA_BONUS = 5.0D;

  /**
   * 産物が「鍛冶屋が作業台で合わせた修理」なら、耐久の戻り分を計算し直して返す。
   */
  public static ItemStack bonus(final ItemStack result, final Player player,
      final CraftingContainer grid) {

    if (!ShiftingOrigins.Config.BLACKSMITH_REPAIR.get()
        || result.isEmpty() || !result.isDamageableItem()) {
      return result;
    }
    // ⚠ vanilla の器なら上流の RepairItemRecipeMixin が既に掛けている。触ると二重になる。
    if (grid instanceof TransientCraftingContainer || !(player instanceof ServerPlayer server)) {
      return result;
    }
    final ItemStack[] pair = twoOfAKind(grid);
    if (pair == null || pair[0].getItem() != result.getItem()) {
      return result;
    }
    final double bonus = Mth.clamp(
        Mth.floor(IPowerContainer.modify(server,
            OriginsClassesPowers.MODIFY_COMBINE_REPAIR_DURABILITY.get(), VANILLA_BONUS)),
        0, 100);
    if (bonus == VANILLA_BONUS) {
      return result;
    }
    final int max = pair[0].getMaxDamage();
    final int left = (max - pair[0].getDamageValue()) + (max - pair[1].getDamageValue())
        + (int) (max * bonus / 100.0D);
    final ItemStack out = result.copy();
    out.setDamageValue(Math.max(max - left, 0));
    return out;
  }

  /**
   * 盤面が「同じ品を1つずつ、2つだけ」ならその2つを返す。違えば null。
   *
   * <p>⚠ {@code RepairItemRecipe.assemble} と同じ条件で数える
   * （同じ品・どちらも1個・修理できる品）。⚠ ここが緩いと、
   * 修理ではない産物にまで傷を書き込むことになる。
   */
  private static ItemStack[] twoOfAKind(final CraftingContainer grid) {

    ItemStack a = ItemStack.EMPTY;
    ItemStack b = ItemStack.EMPTY;
    for (int i = 0; i < grid.getContainerSize(); i++) {
      final ItemStack slot = grid.getItem(i);
      if (slot.isEmpty()) {
        continue;
      }
      if (a.isEmpty()) {
        a = slot;
      } else if (b.isEmpty()) {
        b = slot;
      } else {
        return null;
      }
    }
    if (a.isEmpty() || b.isEmpty()
        || a.getItem() != b.getItem() || a.getCount() != 1 || b.getCount() != 1
        || !a.isRepairable()) {
      return null;
    }
    return new ItemStack[] {a, b};
  }
}
