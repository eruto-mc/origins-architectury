package io.github.apace100.origins.screen;

import io.github.apace100.origins.Origins;
import io.github.apace100.origins.origin.Impact;
import io.github.apace100.origins.registry.ModItems;
import io.github.edwinmindcraft.origins.api.OriginsAPI;
import io.github.edwinmindcraft.origins.api.data.PartialOrigin;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import io.github.edwinmindcraft.origins.common.OriginsCommon;
import io.github.edwinmindcraft.origins.common.network.C2SChooseOrigin;
import io.github.edwinmindcraft.origins.common.network.C2SChooseRandomOrigin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ChooseOriginScreen extends OriginDisplayScreen {

	private static final Comparator<Holder<Origin>> COMPARATOR = Comparator.comparingInt((Holder<Origin> a) -> a.value().getImpact().getImpactValue()).thenComparingInt((Holder<Origin> a) -> a.value().getOrder());

	private final List<Holder<OriginLayer>> layerList;
	private final int currentLayerIndex;
	private int currentOrigin = 0;
	private final List<Holder<Origin>> originSelection;
	private int maxSelection;

	private Origin randomOrigin;

	public ChooseOriginScreen(List<Holder<OriginLayer>> layerList, int currentLayerIndex, boolean showDirtBackground) {
		super(Component.translatable(Origins.MODID + ".screen.choose_origin"), showDirtBackground);
		this.layerList = layerList;
		this.currentLayerIndex = currentLayerIndex;
		this.originSelection = new ArrayList<>(10);
		Player player = Minecraft.getInstance().player;
		Holder<OriginLayer> currentLayer = layerList.get(currentLayerIndex);
		// ⚠⚠ **当部の直し（2026-09-01）: 層から取った種族を、id でレジストリから引き直す。**
		//
		//    ⚠ **なぜ要るか（実機で測った）**: 層が抱えている `Holder<Origin>` は
		//    ⚠ **層が復号された時点のレジストリの物**で、⚠⚠ **その後レジストリが作り直されると
		//    永久に古い世代を指したまま**になる（`S2CDynamicRegistryPacket.handle` が
		//    `start == 0` のとき `instance.reset(key)` でレジストリのオブジェクトごと作り直す）。
		//
		//    ⚠ 2026-09-01 の走行で、⚠⚠ **全種族が `inRegistry=false`・能力が `unbound == raw`**
		//    （＝1本残らず未結合）だった。⚠ 画面には**説明文までしか出ず、能力が1つも並ばない。**
		//
		//    ⚠ **Oキーの画面（`ViewOriginScreen:42`）は最初から引き直している。**
		//    ⚠ こちらだけが引き直していなかった——⚠⚠ **上流の中の食い違い**なので、揃える。
		//
		//    ⚠ 能力（`Origin` の中の `HolderSet`）も、引き直した種族の物になるので一緒に直る
		//    （⚠ 能力が古かったのは、⚠ **古い種族が古い能力を抱えていた**ため）。
		//
		//    ⚠ 引き直せないとき（id が取れない／レジストリに無い）は**元の物をそのまま使う**——
		//    ⚠⚠ **落とさない。** 出ないより、古くても出るほうがまし。
		net.minecraft.core.Registry<Origin> originsRegistry = OriginsAPI.getOriginsRegistry();
		currentLayer.value().origins(Objects.requireNonNull(player)).forEach(rawOrigin -> {
			Holder<Origin> origin = rawOrigin;
			ResourceKey<Origin> id = rawOrigin.unwrap()
					.map(Optional::of, originsRegistry::getResourceKey).orElse(null);
			if (id != null) {
				Optional<Holder.Reference<Origin>> fresh = originsRegistry.getHolder(id);
				if (fresh.isPresent() && fresh.get().isBound()) {
					if (fresh.get().value() != rawOrigin.value()) {
						Origins.LOGGER.warn(
								"[eruto] re-resolved stale origin {} from the layer", id.location());
					}
					origin = fresh.get();
				}
			}
			if (origin.isBound() && origin.value().isChoosable()) {
				ItemStack displayItem = origin.value().getIcon();
				if (displayItem.getItem() == Items.PLAYER_HEAD) {
					if (!displayItem.hasTag() || !Objects.requireNonNull(displayItem.getTag()).contains("SkullOwner")) {
						displayItem.getOrCreateTag().putString("SkullOwner", player.getDisplayName().getString());
					}
				}
				this.originSelection.add(origin);
			}
		});
		this.originSelection.sort(COMPARATOR);
		this.maxSelection = this.originSelection.size();
		if (currentLayer.value().allowRandom() && currentLayer.value().randomOrigins(player).size() > 0) {
			this.maxSelection += 1;
		}
		if (this.maxSelection == 0) {
			this.openNextLayerScreen();
		}
		Holder<Origin> newOrigin = this.getCurrentOriginInternal();
		this.showOrigin(newOrigin, layerList.get(currentLayerIndex), newOrigin.value() == this.randomOrigin);
	}

	private void openNextLayerScreen() {
		// eruto patch: 「選ぶ」を押した＝もうやめる話ではない。
		// ⚠ 残したままだと、次に `/origin gui` 等で開いた画面まで閉じられてしまう。
		OriginSelectionCancel.clear();
		Minecraft.getInstance().setScreen(new WaitForNextLayerScreen(this.layerList, this.currentLayerIndex, this.showDirtBackground));
	}

	/**
	 * eruto patch: <b>やめてよい場面だけ</b> Esc を開ける。
	 *
	 * <p>⚠ 上流は {@code false} 固定で、閉じる手段がまったく無い。理由はある——
	 * 層を空にしたまま閉じると {@code hasAllOrigins()} が偽のまま残り、
	 * {@code SelectionInvulnerabilityMixin} が全ダメージを無効にする＝<b>無敵で詰む</b>。
	 *
	 * <p>⚠⚠ だから<b>既定は上流のまま（閉じられない）</b>で、
	 * サーバーが「珠を使った直後」だけ印を立てる（{@link OriginSelectionCancel}）。
	 */
	@Override
	public boolean shouldCloseOnEsc() {
		return OriginSelectionCancel.isCancelable();
	}

	/**
	 * eruto patch: 閉じるときは必ずサーバーへ「やめる」を送る。
	 *
	 * <p>⚠ 閉じ方は2つとも ここに集まる:
	 * <pre>
	 *   Esc  → Screen.keyPressed → shouldCloseOnEsc()==true → onClose()
	 *   ×    → ボタンの処理 ────────────────────────────────→ onClose()
	 * </pre>
	 *
	 * <p>⚠ 「選ぶ」を押した経路はここを通らない（{@code openNextLayerScreen()} が
	 * 画面を差し替えるので {@code onClose()} は呼ばれない）。⚠ 印はあちらで下ろしている。
	 */
	@Override
	public void onClose() {
		if (OriginSelectionCancel.isCancelable()) {
			OriginSelectionCancel.requestCancel();
		}
		this.minecraft.setScreen(null);
	}

	@Override
	protected void init() {
		super.init();
		this.guiLeft = (this.width - windowWidth) / 2;
		this.guiTop = (this.height - windowHeight) / 2;
		if (this.maxSelection > 1) {
			this.addRenderableWidget(Button.builder(Component.literal("<"),  b -> {
                this.currentOrigin = (this.currentOrigin - 1 + this.maxSelection) % this.maxSelection;
                Holder<Origin> newOrigin = this.getCurrentOriginInternal();
                this.showOrigin(newOrigin, this.layerList.get(this.currentLayerIndex), newOrigin.value() == this.randomOrigin);
            }).bounds(this.guiLeft - 40, this.height / 2 - 10, 20, 20).build());
			this.addRenderableWidget(Button.builder(Component.literal(">"), b -> {
				this.currentOrigin = (this.currentOrigin + 1) % this.maxSelection;
				Holder<Origin> newOrigin = this.getCurrentOriginInternal();
				this.showOrigin(newOrigin, this.layerList.get(this.currentLayerIndex), newOrigin.value() == this.randomOrigin);
			}).bounds(this.guiLeft + windowWidth + 20, this.height / 2 - 10, 20, 20).build());
		}
		this.addRenderableWidget(Button.builder(Component.translatable(Origins.MODID + ".gui.select"), b -> {
			ResourceLocation layer = this.layerList.get(this.currentLayerIndex).unwrap().map(Optional::of, OriginsAPI.getLayersRegistry(null)::getResourceKey).map(ResourceKey::location).orElseThrow();
			if (this.currentOrigin == this.originSelection.size())
				OriginsCommon.CHANNEL.send(PacketDistributor.SERVER.noArg(), new C2SChooseRandomOrigin(layer));
			else {
				Optional<ResourceKey<Origin>> key = this.getCurrentOrigin().unwrap().map(Optional::of, OriginsAPI.getOriginsRegistry(null)::getResourceKey);
				if (key.isPresent())
					OriginsCommon.CHANNEL.send(PacketDistributor.SERVER.noArg(), new C2SChooseOrigin(layer, key.get().location()));
				else
					Origins.LOGGER.error("Unregistered origin found for layer {}: {}", layer, this.getCurrentOrigin());
			}
			// The below is necessary for opening the Waiting For Powers Screen.
			this.openNextLayerScreen();
		}).bounds(this.guiLeft + windowWidth / 2 - 50, this.guiTop + windowHeight + 5, 100, 20).build());
		// eruto patch: やめられる場面だけ、× ボタンを窓の右上に置く。
		// ⚠ 位置は上の「＞」と同じ列（`guiLeft + windowWidth + 20`）の一番上。
		if (OriginSelectionCancel.isCancelable()) {
			this.addRenderableWidget(Button.builder(Component.literal("×"), b -> this.onClose())
					.bounds(this.guiLeft + windowWidth + 20, this.guiTop, 20, 20).build());
		}
	}

	@Override
	protected Component getTitleText() {
		Component titleText = this.getCurrentLayer().get().title().choose();
		if (titleText != null)
			return titleText;
		return Component.translatable(Origins.MODID + ".gui.choose_origin.title", this.getCurrentLayer().get().name());
	}

	private Holder<Origin> getCurrentOriginInternal() {
		if (this.currentOrigin == this.originSelection.size()) {
			if (this.randomOrigin == null) {
				this.initRandomOrigin();
			}
			return Holder.direct(this.randomOrigin);
		}
		return this.originSelection.get(this.currentOrigin);
	}

	private void initRandomOrigin() {
		this.randomOrigin = PartialOrigin.builder().icon(new ItemStack(ModItems.ORB_OF_ORIGIN.get())).impact(Impact.NONE).order(Integer.MAX_VALUE).loadingOrder(Integer.MAX_VALUE).build().create(Origins.identifier("random"));
		MutableComponent text = Component.literal("");
		List<Holder<Origin>> randoms = this.layerList.get(this.currentLayerIndex).value()
				.randomOrigins(Objects.requireNonNull(Minecraft.getInstance().player)).stream()
				.filter(Objects::nonNull).sorted(COMPARATOR).toList();
		randoms.forEach(x -> text.append(x.value().getName()).append("\n"));
		this.setRandomOriginText(text);
	}

	@Override
	public void renderBackground(@NotNull GuiGraphics graphics) {
		if (this.showDirtBackground) {
			super.renderDirtBackground(graphics);
		} else {
			super.renderBackground(graphics);
		}
	}

	@Override
	public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		if (this.maxSelection == 0) {
			this.openNextLayerScreen();
			return;
		}
		super.render(graphics, mouseX, mouseY, delta);
	}
}
