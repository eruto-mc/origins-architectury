package net.erutobusiness.shiftingorigins;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Origins Classes の一括破壊（木こりの伐採・datapack で足した鉱脈掘り）を、
 * 同じ tick に全部ではなく少しずつ壊すようにする。
 *
 * <p>なぜ要るか: Origins Classes は最大255ブロックを1tickで壊す。小さい木なら気にならないが、
 * 巨木や大鉱脈は一瞬で消えて手応えが無く、負荷も1tickに集中する。
 *
 * <p>やり方: {@code MultiMinePower.apply} が返すブロック一覧を横取りして**空の一覧を返し**、
 * 実際の破壊はこちらのキューが数tickかけて行う。破壊は
 * {@code ServerPlayerGameMode.destroyBlock} を通すので、ドロップ・経験値・道具の消耗・
 * 他MODのイベントは手掘りとまったく同じに走る。
 */
@Mod(ShiftingOrigins.MOD_ID)
public final class ShiftingOrigins {

  public static final String MOD_ID = "shiftingorigins";

  /**
   * 鉱脈用の一括破壊 power。Origins Classes の `MultiMinePower` を**そのまま使い、
   * 探索だけ差し替える**（`apply` は `instanceof MultiMinePower` で工場を拾うので、
   * こちらの工場も同じ経路で動く）。木こりは上流のまま触らない。
   */
  public static final net.minecraftforge.registries.DeferredRegister<
      io.github.edwinmindcraft.apoli.api.power.factory.PowerFactory<?>> POWER_FACTORIES =
      net.minecraftforge.registries.DeferredRegister.create(
          io.github.edwinmindcraft.apoli.api.registry.ApoliRegistries.POWER_FACTORY_KEY, MOD_ID);

  public static final net.minecraftforge.registries.RegistryObject<
      dev.limonblaze.originsclasses.common.apoli.power.MultiMinePower> ORE_VEIN =
      POWER_FACTORIES.register("vein_mine",
          () -> new dev.limonblaze.originsclasses.common.apoli.power.MultiMinePower(
              OreVeinRange::find));

  /**
   * 浮遊（H の入切で、入れている間ずっと上がる）。
   *
   * <p>⚠ <b>印だけの power</b>——上げる処理は {@code mixin/HoverMixin} が
   * {@code LivingEntity.travel} の中でやる。⚠ この power が active かどうかだけを見る。
   * ⚠ 状態効果を使わないのは、⚠⚠ <b>農夫の加護が有害扱いの効果を毎tick 消すから</b>
   * （{@link Hover} の説明）。
   */
  public static final net.minecraftforge.registries.RegistryObject<
      io.github.edwinmindcraft.apoli.common.power.DummyPower> HOVER =
      POWER_FACTORIES.register("hover",
          io.github.edwinmindcraft.apoli.common.power.DummyPower::new);

  /**
   * 溶岩を水と同じように泳ぐ（{@link LavaSwim}）。
   *
   * <p>⚠ <b>印だけの power</b>——泳ぎは {@code mixin/FluidTypeMixin} と
   * {@code mixin/LavaSwimMixin} が受け持つ。⚠ この power が active かどうかだけを見る。
   */
  public static final net.minecraftforge.registries.RegistryObject<
      io.github.edwinmindcraft.apoli.common.power.DummyPower> LAVA_SWIM =
      POWER_FACTORIES.register("lava_swim",
          io.github.edwinmindcraft.apoli.common.power.DummyPower::new);

  /**
   * 戦士の3つと釣り人の1つ（2026-09-06）。
   *
   * <p>⚠ <b>どれも印だけの power</b>——中身は Java が持つ（{@link WarriorCombat} と
   * {@code mixin/PlayerShieldMixin}・{@code mixin/FishingHookMixin}）。
   * ⚠ apoli には<b>体力を見る条件も、盾で受けた状態も、釣りの待ち時間も無い</b>ので、
   * data 側では書けない。⚠ それでも power として登録するのは、
   * <b>種族・職業の説明画面に名前と説明を出すため</b>（浮遊・溶岩泳ぎと同じ型）。
   */
  public static final net.minecraftforge.registries.RegistryObject<
      io.github.edwinmindcraft.apoli.common.power.DummyPower> SHIELD_MASTER =
      POWER_FACTORIES.register("shield_master",
          io.github.edwinmindcraft.apoli.common.power.DummyPower::new);

  public static final net.minecraftforge.registries.RegistryObject<
      io.github.edwinmindcraft.apoli.common.power.DummyPower> RIPOSTE =
      POWER_FACTORIES.register("riposte",
          io.github.edwinmindcraft.apoli.common.power.DummyPower::new);

