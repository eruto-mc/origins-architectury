package net.erutobusiness.shiftingorigins;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffer;

/**
 * 商人には、村人の値上がりが乗らない（2026-09-09・あなたの決定）。
 *
 * <h2>値段はこう決まる（実物から採った）</h2>
 *
 * <p>Forge のパッチ（{@code MerchantOffer.java.patch}）に式がそのまま出ている:
 *
 * <pre>
 *   int j = max(0, floor(baseCostA.getCount() * demand * priceMultiplier));
 *   costA = baseCostA.copyWithCount(clamp(count + j + specialPriceDiff, 1, maxStackSize));
 * </pre>
 *
 * <p>{@code demand} が増えるのは {@code MerchantOffer.updateDemand()} で、中身は2行——
 * {@code demand = demand + uses - (maxUses - uses)}（1.20.1 の本体を逆アセンブルして確認）。
 * 呼ぶのは {@code Villager.restock()} と {@code catchUpDemand()} の2つ。
 *
 * <h2>⚠⚠ だから demand 側では書けない</h2>
 *
 * <p>⚠ {@code restock()} は<b>プレイヤーを引数に取らない</b>（村人の日課）。
 * ⚠⚠ <b>誰が取引したかを知らない場所で上がる</b>ので、「商人が取引しても上がらない」を
 * demand 側に書く道が無い。
 *
 * <p>⚠ プレイヤーごとに効く欄は {@code specialPriceDiff} ひとつ。
 * {@code Villager.updateSpecialPrices(Player)} が<b>取引を開くたびに</b>呼ばれ、
 * 評判と村の英雄がここで値引きを入れている。⚠ <b>Forge のイベントは無い</b>
 * （{@code Villager.java.patch} を全部読んで確かめた。足されているのは
 * 「しゃがみ中は取引しない」「職業名の名前空間」「雷で魔女になる」の3つだけ）。
 *
 * <h2>やり方</h2>
 *
 * <p>⚠ <b>demand で上がったぶんを、そのまま値引きに入れる</b>——上がった分ちょうど。
 * ⚠ <b>安くはならない</b>（基本の値段に戻るだけ）。
 *
 * <p>⚠ 二重に効かない: {@code specialPriceDiff} は取引をやめたときに
 * {@code resetSpecialPrices()} で 0 に戻り、開くたびに作り直される
 * （{@code startTrading} が {@code updateSpecialPrices} → {@code setTradingPlayer} の順に呼ぶ）。
 *
 * <p>⚠ 「尽きにくい品揃え」（{@link MerchantStock}）とは<b>別の場所</b>。
 * あちらは取引の直後に {@code uses} を戻すので、⚠ <b>補充のときの demand の上がり方が小さくなる</b>。
 * 向きは同じだが、⚠ <b>二重でも基本の値段より安くはならない</b>（上がった分だけ引くため）。
 */
public final class MerchantPrice {

  private MerchantPrice() {
  }

  /**
   * その村人の取引から、需要で上がったぶんを打ち消す。
   *
   * <p>⚠ 呼ぶのは {@code mixin/VillagerPriceMixin}（{@code updateSpecialPrices} の末尾）。
   */
  public static void cancelDemand(final Villager villager, final Player player) {

    if (!(player instanceof ServerPlayer server)
        || !ShiftingOrigins.Config.MERCHANT_STEADY_PRICE.get()
        || !ClassPowers.isMerchant(server)) {
      return;
    }
    for (final MerchantOffer offer : villager.getOffers()) {
      final int raised = Math.max(0, Mth.floor(
          (float) (offer.getBaseCostA().getCount() * offer.getDemand())
              * offer.getPriceMultiplier()));
      if (raised > 0) {
        offer.addToSpecialPriceDiff(-raised);
      }
    }
  }
}
