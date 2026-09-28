package net.erutobusiness.shiftingorigins;

import io.github.edwinmindcraft.apoli.api.component.IPowerContainer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Comparator;

/**
 * プレイヤーが配った悪い効果（負傷・ウィザーなど）で傷ついた相手に、<b>そのプレイヤーを「やった人」として記録する</b>。
 *
 * <h2>何が困っていたか</h2>
 *
 * 効果のダメージ（負傷のポーション・ウィザー）は、バニラの作りでは<b>ダメージの側に「やった人」を持たない</b>
 * （{@code damageSources().magic()} / {@code wither()}）。なので、次の 2 つで相手が倒れても:
 *
 * <ul>
 *   <li>Pixie の Mischief の「ニセの爆発」 … 使った本人として {@code effect give @e[distance=..2.5] minecraft:instant_damage}
 *       を撃つ（{@code mdvlorigins/pixie_fake_explosion.mcfunction}）</li>
 *   <li>Revenant の Hellraiser … 骨の壁の足元に、<b>持ち主の無い</b>ウィザー III の雲を置く
 *       （{@code mdvlorigins/hellraiser.mcfunction}。壁のスケルトンとして {@code summon area_effect_cloud}）</li>
 * </ul>
 *
 * 死亡メッセージに使った人の名前が出ず、倒した敵は経験値も、プレイヤーが倒したときだけの落とし物も出さなかった。
 *
 * <h2>やり方（上流の定義は書き換えない）</h2>
 *
 * <ol>
 *   <li>{@code MobEffectEvent.Added} で、効果を配った元（{@code getEffectSource()}）を見る。
 *       元がプレイヤーなら、または持ち主がプレイヤーの雲なら、受けた相手に
 *       {@code setLastHurtByPlayer} と {@code setLastHurtByMob} を入れ、記録の残る時間を殴られたときと同じ 100 tick にする
 *       （バニラの {@code setLastHurtByPlayer} は時間に「生まれてからの tick 数」を入れるため）。本人の味方には付けない。
 *       ⇒ 死亡メッセージは「〜と戦いながら」の形（{@code death.attack.*.player}）で名前が出て、
 *       経験値とプレイヤーが倒したときだけの落とし物も出る（どちらもバニラが {@code lastHurtByPlayer} を見る）。
 *       ⚠ 記録は 5 秒（100 tick）で切れる。雲のウィザーは 2 秒ごとにかけ直すので、そのたびに入れ直される</li>
 *   <li>Hellraiser の雲には持ち主が無いので、出てきたとき（{@code EntityJoinLevelEvent}）に、
 *       近くに骨の壁（{@code necroskelwall} の印）が在れば、Hellraiser を持つ一番近いプレイヤー（4 ブロック以内）を持ち主にする。
 *       壁は使った本人の周り半径 3 に並ぶので、本人は必ずこの範囲に入る</li>
 * </ol>
 *
 * <p>悪い効果（{@code MobEffectCategory.HARMFUL}）だけを見る。使った本人には記録しない。
 * ⚠ 当てた相手（ゾンビピグリン・アイアンゴーレムなど）は、殴ったときと同じく使った人を狙うようになる。
 */
public final class EffectCredit {

  private static final org.apache.logging.log4j.Logger LOG =
      org.apache.logging.log4j.LogManager.getLogger("shifting_origins/EffectCredit");
  private static final String WALL_TAG = "necroskelwall";
  private static final ResourceLocation HELLRAISER =
      new ResourceLocation("medievalorigins", "revenant/hellraiser");

  private EffectCredit() {
  }

  @SubscribeEvent
  public static void onEffectAdded(MobEffectEvent.Added event) {
    LivingEntity target = event.getEntity();
    if (target.level().isClientSide()) {
      return;
    }
    if (event.getEffectInstance().getEffect().getCategory() != MobEffectCategory.HARMFUL) {
      return;
    }
    Player who = responsible(event.getEffectSource());
    if (who == null || who == target) {
      return;
    }
    // ⚠ 本人の味方（呼び出した従者・手なずけた動物・同じチーム）には付けない。付けると本人を狙いだす
    if (target.isAlliedTo(who) || who.isAlliedTo(target)) {
      return;
    }
    target.setLastHurtByPlayer(who);
    // ⚠⚠ バニラの setLastHurtByPlayer（m_6598_）は、記録の残る時間（f_20889_）に「生まれてからの tick 数」（f_19797_）を入れる。
    //    生まれたばかりの相手では 0 になってすぐ消え、長く生きている相手では何時間も残る（あとの落下死まで「〜と戦いながら」になる）。
    //    殴られたとき（hurt）と同じ 100（5 秒）を入れ直す（2026-09-28 に javap で確かめた）
    setHurtByPlayerTime(target, 100);
    target.setLastHurtByMob(who);
    LOG.debug("効果 {} で {} に、やった人 {} を付けた", event.getEffectInstance().getEffect().getDescriptionId(),
        target.getName().getString(), who.getName().getString());
  }

  private static java.lang.reflect.Field hurtByPlayerTime;
  private static boolean hurtByPlayerTimeBroken;

  /** {@code LivingEntity.lastHurtByPlayerTime}（SRG 名 f_20889_）を書く。読めなければ 1 回だけ警告して、以後は触らない。 */
  private static void setHurtByPlayerTime(LivingEntity target, int ticks) {
    if (hurtByPlayerTimeBroken) {
      return;
    }
    try {
      if (hurtByPlayerTime == null) {
        hurtByPlayerTime = net.minecraftforge.fml.util.ObfuscationReflectionHelper.findField(LivingEntity.class, "f_20889_");
      }
      hurtByPlayerTime.setInt(target, ticks);
    } catch (RuntimeException | IllegalAccessException e) {
      hurtByPlayerTimeBroken = true;
      LOG.warn("⚠ lastHurtByPlayerTime を書けなかった。やった人の記録の残る時間はバニラのまま（生まれてからの tick 数）: {}", e.toString());
    }
  }

  /** 効果を配った元から、責任のあるプレイヤーを引く。プレイヤーそのものか、持ち主がプレイヤーの雲。 */
  private static Player responsible(Entity source) {
    if (source instanceof Player p) {
      return p;
    }
    if (source instanceof AreaEffectCloud cloud && cloud.getOwner() instanceof Player p) {
      return p;
    }
    return null;
  }

  @SubscribeEvent
  public static void onJoin(EntityJoinLevelEvent event) {
    if (event.getLevel().isClientSide() || !(event.getEntity() instanceof AreaEffectCloud cloud)) {
      return;
    }
    if (cloud.getOwner() != null) {
      return;
    }
    boolean nearWall = !event.getLevel().getEntities(cloud, cloud.getBoundingBox().inflate(1.0),
        e -> e.getTags().contains(WALL_TAG)).isEmpty();
    if (!nearWall) {
      return;
    }
    event.getLevel().getEntitiesOfClass(ServerPlayer.class, cloud.getBoundingBox().inflate(4.0),
            p -> IPowerContainer.get(p).map(c -> c.hasPower(HELLRAISER)).orElse(false))
        .stream()
        .min(Comparator.comparingDouble(p -> p.distanceToSqr(cloud)))
        .ifPresent(p -> {
          cloud.setOwner(p);
          LOG.debug("骨の壁の雲に持ち主 {} を付けた", p.getName().getString());
        });
  }
}
