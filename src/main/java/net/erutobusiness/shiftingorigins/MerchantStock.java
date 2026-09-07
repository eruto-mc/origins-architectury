package net.erutobusiness.shiftingorigins;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraftforge.event.entity.player.TradeWithVillagerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 商人が取引すると、<b>その取引の在庫が確率で減らない</b>（2026-09-07）。
 *
 * <p>⚠⚠ <b>上流は「絶対に減らない」だった。</b> {@code origins-classes:trade_availability}
 * （尽きない品揃え）の実体は {@code origins_classes:infinite_trade} という中身の無い power で、
 * 働いているのは {@code dev.limonblaze.originsclasses.core.mixin.common.minecraft
 * .AbstractVillagerMixin} が {@code notifyTrade} の末尾で {@code --offer.uses} すること。
 * ⚠ 1回ぶん増えた使用回数を、そのまま戻している。
 *
 * <p><b>やり方</b>: 上流の power は無害な定義で上書きし
 * （{@code data/origins-classes/powers/trade_availability.json} の
 * {@code apoli:simple} ＋ {@code loading_priority: 100}）、
 * ⚠ <b>職業を直に見るこちらが、確率で同じことをする。</b>
 * ⚠ 上流の power を持たなくなるので、⚠⚠ <b>上流の mixin は一度も発火しない</b>
 * （あちらの判定は {@code IPowerContainer.hasPower(…, INFINITE_TRADE)} 1本だけ）。
 * ⚠ {@code hidden} は立てていないので、能力の一覧と日本語の説明はこれまでどおり出る。
 *
 * <p>⚠⚠ <b>mixin を使わない</b>（2026-09-07・あなたの指摘「わざわざ fork してるなら
 * mixin じゃなく現物をいじればいい」）。⚠ <b>Forge が同じ場所にイベントを置いている</b>——
 * {@code patches/net/minecraft/world/entity/npc/AbstractVillager.java.patch} が
 * {@code notifyTrade} の<b>最後の1行</b>に
 * {@code EVENT_BUS.post(new TradeWithVillagerEvent(this.tradingPlayer, offer, this))} を足しており、
 * ⚠ <b>{@code @At("TAIL")} で入るのと同じ位置・同じ時点</b>（使用回数が増えた後）。
 * ⚠ 上流 origins-classes がここを mixin でやっているのは、
 * ⚠⚠ <b>このイベントが Forge 47.1 の時点では無かったから</b>（47.3 の patch には在り、
 * 47.1 の patch には無い。両方の userdev を開いて確かめた）。
 *
 * <p>⚠ イベントは<b>サーバ側だけ</b>で飛ぶ（Forge の javadoc が明記）。
 *
 * <p>⚠⚠ <b>datapack でやらない理由</b>: {@code hasPower} は「有効な power か」を毎回見るので、
 * power の JSON へ {@code apoli:predicate} ＋ {@code minecraft:random_chance} を足せば
 * datapack だけでも確率にはなる。⚠ <b>しかしそれは jar と datapack が同じ id を
 * 別々に定義する形</b>で、{@code selection/design/origins-consolidation.md} が
 * <b>直すべき問題として挙げている5件</b>と同じ型を1件増やす。
 *
 * <p>⚠⚠ <b>画面のずれ</b>: 取引の残り回数は<b>クライアントも自分で数えている</b>
 * （{@code ClientSideMerchant.notifyTrade}）。⚠ 上流はそこを
 * {@code OriginsClasses.INFINITE_TRADER} という真偽値1つで合わせていたが、
 * ⚠⚠ <b>その値が決まるのは「村人を右クリックした瞬間の1回」だけ</b>
 * （{@code PowerEventHandler.onInteractEntity}）。⚠ 確率にすると、その1回の目と
 * 取引ごとの目が食い違い、<b>売り切れの×が出る時期がサーバとずれる</b>。
 * ⚠ ここでは上流の power を殺してあるので<b>クライアントは素のバニラとして数え</b>、
 * ⚠ <b>減らさなかったときだけサーバから品書きを送り直して合わせる</b>（{@link #resend}）。
 *
 * <p>⚠ バニラの取引画面に<b>残り回数の数字は無い</b>（売り切れかどうかの×だけ）ので、
 * 部員から見える印は「売り切れになるのが遅い」だけになる。
 */
public final class MerchantStock {

  private MerchantStock() {
  }

  /** 取引が1回成立した直後（{@code AbstractVillager.notifyTrade} の最後）に飛ぶ。 */
  @SubscribeEvent
  public static void onTrade(final TradeWithVillagerEvent event) {
    afterTrade(event.getAbstractVillager(), event.getEntity(), event.getMerchantOffer());
  }

  /**
   * ⚠ 判定の順は<b>安い順</b>: 設定 → 使用回数 → 相手がサーバのプレイヤーか → 職業。
   * ⚠ 職業の読み出しは capability を触るので最後に置く。
   *
   * <p>⚠ {@code offer.uses} は {@code notifyTrade} が先に1つ増やしているので、
   * ここでは必ず 1 以上。⚠ それでも 0 を見ているのは、⚠⚠ <b>他の MOD が同じ
   * {@code notifyTrade} の末尾でさらに減らしていた場合に、負の値にしないため</b>。
   */
  private static void afterTrade(final AbstractVillager merchant, final Player customer,
      final MerchantOffer offer) {

    final double keep = ShiftingOrigins.Config.MERCHANT_STOCK_KEPT.get();
    if (keep <= 0.0D || offer.uses <= 0) {
      return;
    }
    if (!(customer instanceof ServerPlayer server) || !ClassPowers.isMerchant(server)) {
      return;
    }
    if (server.getRandom().nextDouble() >= keep) {
      return;
    }
    offer.uses--;
    resend(merchant, server);
  }

  /**
   * 減らさなかったことを、開いたままの取引画面へ伝える。
   *
   * <p>⚠⚠ <b>これが無いとクライアントだけ回数が進む。</b> クライアントは自分の写しを
   * 持っており、売り切れた取引は<b>選べなくなる</b>ので、
   * ⚠ サーバはまだ売れると思っているのに部員は買えない、という形で出る
   * （画面を開き直すまで直らない）。
   *
   * <p>⚠ 送る中身はバニラの {@code Merchant.openTradingScreen} と同じ並び。
   * ⚠ 等級だけは村人しか持たないので、行商人は 1（あちらも同じ値を渡している）。
   */
  private static void resend(final AbstractVillager merchant, final ServerPlayer server) {

    if (!(server.containerMenu instanceof MerchantMenu menu)) {
      return;
    }
    final MerchantOffers offers = merchant.getOffers();
    if (offers.isEmpty()) {
      return;
    }
    final int level = merchant instanceof Villager villager
        ? villager.getVillagerData().getLevel()
        : 1;
    server.sendMerchantOffers(menu.containerId, offers, level, merchant.getVillagerXp(),
        merchant.showProgressBar(), merchant.canRestock());
  }
}