  public static final net.minecraftforge.registries.RegistryObject<
      io.github.edwinmindcraft.apoli.common.power.DummyPower> LAST_STAND =
      POWER_FACTORIES.register("last_stand",
          io.github.edwinmindcraft.apoli.common.power.DummyPower::new);

  public static final net.minecraftforge.registries.RegistryObject<
      io.github.edwinmindcraft.apoli.common.power.DummyPower> QUICK_BITE =
      POWER_FACTORIES.register("quick_bite",
          io.github.edwinmindcraft.apoli.common.power.DummyPower::new);

  /**
   * ⚠⚠ <b>画面に出ていなかった2つ</b>（2026-09-06・あなたの指示
   * 「画面の能力一覧に意図せず出てきてないものはすべて出すようにして」）。
   *
   * <p>どちらも Java が<b>職業を直に見て</b>働いていた（{@code ClassPowers.isCook} /
   * {@code isLumberjack}）。⚠ 画面は power の一覧しか描かないので、
   * ⚠⚠ <b>存在ごと見えていなかった。</b>
   *
   * <ul>
   *   <li><b>手際</b>（料理人）… 近くの調理台が2倍。⚠ <b>料理人の一番大きな能力</b>なのに、
   *       職業を選ぶ画面に一行も出ていなかった</li>
   *   <li><b>芽吹かせる</b>（木こり）… 苗木の骨粉が1個で木になる。
   *       ⚠ <b>同じ日に私が足したときに付け忘れた</b></li>
   * </ul>
   *
   * <p>⚠ <b>挙動は1ミリも変えない。</b> 判定を power へ移すのではなく、
   * <b>印として並べて配るだけ</b>にしてある（既存の Java はそのまま職業を見る）。
   * ⚠ そうしないと、片方だけ配られた状態で挙動が変わりうる。
   */
  public static final net.minecraftforge.registries.RegistryObject<
      io.github.edwinmindcraft.apoli.common.power.DummyPower> KITCHEN_HAND =
      POWER_FACTORIES.register("kitchen_hand",
          io.github.edwinmindcraft.apoli.common.power.DummyPower::new);

  // ⚠ 鍛冶屋の2つ（2026-09-06）。⚠ **働いているのは Java の側**で、ここは画面へ並べる印。
  public static final net.minecraftforge.registries.RegistryObject<
      io.github.edwinmindcraft.apoli.common.power.DummyPower> BLACKSMITH_TOOLS_POWER =
      POWER_FACTORIES.register("blacksmith_tools",
          io.github.edwinmindcraft.apoli.common.power.DummyPower::new);

  public static final net.minecraftforge.registries.RegistryObject<
      io.github.edwinmindcraft.apoli.common.power.DummyPower> BLACKSMITH_FURNACE_POWER =
      POWER_FACTORIES.register("blacksmith_furnace",
          io.github.edwinmindcraft.apoli.common.power.DummyPower::new);

  public static final net.minecraftforge.registries.RegistryObject<
      io.github.edwinmindcraft.apoli.common.power.DummyPower> SAPLING_BONEMEAL =
      POWER_FACTORIES.register("sapling_bonemeal",
          io.github.edwinmindcraft.apoli.common.power.DummyPower::new);

  /**
   * ⚠⚠ <b>残っていた4つの印を、当部の型へそろえた</b>（2026-09-09・あなたの指示
   * 「そもそも動くのを Java に統一して Java を機械で引ける手を考えるべきなのでは？」）。
   *
   * <p>それまでこの4つは {@code apoli:simple} を書いていた。⚠ <b>中身は同じ
   * {@link io.github.edwinmindcraft.apoli.common.power.DummyPower}</b> なので
   * <b>挙動は1ミリも変わらない</b>——変わるのは<b>機械から見分けが付くこと</b>だけ。
   *
   * <p>⚠ <b>なぜ見分けが要るか</b>: {@code apoli:simple} を当部は2通りに使っていた。
   * <ul>
   *   <li>働いているのは Java の側で、JSON は<b>一覧に名前を出すための印</b>（生きている）</li>
   *   <li>上流の能力を<b>殺すため</b>に無害な内容で上書きした残骸（何も働かない）</li>
   * </ul>
   * ⚠⚠ <b>JSON を見ただけでは、この2つを見分けられなかった。</b> 実際
   * {@code origins-classes:explorer_kit}（開始装備を殺した残骸）は、
   * <b>人が目で見つけるまで検査を素通りしていた</b>。
   *
   * <p>⇒ 印は当部の型にする。型はここに在るので<b>登録に無い型を書けば起動時に落ちる</b>＝
   * ⚠ <b>「当部の名前空間で {@code apoli:simple} なら残骸」と機械が言い切れる</b>
   * （{@code dev/verify/check_origin_powers.py} の②）。
   */
  public static final net.minecraftforge.registries.RegistryObject<
      io.github.edwinmindcraft.apoli.common.power.DummyPower> POTION_SHARING =
      POWER_FACTORIES.register("potion_sharing",
          io.github.edwinmindcraft.apoli.common.power.DummyPower::new);

