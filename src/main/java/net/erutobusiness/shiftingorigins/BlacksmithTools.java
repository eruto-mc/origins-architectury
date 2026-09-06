package net.erutobusiness.shiftingorigins;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.AnvilRepairEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 鍛冶屋の道具と型は減らない（2026-09-06・あなたの決定）。
 *
 * <p><b>なぜこの形にしたか</b>: 鍛冶屋は ⚠ <b>4つのうち1つが目に見えず、1つは MOD に
 * 先回りされていた</b>。実測:
 *
 * <pre>
 *   名工の手     … 剣 +0.5／防具の硬さ +0.25／掘る速さ ×1.1
 *                  ⚠⚠ **道具の説明にも緑の行にも出ない**（apoli は付けた power を描かない）
 *   効率的な修理 … 素材が半分・合体修理の耐久2倍
 *                  ⚠ 金床の「安さ」は `EasyAnvils` が**全員へ配り済み**
 *                     （`too_expensive_limit = -1` ／ `prior_work_penalty = FIXED`）
 * </pre>
 *
 * <p>⚠ 残っていた代償は<b>経験値</b>だが、⚠⚠ <b>金床の経験値は「魔法を移す代金」</b>
 * （付与の数と希少度と前作業で決まる）。⚠ <b>そこは聖職者の軸</b>なので取らない。
 *
 * <p>⚠ 代わりに<b>物としての代償</b>を取り除く——⚠ <b>鍛冶型と金床は、鍛冶屋の手では減らない。</b>
 *
 * <h2>⚠ mixin を書いていない</h2>
 *
 * <p>⚠ Forge が正式な口を持っている（実物で確認）:
 *
 * <pre>
 *   AnvilMenu.onTake の 44   ForgeHooks.onAnvilRepair(...) → 壊れる確率を返す
 *   AnvilRepairEvent          getBreakChance() / setBreakChance(float)
 * </pre>
 *
 * <p>⚠⚠ <b>`EasyAnvils` が 0.05 を入れているのも同じ口</b>なので、⚠ <b>後ろに並んで 0 にする</b>。
 * ⚠ 今日3回踏んだ「別 MOD がバニラごと乗っ取っていてイベントが飛ばない」型を避けられる——
 * ⚠ <b>`ModAnvilMenu.onTake` は1行目で `super.onTake` を呼んでいる</b>ので、この口は生きている。
 *
 * <p>⚠ 鍛冶型のほうは {@code mixin/SmithingMenuMixin}（Forge に口が無いため）。
 */
public final class BlacksmithTools {

  private BlacksmithTools() {
  }

  /**
   * 金床が欠けない。
   *
   * <p>⚠ {@code LOWEST}——⚠ <b>他の MOD が確率を決め終わった後に 0 にする</b>。
   * ⚠ 早く割り込むと `EasyAnvils` の 0.05 に上書きされる。
   */
  @SubscribeEvent(priority = EventPriority.LOWEST)
  public static void onAnvilRepair(final AnvilRepairEvent event) {

    if (!ShiftingOrigins.Config.BLACKSMITH_TOOLS.get()
        || !(event.getEntity() instanceof ServerPlayer player)
        || !ClassPowers.isBlacksmith(player)) {
      return;
    }
    // ⚠ 0 なら `player.getRandom().nextFloat() < 0` が必ず偽になり、欠ける枝へ入らない。
    event.setBreakChance(0.0F);
  }
}
