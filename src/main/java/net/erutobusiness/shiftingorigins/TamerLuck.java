package net.erutobusiness.shiftingorigins;

import net.erutobusiness.shiftingorigins.mixin.EntityRandomAccessor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;

/**
 * 調教師の「懐かれる質」——手なずけの抽選を2回振って、良いほうを取る（2026-09-09）。
 *
 * <h2>⚠⚠ なぜ「その生き物の乱数を差し替える」形なのか</h2>
 *
 * <p>⚠ <b>手なずけは生き物ごとに別の実装</b>で、共通の口が無い。
 * ⚠ Forge の {@code AnimalTameEvent} は<b>抽選に当たった後</b>にしか飛ばないので、
 * ⚠⚠ <b>確率には手が届かない。</b>
 *
 * <p>⚠ この構成には手なずけられる生き物を足す MOD が多い（{@code TamableAnimal} に触る jar は
 * <b>24 本</b>）。⚠ 1つずつ書くのは現実的でない。
 *
 * <p>⇒ ⚠ <b>全部が共通で使っているものが1つだけ在った</b>——<b>その生き物の
 * {@code RandomSource}</b>。20 の実装を逆アセンブルして、19 件がそれで振っていることを確かめた
 * （残る1件・ハムスターは抽選そのものが無い）。⚠ <b>だから乱数の側から効かせる。</b>
 *
 * <h2>窓は2つ</h2>
 *
 * <ul>
 *   <li>{@code Mob.mobInteract} … 右クリックで手なずける生き物ぜんぶ
 *       （バニラのオオカミ・ネコ・オウム、Alex's Mobs の 18 種ほか）</li>
 *   <li>⚠ {@code RunAroundLikeCrazyGoal.tick} … <b>馬・ロバ・ラバ</b>。
 *       ⚠ あちらは右クリックではなく<b>乗っているあいだ</b>に
 *       {@code nextInt(getMaxTemper()) < getTemper()} で振る（実物を逆アセンブルして確かめた）</li>
 * </ul>
 *
 * <p>⚠ <b>馬だけの特別扱いではない。</b> 仕組みは1つ（2回振る）で、
 * <b>その仕組みが届く窓を2つ用意している</b>だけ。
 *
 * <h2>⚠ 危ないところ</h2>
 *
 * <p>⚠ 差し替えている間は、⚠ <b>その生き物が振る他の乱数にも当たる</b>（音・粒子・落とし物）。
 * ⚠ <b>窓は右クリック1回ぶん／暴れる判定1回ぶん</b>なので影響は小さい側に倒れている。
 * ⚠ さらに<b>まだ懐いていない生き物にしか掛けない</b>。
 *
 * <p>⚠ 届かないものが在っても、⚠ <b>壊れ方は「その生き物だけ確率が変わらない」</b>。
 */
public final class TamerLuck {

  private TamerLuck() {
  }

  /**
   * この生き物に掛けてよいか。
   *
   * <p>⚠ 条件は3つ——⚠ <b>設定が入</b>／⚠ <b>相手がまだ懐いていない</b>／
   * ⚠ <b>その人が調教師</b>。
   */
  public static boolean shouldHelp(final Entity target, final Player player) {

    if (!(player instanceof ServerPlayer server)
        || target.level().isClientSide
        || !ShiftingOrigins.Config.BEASTMASTER_LUCK.get()) {
      return false;
    }
    if (isTamed(target)) {
      return false;
    }
    return ClassPowers.isBeastmaster(server);
  }

  /**
   * もう懐いているか。
   *
   * <p>⚠ <b>型で見る</b>——{@code TamableAnimal}（オオカミ型）と {@code AbstractHorse}（馬型）で
   * 判定のメソッドが違う。⚠ どちらでもない生き物（自前の手なずけを持つ MOD の一部）は
   * ⚠ <b>「まだ懐いていない」側に倒す</b>——掛けても失敗するだけで、害が無いため。
   */
  private static boolean isTamed(final Entity target) {
    if (target instanceof TamableAnimal tamable) {
      return tamable.isTame();
    }
    if (target instanceof AbstractHorse horse) {
      return horse.isTamed();
    }
    if (target instanceof OwnableEntity ownable) {
      return ownable.getOwnerUUID() != null;
    }
    return false;
  }

  /**
   * 乱数を包む。⚠ <b>既に包んであれば何もしない</b>（二重に掛けない）。
   *
   * @return 包んだなら true（戻すのは呼んだ側の役目）
   */
  public static boolean wrap(final Entity target) {
    final EntityRandomAccessor acc = (EntityRandomAccessor) target;
    final RandomSource now = acc.shiftingorigins$getRandom();
    if (now instanceof LuckyRandom) {
      return false;
    }
    acc.shiftingorigins$setRandom(new LuckyRandom(now));
    return true;
  }

  /** 包みを外す。⚠ <b>包まれていなければ何もしない。</b> */
  public static void unwrap(final Entity target) {
    final EntityRandomAccessor acc = (EntityRandomAccessor) target;
    if (acc.shiftingorigins$getRandom() instanceof LuckyRandom lucky) {
      acc.shiftingorigins$setRandom(lucky.unwrap());
    }
  }
}
