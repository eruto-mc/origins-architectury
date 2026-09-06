package net.erutobusiness.shiftingorigins;

import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import io.github.edwinmindcraft.apoli.api.component.IPowerDataCache;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredDamageCondition;
import io.github.edwinmindcraft.apoli.common.registry.ApoliPowers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.event.entity.living.LivingUseTotemEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 種族の蘇りを、不死のトーテムより先に働かせる（2026-09-06・あなたの指示）。
 *
 * <p><b>直す前は逆だった。</b> バニラの並びを実物のバイトコードで読んだ結果
 * （`forge-1.20.1-47.4.0_mapped_official` の {@code LivingEntity}）:
 *
 * <pre>
 *   hurt の 682   checkTotemDeathProtection(DamageSource)Z   ← トーテムの判定
 *   hurt の 685   ifne -&gt; 736                                 ← 守られたら死の処理を全部飛ばす
 *   hurt の 720   die(DamageSource)V                          ← トーテムが偽のときだけ来る
 *   die  の   2   ForgeHooks.onLivingDeath(...)               ← 当部の蘇りはここに乗っている
 * </pre>
 *
 * <p>⚠ つまり<b>トーテムを持っているあいだ、種族の蘇りは呼ばれもしない</b>。
 * トーテムは1回で消えるが、こちらは腐肉と骨を溜め直せば何度でも使える。
 * ⚠ <b>先に消えるべきなのは、溜め直せるほうだった。</b>
 *
 * <p><b>やり方</b>: ⚠ <b>mixin を書かない。</b> Forge が
 * {@code checkTotemDeathProtection} の中（682 の内側・63 の位置）で
 * {@code ForgeHooks.onLivingUseTotem} を呼んでいて、⚠ <b>そこで打ち切ると
 * {@code stack.shrink(1)}（78）を飛ばして手のループへ戻る</b>ので、
 * <b>トーテムは減らず、判定は偽になり、そのまま死の処理＝当部の蘇りへ進む</b>。
 * ⚠ 既製品も探した（Totem of Anything ／ Void Totem ／ More Totems Of Undying ／
 * Apoli 系の追加 power 型）が、<b>優先順位を入れ替えるものは1つも無かった</b>。
 *
 * <p>⚠⚠ <b>ここを間違えると、トーテムを消した上に死ぬ。</b> だから判定は
 * {@code PreventDeathPower.tryPreventDeath} と<b>同じ2つ</b>を、同じ順で見る——
 * ①{@code IPowerDataCache} が在ること（無いと上流の handler は何もしない）
 * ②{@code prevent_death} のうち条件を満たすものが1つ以上在ること。
 * ⚠ <b>片方でも違えたら、打ち切らない。</b>
 *
 * <p>⚠ 当部のパックで {@code prevent_death} を使う power は
 * <b>{@code world3:undying}（アンデッドの「まだ還らない」）の1つだけ</b>（実測）。
 * ⚠ それでも power を名指ししない——名指しすると、次に蘇りを足した日に黙って外れる。
 */
public final class UndyingBeforeTotem {

  private UndyingBeforeTotem() {
  }

  /**
   * トーテムが使われる直前。
   *
   * <p>⚠ {@code HIGH} にしてある。⚠ <b>他の MOD がトーテムの扱いを変えているとき、
   * そちらの判断より先に打ち切ると読みが変わる</b>ので、{@code HIGHEST} は使わない。
   */
  @SubscribeEvent(priority = EventPriority.HIGH)
  public static void onUseTotem(final LivingUseTotemEvent event) {

    if (!ShiftingOrigins.Config.UNDYING_BEFORE_TOTEM.get()
        || !(event.getEntity() instanceof ServerPlayer player)
        || !willRevive(player, event.getSource())) {
      return;
    }
    // ⚠ 打ち切る＝このトーテムを使わない。⚠ **減らない**（shrink を飛ばすため）。
    event.setCanceled(true);
  }

  /**
   * このあと {@code prevent_death} が確実に働くか。
   *
   * <p>⚠⚠ <b>上流と同じ判定でなければならない。</b> 参照元は
   * {@code ApoliPowerEventHandler.preventLivingDeath} と
   * {@code PreventDeathPower.tryPreventDeath}。
   */
  private static boolean willRevive(final ServerPlayer player, final DamageSource source) {
    return IPowerDataCache.get(player)
        .map(IPowerDataCache::getDamage)
        .map(amount -> IPowerContainer
            .getPowers(player, ApoliPowers.PREVENT_DEATH.get()).stream()
            .anyMatch(power -> ConfiguredDamageCondition.check(
                power.value().getConfiguration().condition(), source, amount)))
        .orElse(false);
  }
}
