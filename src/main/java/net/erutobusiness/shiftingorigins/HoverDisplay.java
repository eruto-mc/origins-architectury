package net.erutobusiness.shiftingorigins;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;

/**
 * 浮遊が入っている間、バフ欄へアイコンを出しておく。
 *
 * <p>⚠ <b>動きとは無関係</b>。ここが止まっても浮遊は効く（{@link HoverEffect} の説明）。
 *
 * <h2>⚠ なぜ掛け直す形にしたか</h2>
 *
 * ⚠ 長い時間で1回だけ掛けると、⚠⚠ <b>浮遊を切らずに落ちた（サーバが落ちた・強制終了した）ときに
 * アイコンが残り続ける</b>。掛け直す形なら {@code REFRESH_BELOW} tick で勝手に消える。
 * ⚠ バニラのビーコンも同じ形（短い効果を掛け直す）なので、見え方も見慣れたものになる。
 */
public final class HoverDisplay {

  /** 掛け直すときの長さ（tick）。 */
  private static final int DURATION = 100;

  /** 残りがこれを下回ったら掛け直す。⚠ 0 まで落ちる前に足すので、途切れて見えない。 */
  private static final int REFRESH_BELOW = 60;

  private HoverDisplay() {
  }

  @SubscribeEvent
  public static void onPlayerTick(final TickEvent.PlayerTickEvent event) {
    if (event.phase != TickEvent.Phase.END || event.side != LogicalSide.SERVER) {
      return;
    }
    if (!(event.player instanceof ServerPlayer player) || !ShiftingOrigins.HOVER_EFFECT.isPresent()) {
      return;
    }
    MobEffectInstance current = player.getEffect(ShiftingOrigins.HOVER_EFFECT.get());
    boolean wanted = ShiftingOrigins.Config.HOVER_SHOW_ICON.get() && Hover.isActive(player);

    if (!wanted) {
      // ⚠ **自分が出した分だけ外す**（他から同じ効果が来ることは無いが、形を揃えておく）。
      if (current != null) {
        player.removeEffect(ShiftingOrigins.HOVER_EFFECT.get());
      }
      return;
    }
    if (current == null || current.getDuration() < REFRESH_BELOW) {
      // ⚠ ambient にすると画面の縁の粒が出ない。⚠ 粒も音も要らないので切る。
      player.addEffect(new MobEffectInstance(
          ShiftingOrigins.HOVER_EFFECT.get(), DURATION, 0, true, false, true));
    }
  }
}
