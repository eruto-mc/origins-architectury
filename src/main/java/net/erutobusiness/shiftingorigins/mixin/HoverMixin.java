package net.erutobusiness.shiftingorigins.mixin;

import net.erutobusiness.shiftingorigins.Hover;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 浮遊を、状態効果を経由せずに {@code LivingEntity.travel} へ届ける。
 *
 * <p>⚠ <b>やり方</b>: {@code travel} の中の「浮遊が付いているか」の問い合わせに、
 * power が active なら真を返す。⚠ 強さを読む側にも<b>作り物の効果</b>を返す。
 * ⚠⚠ <b>上がり方の計算はバニラのまま</b>——こちらは答えを差し替えるだけ。
 *
 * <p>⚠ <b>なぜ位置や速度を直接いじらないか</b>: バニラの浮遊は
 * {@code travel} の中で重力・摩擦と同じ場所で速度に混ぜている。
 * ⚠ 外から毎tick 速度を書くと、重力の処理と取り合って<b>ガクつく</b>。
 * ⚠ 同じ場所へ同じ形で入れるのが、いちばん手触りが揃う。
 *
 * <p>⚠ <b>両側で走る</b>（{@code travel} はクライアントも通る）。
 * ⚠ 片側だけだと、クライアントの予測とサーバの判定が食い違って引き戻される。
 *
 * <p>⚠ 効かせる相手は datapack の {@code world3:hover} の {@code lift}
 * （型 {@code shiftingorigins:hover}・条件は H のトグル）。
 * ⚠⚠ <b>power と mixin は対で1つの機能。</b>片方だけ消さない。
 *
 * <p>経緯（農夫の加護が浮遊を剥がしていた件）は {@link Hover} の説明と
 * {@code mod-notes/blazeborn-nether.md}。
 */
@Mixin(LivingEntity.class)
public abstract class HoverMixin {

  /**
   * 「浮遊が付いているか」への答え。
   *
   * <p>⚠ {@code travel} は低速落下の問い合わせにも同じメソッドを使うので、
   * ⚠ <b>順番（ordinal）ではなく、渡された効果が浮遊かどうかで見分ける</b>。
   */
  @Redirect(method = "travel", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/world/entity/LivingEntity;hasEffect(Lnet/minecraft/world/effect/MobEffect;)Z"))
  private boolean shiftingorigins$hoverCountsAsLevitation(LivingEntity self, MobEffect effect) {
    if (effect == MobEffects.LEVITATION && Hover.isActive(self)) {
      return true;
    }
    return self.hasEffect(effect);
  }

  /**
   * 強さを読む側。
   *
   * <p>⚠ <b>本物の効果が在ればそちらを優先する</b>（シャルカーに撃たれた分を消さない）。
   * ⚠ 無いときだけ作り物を返す。
   */
  @Redirect(method = "travel", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/world/entity/LivingEntity;getEffect(Lnet/minecraft/world/effect/MobEffect;)Lnet/minecraft/world/effect/MobEffectInstance;"))
  private MobEffectInstance shiftingorigins$hoverAmplifier(LivingEntity self, MobEffect effect) {
    MobEffectInstance real = self.getEffect(effect);
    if (real == null && effect == MobEffects.LEVITATION && Hover.isActive(self)) {
      return Hover.asLevitation();
    }
    return real;
  }
}
