package io.github.apace100.origins.content;

import io.github.edwinmindcraft.origins.api.OriginsAPI;
import io.github.edwinmindcraft.origins.api.capabilities.IOriginContainer;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import io.github.edwinmindcraft.origins.common.OriginsCommon;
import io.github.edwinmindcraft.origins.common.network.S2COpenOriginScreen;
import io.github.edwinmindcraft.origins.common.registry.OriginRegisters;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class OrbOfOriginItem extends Item {

	/**
	 * eruto patch: 珠を重ねられる上限（2026-09-01 に mixin から畳んだ）。
	 *
	 * <p>⚠ 上流はコンストラクタで {@code stacksTo(1)} と<b>焼き込んで</b>いる。当部は珠を
	 * 実績の褒美として配るので、1個ずつ枠を潰されると持ち物が埋まる。
	 *
	 * <p>⚠ <b>コンストラクタの値は変えない。</b> バニラの {@code Item#getMaxStackSize()} は
	 * {@code final} で、実体の {@code f_41370_} も {@code private final}。
	 * ⚠⚠ かわりに <b>Forge が足した {@code getMaxStackSize(ItemStack)} を上書きする</b>——
	 * パッチ後の {@code ItemStack#getMaxStackSize()} はそちらを呼ぶ
	 * （{@code forge-1.20.1-47.4.22-server.jar} の {@code ItemStack.m_41741_} が
	 * {@code invokevirtual Item.getMaxStackSize:(Lnet/minecraft/world/item/ItemStack;)I}）。
	 *
	 * <p>⚠ <b>使ったときに溶けない</b>ことを確かめてある——下の {@code use} は
	 * {@code shrink(1)} なので、重なっていても1個ずつ減る。
	 *
	 * <p>⚠⚠ <b>NBT が違う珠は互いに重ならない</b>（バニラの決まり）。当部の珠は
	 * {@code Targets} に層を持つので、<b>同じ層を指す珠同士だけが重なる</b>——
	 * 「種族の珠」×64 と「職業の珠」×64 は別の山になる。これは仕様で、直せない。
	 */
	private static final int ERUTO_STACK_LIMIT = 64;

	/**
	 * eruto patch: 珠を使う前後で呼ばれる口（2026-09-01 に mixin から畳んだ）。
	 *
	 * <p>⚠⚠ <b>なぜ口にするか</b>: 中身（やめられるようにする控え）は
	 * {@code shiftingorigins} が持っており、⚠ ここから名前で呼ぶと
	 * ⚠⚠ <b>循環参照になって組み立てられない</b>。⚠ 向きを逆にして、
	 * <b>あちらが起動時に登録する</b>形にしてある。
	 *
	 * <p>⚠ 登録されていなければ<b>何もしない</b>——その MOD を外した構成でも壊れない。
	 */
	public interface UseListener {
		/** 使う直前（⚠ 層が空にされる前に控えるため）。 */
		void beforeUse(ServerPlayer player, ItemStack orb, boolean consumed);

		/** 使った直後（⚠ やめられることをクライアントへ伝えるため）。 */
		void afterUse(ServerPlayer player);
	}

	private static UseListener useListener = null;

	/** eruto patch: 珠を使う前後の中身を登録する（{@code shiftingorigins} が起動時に呼ぶ）。 */
	public static void setUseListener(UseListener value) {
		useListener = value;
	}

	public OrbOfOriginItem() {
		super(new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
	}

	/** eruto patch: 上の {@link #ERUTO_STACK_LIMIT} を参照。 */
	@Override
	public int getMaxStackSize(@NotNull ItemStack stack) {
		return ERUTO_STACK_LIMIT;
	}

	@Override
	@NotNull
	public InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		// eruto patch: 層が空にされる**前に**控える（やめられるようにするため）。
		if (!level.isClientSide() && useListener != null && player instanceof ServerPlayer sp0) {
			useListener.beforeUse(sp0, sp0.getItemInHand(hand), !sp0.getAbilities().instabuild);
		}
		if (!level.isClientSide()) {
			IOriginContainer.get(player).ifPresent(container -> {
				Map<OriginLayer, Origin> targets = this.getTargets(stack);
				if (targets.size() > 0) {
					for (Map.Entry<OriginLayer, Origin> target : targets.entrySet()) {
						container.setOrigin(target.getKey(), target.getValue());
					}
				} else {
					for (Holder.Reference<OriginLayer> layer : OriginsAPI.getActiveLayers()) {
						container.setOrigin(layer.key(), Objects.requireNonNull(OriginRegisters.EMPTY.getKey()));
					}
				}
				if (player instanceof ServerPlayer sp) {
					container.checkAutoChoosingLayers(false);
					PacketDistributor.PacketTarget target = PacketDistributor.PLAYER.with(() -> sp);
					OriginsCommon.CHANNEL.send(target, container.getSynchronizationPacket());
					OriginsCommon.CHANNEL.send(target, new S2COpenOriginScreen(false));
					container.synchronize();
				}
			});
		}
		if (!player.isCreative()) {
			stack.shrink(1);
		}
		// eruto patch: 使い終えた合図（やめられることをクライアントへ伝える）。
		if (!level.isClientSide() && useListener != null && player instanceof ServerPlayer sp1) {
			useListener.afterUse(sp1);
		}
		return InteractionResultHolder.consume(stack);
	}

	/**
	 * eruto patch: 説明欄の「」を直す（2026-09-01 に mixin から畳んだ）。
	 *
	 * <p>⚠ 上流は珠が指す種族が {@code Origin.EMPTY} と<b>同一オブジェクトなら</b>
	 * 「新しい〜を選び直せる」、違えば「〜を『(名前)』にする」と出す。
	 * ⚠⚠ 当部の珠は NBT に {@code Origin:"origins:empty"} を持ち、これは
	 * <b>レジストリから引いた別のオブジェクト</b>なので後者に落ちる。
	 * その名前は {@code Component.literal("")} なので、
	 * ⚠ <b>「種族を『』にする。」</b>と表示されていた。
	 *
	 * <p>⚠ <b>NBT から {@code Origin} を外せば直る、ではない。</b>
	 * {@code getTargets} は {@code Origin} を持たない項目を<b>捨てる</b>ので、
	 * 対象0件＝「全部の層を選び直す」に化ける。
	 * ⚠ <b>名前が空のときに「選び直せる」側の文言を出す</b>、が正しい直し方。
	 */
	@Override
	public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> components, @NotNull TooltipFlag flags) {
		Map<OriginLayer, Origin> targets = this.getTargets(stack);
		for (Map.Entry<OriginLayer, Origin> entry : targets.entrySet()) {
			if (isBlankOrigin(entry.getValue()))
				components.add(Component.translatable("item.origins.orb_of_origin.layer_generic", entry.getKey().name()).withStyle(ChatFormatting.GRAY));
			else
				components.add(Component.translatable("item.origins.orb_of_origin.layer_specific", entry.getKey().name(), entry.getValue().getName()).withStyle(ChatFormatting.GRAY));
		}
	}

	/** eruto patch: 名前を持たない種族＝「未選択に戻す」ための指定。上流の {@code Origin.EMPTY} も含む。 */
	private static boolean isBlankOrigin(Origin origin) {
		return origin == Origin.EMPTY || origin.getName().getString().isEmpty();
	}

	private Map<OriginLayer, Origin> getTargets(ItemStack stack) {
		Map<OriginLayer, Origin> targets = new HashMap<>();
		if (!stack.hasTag()) {
			return targets;
		}
		CompoundTag nbt = Objects.requireNonNull(stack.getTag());
		ListTag targetList = nbt.getList("Targets", Tag.TAG_COMPOUND);
		for (Tag nbtElement : targetList) {
			CompoundTag targetNbt = (CompoundTag) nbtElement;
			if (targetNbt.contains("Layer", Tag.TAG_STRING)) {
				try {
					ResourceLocation id = new ResourceLocation(targetNbt.getString("Layer"));
					OriginLayer layer = OriginsAPI.getLayersRegistry().get(id);
					if (layer == null) continue;
					Origin origin = Origin.EMPTY;
					ResourceLocation originId = null;
					if (targetNbt.contains("Origin", Tag.TAG_STRING)) {
						originId = new ResourceLocation(targetNbt.getString("Origin"));
						origin = OriginsAPI.getOriginsRegistry().get(originId);
					}
					if (origin == null || originId == null)
						continue;
					if (layer.enabled() && (layer.contains(originId) || origin.isSpecial())) {
						targets.put(layer, origin);
					}
				} catch (Exception e) {
					// no op
				}
			}
		}
		return targets;
	}
}
