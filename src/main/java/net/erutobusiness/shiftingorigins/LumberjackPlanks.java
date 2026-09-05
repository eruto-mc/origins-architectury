package net.erutobusiness.shiftingorigins;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;

/**
 * 木こりが丸太から板を作ると、枚数が増える（2026-09-05）。
 *
 * <p>⚠⚠ <b>なぜ当部で作り直したか</b>: 上流の {@code origins-classes:more_planks_from_logs}
 * （「丸太を無駄にしない」）は <b>作業台では1枚も増えていなかった</b>。
 * 部員から「動作していない」と報告が来て、実物を辿って分かった:
 *
 * <ol>
 *   <li>上流の実体は {@code apoli:modify_crafting} ＝ {@code ModifiedCraftingRecipe} という
 *       <b>動的なレシピ</b>で、盤面を見て産物を差し替える</li>
 *   <li>その {@code matches()} と {@code assemble()} は<b>最初の1行</b>が
 *       {@code inv instanceof TransientCraftingContainer} で、違えば false ／ 空を返す</li>
 *   <li>バニラの作業台は {@code CraftingMenu.craftSlots = new TransientCraftingContainer(this,3,3)}
 *       なので本来は通る</li>
 *   <li>⚠ <b>Visual Workbench</b> が {@code CraftingTableBlockMixin}（{@code required: true}・
 *       条件なし）で作業台を差し替え、{@code craftSlots} を
 *       <b>{@code ForwardingCraftingContainer}</b> に入れ替えている。
 *       ⚠ その class の親は {@code java.lang.Object} で、実装は {@code CraftingContainer} だけ
 *       ＝ <b>{@code TransientCraftingContainer} ではない</b>（バイト列で確認）</li>
 * </ol>
 *
 * <p>⚠ 配っている物すべて（jar 309本・本番ワールドの datapack 30本）で
 * {@code apoli:modify_crafting} を使う power は<b>この1件だけ</b>だった。
 * 壊れていたのがこの能力だけなのも、それで説明が付く。
 *
 * <p><b>やり方</b>: 産物を差し替えるレシピを足すのをやめ、
 * {@code CraftingMenu.slotChangedCraftingGrid} が組み上げた産物の<b>枚数だけ</b>を増やす
 * （{@code mixin/CraftingResultMixin}）。⚠ <b>レシピが1本のままなので、
 * バニラのレシピと並ばず、Polymorph の選択欄も出ない。</b>
 *
 * <p>⚠ この経路は<b>持ち物の 2×2 と作業台の両方</b>を通る——
 * {@code InventoryMenu.slotsChanged} も {@code CraftingMenu.slotChangedCraftingGrid} を呼び、
 * Visual Workbench の {@code ModCraftingMenu} も同じ静的メソッド（{@code m_150546_}）を呼ぶ
 * （どちらもバイト列で確認）。
 *
 * <p>⚠ <b>上流の power は無害な定義で上書きしてある</b>
 * （{@code data/origins-classes/powers/more_planks_from_logs.json} の
 * {@code loading_priority: 100}）。⚠ そうしないと、2×2 では上流が働いて 6 枚にした上へ
 * こちらが 2 枚足し、<b>8 枚</b>になりうる。⚠ {@code hidden} は立てていないので、
 * 能力の一覧と日本語の説明はこれまでどおり出る。
 */
public final class LumberjackPlanks {

  private LumberjackPlanks() {
  }

  /**
   * 産物が「木こりが丸太から作った板」なら、枚数を増やして返す。
   *
   * <p>⚠ 判定の順は<b>安い順</b>: 枚数の設定 → 産物のタグ → 盤面 → 職業。
   * ⚠ 職業の読み出しは capability を触るので最後に置く（クラフト画面は1操作で何度も走る）。
   *
   * <p>⚠ <b>上流は「板なら無条件に 6 枚」だった</b>が、ここは
   * <b>盤面が丸太1本だけのとき</b>に限って足す。⚠ 部員へ配っている日本語が
   * 「丸太1本から、板が2枚多く取れる」なので、<b>文のとおりの条件</b>にした
   * （上流の書き方だと、板を1枚だけ返す MOD のレシピまで 6 枚になる）。
   */
  public static ItemStack bonus(final ItemStack result, final Player player,
      final CraftingContainer grid) {

    final int extra = ShiftingOrigins.Config.BONUS_PLANKS.get();
    if (extra <= 0 || result.isEmpty() || !result.is(ItemTags.PLANKS)) {
      return result;
    }
    if (!isSingleLog(grid) || !(player instanceof ServerPlayer server)
        || !ClassPowers.isLumberjack(server)) {
      return result;
    }
    final ItemStack out = result.copy();
    out.setCount(Math.min(out.getMaxStackSize(), out.getCount() + extra));
    return out;
  }

  /**
   * 盤面に置かれている物が「丸太1本だけ」か。
   *
   * <p>⚠ 個数は見ない（バニラの板のレシピは1本ずつしか消費しない）。
   * ⚠ {@code #minecraft:logs} なので、皮を剥いだ丸太と樹皮付きの木も含み、
   * MOD が足した木も同じに扱われる。
   */
  private static boolean isSingleLog(final CraftingContainer grid) {

    ItemStack only = ItemStack.EMPTY;
    for (int i = 0; i < grid.getContainerSize(); i++) {
      final ItemStack slot = grid.getItem(i);
      if (slot.isEmpty()) {
        continue;
      }
      if (!only.isEmpty()) {
        return false;
      }
      only = slot;
    }
    return !only.isEmpty() && only.is(ItemTags.LOGS);
  }
}