  public static final net.minecraftforge.registries.RegistryObject<
      io.github.edwinmindcraft.apoli.common.power.DummyPower> SKELETON_SHY =
      POWER_FACTORIES.register("skeleton_shy",
          io.github.edwinmindcraft.apoli.common.power.DummyPower::new);

  /** 木こりの「板の取り方」。⚠ 働いているのは {@link LumberjackPlanks}。 */
  public static final net.minecraftforge.registries.RegistryObject<
      io.github.edwinmindcraft.apoli.common.power.DummyPower> EXTRA_PLANKS =
      POWER_FACTORIES.register("extra_planks",
          io.github.edwinmindcraft.apoli.common.power.DummyPower::new);

  /** 商人の「尽きにくい品揃え」。⚠ 働いているのは {@link MerchantStock}。 */
  public static final net.minecraftforge.registries.RegistryObject<
      io.github.edwinmindcraft.apoli.common.power.DummyPower> MERCHANT_STOCK =
      POWER_FACTORIES.register("merchant_stock",
          io.github.edwinmindcraft.apoli.common.power.DummyPower::new);

  /** 浮遊のアイコンを出すためだけの状態効果（{@link HoverEffect}）。 */
  public static final net.minecraftforge.registries.DeferredRegister<
      net.minecraft.world.effect.MobEffect> EFFECTS =
      net.minecraftforge.registries.DeferredRegister.create(
          net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS, MOD_ID);

  public static final net.minecraftforge.registries.RegistryObject<
      net.minecraft.world.effect.MobEffect> HOVER_EFFECT =
      EFFECTS.register("hover", HoverEffect::new);

  public ShiftingOrigins() {
    POWER_FACTORIES.register(FMLJavaModLoadingContext.get().getModEventBus());
    EFFECTS.register(FMLJavaModLoadingContext.get().getModEventBus());
    // ⚠ 浮遊が入っていることをバフ欄へ出す係。⚠ **動きとは無関係**（止まっても浮遊は効く）。
    net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(HoverDisplay.class);
    FMLJavaModLoadingContext.get().getModEventBus().addListener(ShiftingOrigins::onConfigLoad);
    net.minecraftforge.fml.ModLoadingContext.get()
        .registerConfig(ModConfig.Type.SERVER, Config.SPEC);
    Net.register();
    net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(PacedBreakQueue.class);
    net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(ClassPowers.class);
    net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(PotionSharing.class);
    net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(OriginChangeCancel.class);
    net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(OriginLayerGuard.class);
    net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(SkeletonShy.class);
    // ⚠ 木こりが苗木へ骨粉を使うと1個で木になる。⚠ **板の増量のほうはイベントではなく
    //   `mixin/CraftingResultMixin`**（産物の枠を組む所を通す必要があるため）。
    net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(SaplingBonemeal.class);
    // ⚠ 聖職者のエンチャント。⚠ **鍛冶屋の修理は `mixin/CraftingResultMixin`**（産物の枠を通す）。
    net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(ClericEnchanting.class);
    // ⚠ 戦士の受け流しと背水。⚠ **盾を割られない側は mixin**（`Player.disableShield` を打ち切る）。
    net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(WarriorCombat.class);
    // ⚠ `[種族・職業]` を Tab とサイドバーにだけ出す。名札・チャット・死亡メッセージには出さない。
    //   ⚠ 止め方は config の nameLabels.enabled（datapack 側が数秒で元へ戻る）。理由は NameLabels。
    net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(NameLabels.class);
    // ⚠ 精気吸収がアンデッドから吸えていた（上流の説明は「効かない」と書いているのに実装が無い）
    net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(SiphonGuard.class);
    // ⚠ 種族の蘇りを、不死のトーテムより先に働かせる。⚠ **トーテムは減らない**（理由は当のクラス）。
    net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(UndyingBeforeTotem.class);
    // ⚠ 鍛冶屋の道具と型は減らない（金床が欠けない側）。
    //   ⚠ **鍛冶型は mixin/SmithingMenuMixin**、⚠ **炉の2倍は mixin/BoundTickingBlockEntityMixin**。
    net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(BlacksmithTools.class);
    // ⚠ 商人の在庫（`TradeWithVillagerEvent`）。⚠ mixin ではなく Forge のイベント——
    //   ⚠ 上流 origins-classes が mixin なのは、⚠⚠ **47.1 にこのイベントが無かったから**。
    net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(MerchantStock.class);
    // ⚠ 溶岩入りの瓶（ブレイズボーンの水）は **Mixin 側**で拾う（`mixin/ItemMixin`）。
    //   ⚠ イベントで拾うと**啜るモーションが出ない**ので、ここには登録しない。
    // ⚠ 能力名の横に自動で付く印（A / T / R）を、画面へ出る前に外す。
    //   ⚠ datapack では消せない（消しても内蔵の絵で足される）。理由は AutoBadgeStrip の説明。
    net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(AutoBadgeStrip.class);
  }

