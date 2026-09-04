package net.erutobusiness.shiftingorigins;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * 浮遊が入っていることを<b>画面に出すためだけ</b>の状態効果。
 *
 * <p>⚠⚠ <b>体は1ミリも動かさない</b>——持ち上げるのは
 * {@link net.erutobusiness.shiftingorigins.mixin.HoverMixin} のほう。
 * こちらは<b>バフ欄にアイコンを出す役</b>だけを持つ。
 *
 * <h2>⚠ なぜ分類が BENEFICIAL なのか</h2>
 *
 * ⚠ もとは {@code minecraft:levitation}（分類は <b>HARMFUL</b>）を掛けていて、
 * ⚠⚠ <b>農夫の加護が HARMFUL の効果を毎tick 消す</b>ので剥がされていた（{@link Hover} の説明）。
 * ⚠ <b>同じ轍を踏まないために BENEFICIAL にする。</b>
 * ⚠ ただし「有益な効果を消して回る MOD」が居れば同じことが起きる——
 * ⚠⚠ <b>状態効果は他人に消される場所だという前提は変わらない。</b>
 * だから<b>動きのほうは効果に頼っていない</b>（消されても飛べなくならない）。
 *
 * <p>⚠ 何もしないので {@code isDurationEffectTick} は常に偽を返す
 * （毎tick 呼ばれる意味が無い）。
 */
public final class HoverEffect extends MobEffect {

  /** ⚠ バフ欄の粒の色。ブレイズの体の色に寄せた橙。 */
  private static final int COLOUR = 0xFFB44A;

  public HoverEffect() {
    super(MobEffectCategory.BENEFICIAL, COLOUR);
  }

  @Override
  public boolean isDurationEffectTick(int duration, int amplifier) {
    return false;
  }

  @Override
  public void applyEffectTick(LivingEntity entity, int amplifier) {
    // ⚠ 何もしない（表示のためだけの効果）。
  }
}
