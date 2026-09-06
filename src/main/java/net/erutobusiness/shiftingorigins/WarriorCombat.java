package net.erutobusiness.shiftingorigins;

import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 戦士の「受け流し」と「背水」（2026-09-06）。
 *
 * <p>⚠ <b>なぜ Java か</b>: apoli に登録されている entity condition を全部並べたが、
 * <b>体力を見る条件が1つも無い</b>（`health` も `relative_health` も無い。在るのは
 * `attribute`＝属性の値で、いまの体力ではない）。⚠ 「盾で受けた直後」を表す状態も無い。
 * ⚠ どちらも data 側では書けないので、⚠ <b>印だけの power ＋ Java</b> という
 * 当部の型（浮遊・溶岩泳ぎと同じ）で作る。
 *
 * <p><b>受け流し</b>: 盾で受けた瞬間から一定時間、次の1撃だけ与ダメージが増える。
 * ⚠ <b>1撃で消える</b>（増えたまま殴り続けられない）。
 *
 * <p><b>背水</b>: 体力の割合で2段。⚠ <b>最大体力で割る</b>ので、
 * 体力の少ない種族（猫・ゴブリンなど）でも同じ条件になる。
 *
 * <p>⚠ 掛け算の順は「背水 → 受け流し」。⚠ どちらも倍率なので、
 * 半分を切って受け流した1撃は両方が乗る（戦士を強くする側に倒すという判断）。
 */
public final class WarriorCombat {

  private WarriorCombat() {
  }

  private static final ResourceLocation RIPOSTE =
      new ResourceLocation(ShiftingOrigins.MOD_ID, "riposte");
  private static final ResourceLocation LAST_STAND =
      new ResourceLocation(ShiftingOrigins.MOD_ID, "last_stand");

  /**
   * 受け流しが構えている人と、その期限（世界の時刻）。
   *
   * <p>⚠ <b>持ち越さない</b>——退出時に消す（{@link #onLogout}）。
   * ⚠ 保存もしない。盾で受けてから数秒の話なので、再入したら消えていて構わない。
   */
  private static final Map<UUID, Long> RIPOSTE_UNTIL = new HashMap<>();

  /** 盾で受けたら、受け流しを構える。 */
  @SubscribeEvent
  public static void onShieldBlock(final ShieldBlockEvent event) {

    if (!(event.getEntity() instanceof ServerPlayer player)
        || !IPowerContainer.get(player).map(c -> c.hasPower(RIPOSTE)).orElse(false)) {
      return;
    }
    RIPOSTE_UNTIL.put(player.getUUID(),
        player.level().getGameTime() + ShiftingOrigins.Config.RIPOSTE_TICKS.get());
  }

  /** 与えるダメージを、背水と受け流しで増やす。 */
  @SubscribeEvent
  public static void onHurt(final LivingHurtEvent event) {

    if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
      return;
    }
    float amount = event.getAmount();
    amount *= lastStand(player);
    amount *= riposte(player);
    if (amount != event.getAmount()) {
      event.setAmount(amount);
    }
  }

  /** 退出したら構えを捨てる（別の人が同じ枠を拾わないように）。 */
  @SubscribeEvent
  public static void onLogout(final PlayerEvent.PlayerLoggedOutEvent event) {
    RIPOSTE_UNTIL.remove(event.getEntity().getUUID());
  }

  /**
   * 背水の倍率。⚠ 割合で見るので、最大体力が違う種族でも同じ条件になる。
   */
  private static float lastStand(final ServerPlayer player) {

    if (!IPowerContainer.get(player).map(c -> c.hasPower(LAST_STAND)).orElse(false)) {
      return 1.0F;
    }
    final float max = player.getMaxHealth();
    if (max <= 0.0F) {
      return 1.0F;
    }
    final float ratio = player.getHealth() / max;
    if (ratio <= 0.25F) {
      return 1.0F + ShiftingOrigins.Config.LAST_STAND_QUARTER.get().floatValue();
    }
    if (ratio <= 0.5F) {
      return 1.0F + ShiftingOrigins.Config.LAST_STAND_HALF.get().floatValue();
    }
    return 1.0F;
  }

  /**
   * 受け流しの倍率。⚠ <b>使ったら消す</b>ので、増えるのは1撃だけ。
   */
  private static float riposte(final ServerPlayer player) {

    final Long until = RIPOSTE_UNTIL.get(player.getUUID());
    if (until == null) {
      return 1.0F;
    }
    // ⚠ 期限切れも「消す」——次に殴るまで残しておくと、いつの受けか分からなくなる
    RIPOSTE_UNTIL.remove(player.getUUID());
    if (player.level().getGameTime() > until) {
      return 1.0F;
    }
    return 1.0F + ShiftingOrigins.Config.RIPOSTE_BONUS.get().floatValue();
  }
}
