package net.erutobusiness.shiftingorigins;

import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * 浮遊（H で入切している間、ふわりと上がり続ける）の判定と値。
 *
 * <p>差し込む場所は {@link net.erutobusiness.shiftingorigins.mixin.HoverMixin}。
 * ここは<b>判定と値だけ</b>を持ち、いつ呼ばれるかは知らない。
 *
 * <h2>⚠⚠ なぜ状態効果をやめたか（2026-09-05）</h2>
 *
 * もとは datapack が {@code minecraft:levitation} を 10 tick ごとに 25 tick ぶん掛け直していた。
 * ⚠ 部員の報告は2つ——「<b>効果時間が短いのに掛け直しが遅くて途切れる</b>」
 * 「<b>他のバフが乗ると、そもそも掛からない</b>」。
 *
 * <p>⚠⚠⚠ 2つ目の原因は Farm &amp; Charm の<b>農夫の加護</b>だった。
 * {@code FarmersBlessingEffect} を読むと、付いた瞬間と<b>毎tick</b>の両方で
 * {@code getCategory() == MobEffectCategory.HARMFUL} の効果を {@code removeEffect} している。
 * ⚠ <b>バニラの {@code minecraft:levitation} は {@code HARMFUL}</b>（シャルカーが与える効果）なので、
 * ⚠⚠ <b>農夫の加護が付いている間、浮遊は毎tick 剥がされていた</b>。
 * ⚠ おばあちゃんの加護も同じはず。
 *
 * <p>⚠ <b>状態効果に乗せている限り、消して回る MOD に剥がされる側から抜けられない。</b>
 * だから効果をやめ、<b>power が在るあいだ直接持ち上げる</b>形にした。
 * ⚠ これで掛け直しの間隔も効果時間も要らなくなる（1つ目の報告もここで消える）。
 *
 * <h2>上がり方は変えていない</h2>
 *
 * ⚠ <b>バニラの浮遊の計算をそのまま使う</b>——{@code LivingEntity.travel} の
 * {@code d += (0.05 * (強さ + 1) - d) * 0.2} に、<b>本物の効果の代わりに作り物を渡す</b>だけ。
 * ⚠ <b>手触りを自分で発明していない。</b>強さは config（既定 0＝いままでと同じ）。
 */
public final class Hover {

  private Hover() {
  }

  /** その人がいま浮遊しているか。⚠ <b>power の条件（H の入切）まで見る。</b> */
  public static boolean isActive(LivingEntity entity) {
    if (!ShiftingOrigins.Config.HOVER_ENABLED.get() || !ShiftingOrigins.HOVER.isPresent()) {
      return false;
    }
    return IPowerContainer.hasPower(entity, ShiftingOrigins.HOVER.get());
  }

  /**
   * バニラの計算へ渡す作り物の効果。
   *
   * <p>⚠ <b>強さしか読まれない</b>（{@code travel} は {@code getAmplifier()} だけを使う）。
   * ⚠ 持ち物にも画面にも入らない——{@code travel} の中でその場限りに使われて捨てられる。
   */
  public static MobEffectInstance asLevitation() {
    return new MobEffectInstance(MobEffects.LEVITATION, 1, ShiftingOrigins.Config.HOVER_AMPLIFIER.get());
  }
}
