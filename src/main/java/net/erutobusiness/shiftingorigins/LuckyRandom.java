package net.erutobusiness.shiftingorigins;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;

/**
 * 「2回振って、良いほうを取る」乱数（2026-09-09）。
 *
 * <p>⚠ 全部を元の乱数へ流す。⚠ <b>変えるのは3つだけ</b>——{@link #nextInt(int)} と
 * {@link #nextFloat()} は<b>小さいほう</b>、{@link #nextBoolean()} は
 * <b>どちらかが真なら真</b>。
 *
 * <h2>なぜ3つなのか（実物を数えた）</h2>
 *
 * <p>手なずけを実装している 20 の class を逆アセンブルし、抽選の直前を読んだ。
 * ⚠⚠ <b>19 件が「その生き物の RandomSource」で振っており、向きもそろっていた</b>:
 *
 * <ul>
 *   <li>バニラ（オオカミ・ネコ・オウム）… {@code random.nextInt(3) == 0}</li>
 *   <li>Alex's Mobs 12 種 … {@code nextInt(n)}／1 種 … {@code nextBoolean()}</li>
 *   <li>Alex's Mobs 5 種（グリズリー・アライグマ・カラス・ゴリラ・コスモー）
 *       … {@code nextFloat() < 0.3}</li>
 *   <li>Critters and Companions・Wilder Nature … {@code nextBoolean()} と {@code nextInt(n)}</li>
 *   <li>⚠ 馬 … {@code RunAroundLikeCrazyGoal.tick} の
 *       {@code random.nextInt(getMaxTemper()) < getTemper()}</li>
 * </ul>
 *
 * <p>⚠ <b>どれも「小さい・真」が成功</b>なので、⚠ <b>良いほうを取れば確率が上がる。</b>
 *
 * <h2>⚠ 0 に固定しない</h2>
 *
 * <p>⚠⚠ {@code nextInt(n)} をいつも 0 にすると<b>必ず成功</b>になり、抽選が消える。
 * ⚠ 「2回振って良いほう」なら確率は残る——オオカミは 1/3 → 5/9、
 * グリズリーは 0.3 → 0.51。⚠ <b>手なずけが「たまに失敗するもの」であることは変わらない。</b>
 *
 * <p>⚠ 当部には同じ考え方の先例が在る——釣り人の「入れ食い」は<b>戦利品の表を2回振る</b>。
 */
public final class LuckyRandom implements RandomSource {

  private final RandomSource inner;

  public LuckyRandom(final RandomSource inner) {
    this.inner = inner;
  }

  /** 包む前の乱数（戻すときに使う）。 */
  public RandomSource unwrap() {
    return this.inner;
  }

  // ── ⚠ ここだけ2回振る ────────────────────────────────────────────

  @Override
  public int nextInt(final int bound) {
    return Math.min(this.inner.nextInt(bound), this.inner.nextInt(bound));
  }

  @Override
  public float nextFloat() {
    return Math.min(this.inner.nextFloat(), this.inner.nextFloat());
  }

  @Override
  public boolean nextBoolean() {
    return this.inner.nextBoolean() || this.inner.nextBoolean();
  }

  // ── 残りは素通し ────────────────────────────────────────────────

  @Override
  public RandomSource fork() {
    return this.inner.fork();
  }

  @Override
  public PositionalRandomFactory forkPositional() {
    return this.inner.forkPositional();
  }

  @Override
  public void setSeed(final long seed) {
    this.inner.setSeed(seed);
  }

  @Override
  public int nextInt() {
    return this.inner.nextInt();
  }

  @Override
  public long nextLong() {
    return this.inner.nextLong();
  }

  @Override
  public double nextDouble() {
    return this.inner.nextDouble();
  }

  @Override
  public double nextGaussian() {
    return this.inner.nextGaussian();
  }
}