  private static void onConfigLoad(final ModConfigEvent event) {
    // 値は都度 get するので、ここでは何もしない（読み込み順の事故を避ける）
  }

  public static final class Config {

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue INTERVAL_TICKS;
    public static final ForgeConfigSpec.IntValue BLOCKS_PER_BATCH;
    public static final ForgeConfigSpec.IntValue MAX_TOTAL_TICKS;
    public static final ForgeConfigSpec.IntValue MAX_DISTANCE;
    public static final ForgeConfigSpec.IntValue ORE_VEIN_MAX;
    public static final ForgeConfigSpec.DoubleValue SHARE_RADIUS_H;
    public static final ForgeConfigSpec.DoubleValue SHARE_RADIUS_V;
    public static final ForgeConfigSpec.ConfigValue<java.util.List<? extends String>> SHARE_CATEGORIES;
    public static final ForgeConfigSpec.BooleanValue GUARD_ENABLED;
    public static final ForgeConfigSpec.IntValue GUARD_GRACE_TICKS;
    public static final ForgeConfigSpec.BooleanValue SKELETON_SHY;
    public static final ForgeConfigSpec.DoubleValue SKELETON_SHY_DISTANCE;
    public static final ForgeConfigSpec.BooleanValue LABELS_ENABLED;
    public static final ForgeConfigSpec.BooleanValue LABELS_TAB;
    public static final ForgeConfigSpec.BooleanValue LABELS_SIDEBAR;
    public static final ForgeConfigSpec.BooleanValue SIPHON_SKIP_UNDEAD;
    public static final ForgeConfigSpec.BooleanValue UNDYING_BEFORE_TOTEM;
    public static final ForgeConfigSpec.BooleanValue LAVA_BOTTLE_DRINK;
    public static final ForgeConfigSpec.IntValue LAVA_BOTTLE_THIRST;
    public static final ForgeConfigSpec.DoubleValue LAVA_BOTTLE_HYDRATION;
    public static final ForgeConfigSpec.BooleanValue HOVER_ENABLED;
    public static final ForgeConfigSpec.IntValue HOVER_AMPLIFIER;
    public static final ForgeConfigSpec.BooleanValue HOVER_SHOW_ICON;
    public static final ForgeConfigSpec.BooleanValue LAVA_SWIM_ENABLED;
    public static final ForgeConfigSpec.IntValue BONUS_PLANKS;
    public static final ForgeConfigSpec.BooleanValue SAPLING_BONEMEAL;
    public static final ForgeConfigSpec.IntValue SAPLING_MAX_STEPS;
    public static final ForgeConfigSpec.BooleanValue CLERIC_ENCHANTING;
    public static final ForgeConfigSpec.BooleanValue BLACKSMITH_REPAIR;
    public static final ForgeConfigSpec.BooleanValue BLACKSMITH_TOOLS;
    public static final ForgeConfigSpec.BooleanValue BLACKSMITH_FURNACE;
    public static final ForgeConfigSpec.BooleanValue SHIELD_MASTER;
    public static final ForgeConfigSpec.IntValue RIPOSTE_TICKS;
    public static final ForgeConfigSpec.DoubleValue RIPOSTE_BONUS;
    public static final ForgeConfigSpec.DoubleValue LAST_STAND_HALF;
    public static final ForgeConfigSpec.DoubleValue LAST_STAND_QUARTER;
    public static final ForgeConfigSpec.DoubleValue QUICK_BITE_FACTOR;
    public static final ForgeConfigSpec.DoubleValue MERCHANT_STOCK_KEPT;
    public static final ForgeConfigSpec.BooleanValue VERBOSE_LOGS;

