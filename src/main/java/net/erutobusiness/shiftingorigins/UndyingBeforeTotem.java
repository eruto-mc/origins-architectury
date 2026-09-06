package net.erutobusiness.shiftingorigins;

import io.github.edwinmindcraft.apoli.common.power.PreventDeathPower;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 種族の蘇りを、不死のトーテムより先に働かせる（2026-09-06・あなたの指示）。
 *
 * <p><b>直す前は逆だった。</b> バニラの並びを実物のバイトコードで読んだ結果
 * （{@code forge-1.20.1-47.4.0_mapped_official} の {@code LivingEntity}）:
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
 * <h2>⚠⚠ 1度目の直し方は空振りした（同じ日に部員が報告）</h2>
 *
 * <p>最初は Forge の {@code LivingUseTotemEvent} を打ち切る形で書いた。⚠ <b>当部では飛ばない。</b>
 * ⚠⚠ <b>{@code BetterTotemOfUndying} が {@code checkTotemDeathProtection} の頭へ
 * {@code @Inject(cancellable = true)} を刺し、{@code BTUUtils.canSaveFromDeath} の返り値を
 * そのまま返している</b>（実物の 18 バイトの注入を逆アセンブルして確認）。
 * ⚠ その {@code canSaveFromDeath} は<b>トーテムを自分で減らし</b>（{@code shrink}）、
 * ⚠⚠ <b>{@code ForgeHooks.onLivingUseTotem} を1度も呼んでいない</b>。
 * ⚠ だから<b>バニラの本体ごと飛ばされ、当部の口は開かないまま</b>だった。
 *
 * <p>⚠ <b>これは当部で5件目の同じ型</b>——上流やバニラの構造を別の MOD が差し替えていて、
 * そこにぶら下げた仕掛けが黙って死ぬ（農夫・木こり・鍛冶屋・聖職者、そしてこれ）。
 *
 * <h2>やり方（当てる場所を、奪われない所へ移した）</h2>
 *
 * <p>⚠ <b>トーテムの判定より手前で終わらせる。</b> {@code LivingDamageEvent} は
 * {@code actuallyHurt} の中で出て、⚠ <b>その後に体力が引かれる</b>:
 *
 * <pre>
 *   actuallyHurt の  38〜56   吸収（黄ハート）を先に引く
 *   actuallyHurt の 121       ForgeHooks.onLivingDamage(...)   ← ここに当てる
 *   actuallyHurt の 128       ifeq -&gt; 167                      ← 0 が返れば体力を引かない
 *   actuallyHurt の 147       setHealth(体力 - 量)
 * </pre>
 *
 * <p>⚠ <b>吸収は 121 より前で引かれている</b>ので、ここでの量は<b>体力から直に引かれる値</b>。
 * ⚠ だから「{@code 量 >= 体力}」が致命の判定になる。
 * ⚠ 打ち切れば 147 に到達しないので、⚠⚠ <b>そもそも死なず、トーテムの判定まで進まない。</b>
 * ⚠ <b>MOD が誰も差し替えていない所</b>なので、{@code BetterTotemOfUndying} が居ても効く。
 *
 * <p>⚠ <b>優先度は {@code LOWEST}</b>——⚠ 他の MOD が量を減らし終えた後の<b>最終の値</b>で
 * 判定する。早く割り込むと、<b>減った結果なら死なない一撃</b>を致命と読み違える。
 *
 * <h2>⚠⚠ 判定を書き写していない</h2>
 *
 * <p>⚠ 1度目は上流の判定を<b>自分の側へ写して</b>いた。⚠⚠ <b>写すとずれる。</b>
 * ⚠ ここでは {@code PreventDeathPower.tryPreventDeath} を<b>そのまま呼ぶ</b>。
 * 上流の handler が呼ぶのと同じ関数なので、⚠ <b>条件の食い違いが原理的に起きない</b>。
 * ⚠ 真を返したときだけ打ち切る——<b>蘇りが実際に走った時だけ、damage を無かったことにする</b>。
 *
 * <p>⚠ 蘇りが働かないとき（腐肉と骨が満ちていない・そもそも持っていない）は<b>何もしない</b>。
 * ⚠⚠ <b>トーテムは今までどおり守る。</b> 守りが減ることは無い。
 */
public final class UndyingBeforeTotem {

  private UndyingBeforeTotem() {
  }

  /**
   * 体力が引かれる直前。
   *
   * <p>⚠ {@code LOWEST} は「他の全員が量を決め終わった後」という意味で、
   * ⚠ <b>止める強さの話ではない</b>。
   */
  @SubscribeEvent(priority = EventPriority.LOWEST)
  public static void onDamage(final LivingDamageEvent event) {

    if (!ShiftingOrigins.Config.UNDYING_BEFORE_TOTEM.get()
        || !(event.getEntity() instanceof ServerPlayer player)
        || event.getAmount() < player.getHealth()) {
      return;
    }
    // ⚠⚠ **上流の関数をそのまま呼ぶ。** 真＝蘇りが走った（体力1・溜めた分は空・回復と効果）。
    if (PreventDeathPower.tryPreventDeath(player, event.getSource(), event.getAmount())) {
      // ⚠ 打ち切る＝体力を引かない。⚠ **死なないので、トーテムの判定へ進まない。**
      event.setCanceled(true);
    }
  }
}
