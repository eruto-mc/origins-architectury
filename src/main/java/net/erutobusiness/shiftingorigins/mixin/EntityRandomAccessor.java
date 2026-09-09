package net.erutobusiness.shiftingorigins.mixin;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * その生き物の乱数を差し替えるための入口（2026-09-09）。
 *
 * <p>⚠ {@code Entity.random} は {@code final} なので、{@link Mutable} を付けて書けるようにする。
 * ⚠ <b>読み書きするだけ</b>で、振る舞いは何も変えない。
 *
 * <p>⚠ 使うのは {@link net.erutobusiness.shiftingorigins.TamerLuck} 1か所だけ。
 */
@Mixin(Entity.class)
public interface EntityRandomAccessor {

  @Mutable
  @Accessor("random")
  void shiftingorigins$setRandom(RandomSource random);

  @Accessor("random")
  RandomSource shiftingorigins$getRandom();
}