    static {
      ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
      INTERVAL_TICKS = b
          .comment("How many ticks to wait between batches. 0 restores the vanilla Origins Classes",
              "behaviour of breaking everything in the same tick.",
              "1 tick per block (20 blocks a second) is the club default; maxTotalTicks raises the",
              "batch size on top of that so a large vein still finishes in a few seconds.")
          .defineInRange("intervalTicks", 1, 0, 200);
      BLOCKS_PER_BATCH = b
          .comment("How many blocks to break per batch.")
          .defineInRange("blocksPerBatch", 1, 1, 1000);
      MAX_TOTAL_TICKS = b
          .comment("Upper bound, in ticks, for finishing one vein. If the configured pace would",
              "take longer, the batch size is raised so that a huge vein does not keep the player",
              "waiting. 0 disables the bound.")
          .defineInRange("maxTotalTicks", 100, 0, 12000);
      MAX_DISTANCE = b
          .comment("Blocks further than this from the player are skipped. Stops the vein from",
              "continuing after the player walks away.")
          .defineInRange("maxDistanceFromPlayer", 48, 4, 256);
      ORE_VEIN_MAX = b
          .comment("Upper bound for the ore vein power added by this mod. The tree felling power",
              "from Origins Classes has its own hard-coded limit of 255 and is not affected.")
          .defineInRange("oreVeinMaxBlocks", 160, 1, 4096);
      b.comment("Cleric potion sharing. A cleric's drunk potion also reaches nearby players.",
              "The vanilla anchor is the splash potion, which uses inflate(4.0, 2.0, 4.0) in",
              "ThrownPotion.applySplash. The defaults here are twice that: a cleric reaches",
              "further by drinking than anyone reaches by throwing.")
          .push("potionSharing");
      SHARE_RADIUS_H = b
          .comment("Horizontal reach in blocks. Vanilla splash is 4.0.")
          .defineInRange("radiusHorizontal", 8.0D, 0.0D, 64.0D);
      SHARE_RADIUS_V = b
          .comment("Vertical reach in blocks. Vanilla splash is 2.0.")
          .defineInRange("radiusVertical", 4.0D, 0.0D, 64.0D);
      SHARE_CATEGORIES = b
          .comment("Which effect categories get shared: BENEFICIAL, NEUTRAL, HARMFUL.",
              "HARMFUL is left out by default so that a cleric buffing themselves does not",
              "poison the people standing next to them. Add it if you want that on purpose.")
          .defineList("categories", java.util.List.of("BENEFICIAL", "NEUTRAL"),
              o -> o instanceof String s2
                  && (s2.equals("BENEFICIAL") || s2.equals("NEUTRAL") || s2.equals("HARMFUL")));
      b.pop();
      b.comment("Safety net for players left with an empty origin layer. While a layer is empty",
              "Origins makes the player invulnerable to every damage type, which is meant as",
              "protection while the choose-origin screen is up. /origin gui empties the layer",
              "without ever opening that screen, so the player is stuck invulnerable instead.",
              "This puts the layer's default origin back, using the same call the login handler",
              "uses. A selection started from an orb is left alone.")
          .push("originGuard");
      GUARD_ENABLED = b
          .comment("Whether to put the default origin back.")
          .define("enabled", true);
      GUARD_GRACE_TICKS = b
          .comment("How long a layer has to stay empty before the default origin goes back in.",
              "Do not set this to 0: another mod may empty a layer for a moment before writing",
              "the new origin, and this net would win that race.")
          .defineInRange("graceTicks", 60, 20, 12000);
      b.pop();
      b.comment("Skeletons back away from the club's beastfolk_dog origin, the way vanilla",
              "skeletons back away from wolves. Vanilla hard-codes Wolf.class in",
              "AbstractSkeleton.registerGoals, so a datapack cannot put a player in that slot.",
              "The numbers below are vanilla's own: 6 blocks, walk 1.0, sprint 1.2, priority 3.")
          .push("skeletonShy");
      SKELETON_SHY = b
          .comment("Whether skeletons avoid the dog beastfolk.")
          .define("enabled", true);
      SKELETON_SHY_DISTANCE = b
          .comment("How far away a skeleton starts backing off, in blocks. Vanilla uses 6.0 for",
              "wolves. Raising this makes skeleton-heavy areas much easier for the dog.")
          .defineInRange("distance", 6.0D, 1.0D, 32.0D);
      b.pop();
      b.comment("Show [origin/class] next to player names in the tab list and the scoreboard",
              "sidebar ONLY -- not above the head, not in chat, not in death messages.",
              "The club datapack used to do this with a team prefix, but a team prefix is read by",
              "all four of those places at once, so it could not be limited to two of them.",
              "Turning this off puts the sidebar display slots back to the club.* objectives and",
              "lowers the flag the datapack watches, so the old look returns within a few seconds",
              "without rebuilding anything.")
          .push("nameLabels");
      LABELS_ENABLED = b
          .comment("Whether this mod owns the origin/class labels.")
          .define("enabled", true);
      LABELS_TAB = b
          .comment("Put the label in the tab list (PlayerEvent.TabListNameFormat).")
          .define("tabList", true);
      LABELS_SIDEBAR = b
          .comment("Put the label in the sidebar, by mirroring each club.* objective into a",
              "view.* one whose score holders are named '[origin/class] player'. Offline players",
              "keep their row, which is the whole point of showing this in the sidebar.")
          .define("sidebar", true);
      b.pop();
      b.comment("The revenant's Essence Extraction (medievalorigins:revenant/siphon).",
              "Upstream's own description says it affects everything 'except summoned dead',",
              "but there is no such exclusion anywhere in the implementation: the power JSON",
              "carries no bientity_condition, SummonedSkeleton never mentions siphon, and",
              "SpellDamageAction has no summon check. So a revenant can summon a zombie and",
              "heal off it forever. This cancels the siphon damage against ALL undead, which",
              "also stops the heal (upstream only heals after the damage lands).")
          .push("siphon");
      SIPHON_SKIP_UNDEAD = b
          .comment("Whether siphon damage against undead mobs is cancelled.")
          .define("skipUndead", true);
      b.pop();
      b.comment("Blazeborn drink Alex's Mobs lava bottles.",
              "Blazeborn take 2 damage from canteen water (world3:damage_from_canteens) but",
              "still get thirsty -- heat_tolerance only covers body temperature. A lava bottle",
              "is made by right-clicking a lava source with a glass bottle, which sets the",
              "player on fire; blazeborn are immune to that, so it is free for them alone.",
              "The tag route does not work: Tough As Nails' thirst tags only say how much a",
              "drink restores, and alexsmobs:lava_bottle is a plain Item with no use action.",
              "The defaults match toughasnails:purified_water_bottle, read from its tags.")
          .push("lavaBottle");
      LAVA_BOTTLE_DRINK = b
          .comment("Whether blazeborn can drink alexsmobs:lava_bottle.")
          .define("enabled", true);
      LAVA_BOTTLE_THIRST = b
          .comment("Thirst restored. Purified water bottle is 5.")
          .defineInRange("thirst", 5, 0, 20);
      LAVA_BOTTLE_HYDRATION = b
          .comment("Hydration restored. Purified water bottle is 80.")
          .defineInRange("hydration", 80.0D, 0.0D, 100.0D);
      b.pop();

      // ⚠⚠ **調査用の出力を既定で黙らせる**（2026-08-29・本番で分かった）。
      //    2026-08-28 の開始日、⚠ **サーバのログの 90%（3時間で 11,900 行）が
      //    この MOD の出力**だった（右クリックごと・拾得ごとに1行）。
      //    ⚠ 遅れの原因ではなかったが、**部員の座標と行動が全部残り**、
      //    ⚠ **本当に見たい行が埋まる**（実際、原因を調べるとき掻き分ける羽目になった）。
      //    ⚠ 消さずに残すのは、どれも切り分けに要った出力だから。
      // ⚠⚠ **浮遊を状態効果から外した**（2026-09-05）。理由は Hover の説明を読む——
      //    ⚠ **農夫の加護が HARMFUL の効果を毎tick 消しており、浮遊はその分類だった。**
      b.comment("Hover (the H toggle). The rise itself uses vanilla's levitation maths;",
              "only the status effect is gone, because other mods strip harmful effects.")
          .push("hover");
      HOVER_ENABLED = b
          .comment("Turn the hover power off without removing it from the origin.")
          .define("enabled", true);
      HOVER_AMPLIFIER = b
          .comment("Levitation amplifier to imitate. 0 is what the datapack used before,",
              "and each step adds 0.05 blocks per tick to the rise.")
          .defineInRange("amplifier", 0, 0, 10);
      HOVER_SHOW_ICON = b
          .comment("Show an icon in the status effect bar while hovering.",
              "Display only - turning this off does not stop the rise.")
          .define("showIcon", true);
      b.pop();

      // ⚠ 溶岩を水と同じ物理で泳ぐ。⚠ **持っている人にだけ効く**（power で見る）。
      b.comment("Swimming in lava. The rise, drag and buoyancy all come from vanilla's",
              "water branch - nothing here invents its own feel.")
          .push("lavaSwim");
      LAVA_SWIM_ENABLED = b
          .comment("Turn the lava swimming power off without removing it from the origin.")
          .define("enabled", true);
      b.pop();

      // ⚠ 板の増量は、上流の power が作業台で1枚も増やせていなかったため当部で作り直した。
      //   ⚠ 理由の全文は LumberjackPlanks の説明（Visual Workbench が容器を差し替えている）。
      b.comment("The lumberjack class. Upstream's more_planks_from_logs is an",
              "apoli:modify_crafting power, and its ModifiedCraftingRecipe bails out unless the",
              "grid is a vanilla TransientCraftingContainer. Visual Workbench swaps the crafting",
              "table's grid for its own ForwardingCraftingContainer, so the bonus never applied",
              "at a table -- which is what club members reported. This mod bumps the assembled",
              "result instead, so no extra recipe competes with the vanilla one.")
          .push("lumberjack");
      BONUS_PLANKS = b
          .comment("Extra planks when a lumberjack crafts planks from a single log.",
              "Vanilla gives 4; upstream's description promises two more, so 2 is the default.",
              "0 turns the bonus off without removing the power from the class.")
          .defineInRange("bonusPlanks", 2, 0, 60);
      SAPLING_BONEMEAL = b
          .comment("Bone meal used by a lumberjack on a sapling grows the tree in one go.",
              "Vanilla rolls 0.45 per bone meal and needs two successes, so an average sapling",
              "costs about 4.4 bone meal. This skips the roll for this class only.",
              "Players who also carry a modify_bone_meal power are left to Origins Classes.")
          .define("saplingBonemeal", true);
      SAPLING_MAX_STEPS = b
          .comment("Upper bound on how many growth steps one bone meal may take. Vanilla",
              "saplings need 2. The bound only exists so a modded sapling with an unusual",
              "performBonemeal cannot spin here forever.")
          .defineInRange("saplingMaxSteps", 8, 1, 64);
      b.pop();

      // ⚠ どちらも「上流の実装が前提にしている vanilla の器を、別の MOD が差し替えた」型。
      //   ⚠ 木こりと同じ形で、2026-09-05 の総当たり（selection/audits/class-powers-alive）で出た。
      b.comment("The cleric's better_enchanting. Upstream relays the enchanter through an NBT",
              "tag written in EnchantmentMenu.slotsChanged, because EnchantmentLevelSetEvent",
              "does not carry a player. Easy Magic's ModEnchantmentMenu declares slotsChanged",
              "itself and never calls the vanilla one, so the tag is never written and the",
              "bonus never applies. This looks the enchanter up from the table position",
              "instead, which does not depend on which mod owns the menu.")
          .push("cleric");
      CLERIC_ENCHANTING = b
          .comment("Whether the cleric's enchanting bonus is restored.",
              "Turns itself off for a stack that already carries upstream's tag, so removing",
              "Easy Magic (or adding Apotheosis) needs no config change.")
          .define("enchantingBonus", true);
      b.pop();
      b.comment("The blacksmith's efficient_repairs, crafting-grid half. Upstream's",
              "RepairItemRecipeMixin returns the vanilla 5% unless the grid is a",
              "TransientCraftingContainer, which Visual Workbench replaces at a crafting",
              "table -- the same break as the lumberjack's planks. The 2x2 inventory grid",
              "kept working, so repairs came out better in the inventory than on a table.")
          .push("blacksmith");
      BLACKSMITH_REPAIR = b
          .comment("Whether the combine-repair durability bonus is restored at a table.",
              "Skipped when the grid is a vanilla container, because upstream applies it there.")
          .define("combineRepair", true);
      BLACKSMITH_TOOLS = b
          .comment("A blacksmith's own gear is not spent: the smithing template survives a",
              "smithing-table use, and the anvil never chips. The anvil's level cost is left",
              "alone on purpose. That cost scales with enchantment count and rarity, so it is",
              "the cleric's axis, not the smith's.")
          .define("toolsNotSpent", true);
      BLACKSMITH_FURNACE = b
          .comment("Furnaces and blast furnaces near a blacksmith run at double speed.",
              "Smokers are excluded: those belong to the cook, whose own power leaves",
              "furnaces alone. The two classes divide the fires between them.")
          .define("furnaceSpeed", true);
      b.pop();

      // ⚠ 2026-09-06 に足した3つ。⚠ **手数（攻撃速度）はここに無い**——
      //   あちらは素の `apoli:attribute` なので、数字は power の JSON 側に在る。
      b.comment("Three powers added to the warrior in 2026-09-06. Apoli has no entity",
              "condition for health and none for 'just blocked with a shield', so these are",
              "marker powers whose behaviour lives in Java. The vanilla axe hit adds 0.75 on",
              "top of a 0.25 base chance to knock a shield aside, i.e. it practically always",
              "lands; shieldMaster stops that for this class only.")
          .push("warrior");
      SHIELD_MASTER = b
          .comment("Whether a warrior's shield can be knocked aside by an axe.",
              "The shield still takes durability damage -- only the stagger is stopped.")
          .define("shieldMaster", true);
      RIPOSTE_TICKS = b
          .comment("How long the riposte stays armed after blocking, in ticks. 20 = 1 second.")
          .defineInRange("riposteTicks", 60, 1, 600);
      RIPOSTE_BONUS = b
          .comment("Extra damage on the first hit after blocking. 0.5 = +50%.",
              "Spent on one hit, so holding the bonus while swinging repeatedly is not possible.")
          .defineInRange("riposteBonus", 0.5D, 0.0D, 10.0D);
      LAST_STAND_HALF = b
          .comment("Extra damage below half health. Measured as a share of max health, so",
              "races with fewer hearts reach it at the same point.")
          .defineInRange("lastStandHalf", 0.15D, 0.0D, 10.0D);
      LAST_STAND_QUARTER = b
          .comment("Extra damage below a quarter health. Replaces the half-health value.")
          .defineInRange("lastStandQuarter", 0.3D, 0.0D, 10.0D);
      b.pop();

      b.comment("The fisher. Vanilla rolls 100-600 ticks of waiting before a fish bites and",
              "then subtracts 100 ticks per level of Lure. Doubling the catch was pointless",
              "while that wait stayed the same, so both of the class's powers were stuck",
              "behind it. This scales the rolled wait instead of subtracting from it, so a",
              "short roll cannot collapse to zero.")
          .push("fisher");
      QUICK_BITE_FACTOR = b
          .comment("Multiplier on the rolled wait. 0.6 = 40% shorter. Lure still applies on top.")
          .defineInRange("quickBiteFactor", 0.6D, 0.05D, 1.0D);
      b.pop();

      b.comment("The merchant's trade_availability. Upstream makes a trade never run out at",
              "all: its mixin gives the use straight back at the end of notifyTrade. A stock",
              "that never moves also makes the villager's own restock meaningless, so the",
              "class removed a whole vanilla rhythm rather than bending it. This rolls for",
              "each trade instead, and tells the open screen when the use was given back so",
              "the client's copy of the offers cannot drift from the server's.")
          .push("merchant");
      MERCHANT_STOCK_KEPT = b
          .comment("Chance that a trade does not spend a use for a merchant. 0.5 = half the",
              "trades are free, so a 12-use trade lasts about 24 trades. 1.0 restores",
              "upstream's never-runs-out behaviour; 0 turns the power off without removing it",
              "from the class.")
          .defineInRange("stockKeptChance", 0.5D, 0.0D, 1.0D);
      b.pop();

      b.comment("Which revival wins when a player holding a Totem of Undying takes a lethal",
              "hit. Vanilla checks the totem inside hurt and only calls die when no totem",
              "saved you, so an Origins prevent_death power never gets a turn. The totem is",
              "gone for good after one use, while the class revival can be refilled, so the",
              "refillable one should be spent first. Turning this off restores vanilla order.")
          .push("undying");
      UNDYING_BEFORE_TOTEM = b
          .comment("Let an Origins revival fire before the totem. The totem is NOT consumed.")
          .define("beforeTotem", true);
      b.pop();

      VERBOSE_LOGS = b
          .comment("Log every cook-station touch, item pickup and paced-break batch.",
              "Off by default: on a live server this is thousands of lines an hour and",
              "buries the lines you actually want. Turn on only while investigating.")
          .define("verboseLogs", false);

      SPEC = b.build();
    }

    /** ⚠ 調査用の出力を出すか。⚠ **既定は false**（上の理由）。 */
    public static boolean verbose() {
      return VERBOSE_LOGS != null && VERBOSE_LOGS.get();
    }

    private Config() {
    }
  }
}
