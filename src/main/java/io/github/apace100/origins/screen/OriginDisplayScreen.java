package io.github.apace100.origins.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.apace100.origins.Origins;
import io.github.apace100.origins.badge.Badge;
import io.github.apace100.origins.badge.BadgeManager;
import io.github.apace100.origins.mixin.DrawContextAccessor;
import io.github.apace100.origins.origin.Impact;
import io.github.edwinmindcraft.apoli.api.ApoliAPI;
import io.github.edwinmindcraft.apoli.api.power.configuration.ConfiguredPower;
import io.github.edwinmindcraft.origins.api.OriginsAPI;
import io.github.edwinmindcraft.origins.api.origin.Origin;
import io.github.edwinmindcraft.origins.api.origin.OriginLayer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class OriginDisplayScreen extends Screen {

	private static final ResourceLocation WINDOW = new ResourceLocation(Origins.MODID, "textures/gui/choose_origin.png");

	//Yes, this is improper usage of that class, but that's what isBound is for.
	@SuppressWarnings("ConstantConditions")
	private static Holder<Origin> unboundOrigin() {
		return Holder.Reference.createStandAlone(OriginsAPI.getOriginsRegistry(null).holderOwner(), null);
	}

	@SuppressWarnings("ConstantConditions")
	private static Holder<OriginLayer> unboundLayer() {
		return Holder.Reference.createStandAlone(OriginsAPI.getLayersRegistry(null).holderOwner(), null);
	}

	@NotNull
	private Holder<Origin> origin = unboundOrigin();
	@NotNull
	private Holder<OriginLayer> layer = unboundLayer();
	private boolean isOriginRandom;
	private Component randomOriginText;

	protected static final int windowWidth = 176;
	protected static final int windowHeight = 182;
	protected int scrollPos = 0;
	private int currentMaxScroll = 0;
	protected float time = 0;

	protected int guiTop, guiLeft;

	protected final boolean showDirtBackground;

	private final LinkedList<RenderedBadge> renderedBadges = new LinkedList<>();

	public OriginDisplayScreen(Component title, boolean showDirtBackground) {
		super(title);
		this.showDirtBackground = showDirtBackground;
		this.showNone();
	}

	public void showNone() {
		this.showOrigin(unboundOrigin(), unboundLayer(), false);
	}

	public void showOrigin(Holder<Origin> origin, Holder<OriginLayer> layer, boolean isRandom) {
		this.origin = origin;
		this.layer = layer;
		this.isOriginRandom = isRandom;
		this.scrollPos = 0;
		this.time = 0;
	}

	public void setRandomOriginText(Component text) {
		this.randomOriginText = text;
	}

	@Override
	protected void init() {
		super.init();
		this.guiLeft = (this.width - windowWidth) / 2;
		this.guiTop = (this.height - windowHeight) / 2;
	}

	@NotNull
	public Holder<Origin> getCurrentOrigin() {
		return this.origin;
	}

	@NotNull
	public Holder<OriginLayer> getCurrentLayer() {
		return this.layer;
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
		this.renderedBadges.clear();
		this.time += delta;
		this.renderBackground(graphics);
		this.renderOriginWindow(graphics, mouseX, mouseY);
		super.render(graphics, mouseX, mouseY, delta);
		if (this.origin.isBound()) {
			this.renderScrollbar(graphics, mouseX, mouseY);
			this.renderBadgeTooltip(graphics, mouseX, mouseY);
		}
	}

	private void renderScrollbar(GuiGraphics graphics, int mouseX, int mouseY) {
		if (!this.canScroll()) {
			return;
		}
        graphics.blit(WINDOW, this.guiLeft + 155, this.guiTop + 35, 188, 24, 8, 134);
		int scrollbarY = 36;
		int maxScrollbarOffset = 141;
		int u = 176;
		float part = this.scrollPos / (float) this.currentMaxScroll;
		scrollbarY += (maxScrollbarOffset - scrollbarY) * part;
		if (this.scrolling) {
			u += 6;
		} else if (mouseX >= this.guiLeft + 156 && mouseX < this.guiLeft + 156 + 6) {
			if (mouseY >= this.guiTop + scrollbarY && mouseY < this.guiTop + scrollbarY + 27) {
				u += 6;
			}
		}
        graphics.blit(WINDOW, this.guiLeft + 156, this.guiTop + scrollbarY, u, 24, 6, 27);
	}

	private boolean scrolling = false;
	private int scrollDragStart = 0;
	private double mouseDragStart = 0;

	private boolean canScroll() {
		return this.origin.isBound() && this.currentMaxScroll > 0;
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		this.scrolling = false;
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (this.canScroll()) {
			this.scrolling = false;
			int scrollbarY = 36;
			int maxScrollbarOffset = 141;
			float part = this.scrollPos / (float) this.currentMaxScroll;
			scrollbarY += (maxScrollbarOffset - scrollbarY) * part;
			if (mouseX >= this.guiLeft + 156 && mouseX < this.guiLeft + 156 + 6) {
				if (mouseY >= this.guiTop + scrollbarY && mouseY < this.guiTop + scrollbarY + 27) {
					this.scrolling = true;
					this.scrollDragStart = scrollbarY;
					this.mouseDragStart = mouseY;
					return true;
				}
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (this.scrolling) {
			int delta = (int) (mouseY - this.mouseDragStart);
			int newScrollPos = Math.max(36, Math.min(141, this.scrollDragStart + delta));
			float part = (newScrollPos - 36) / (float) (141 - 36);
			this.scrollPos = (int) (part * this.currentMaxScroll);
		}
		return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
	}

	private void renderBadgeTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
		for (RenderedBadge rb : this.renderedBadges) {
			if (mouseX >= rb.x &&
				mouseX < rb.x + 9 &&
				mouseY >= rb.y &&
				mouseY < rb.y + 9 &&
				rb.hasTooltip()) {
				int widthLimit = this.width - mouseX - 24;
				((DrawContextAccessor) graphics).invokeDrawTooltip(this.font, rb.getTooltipComponents(this.font, widthLimit), mouseX, mouseY, DefaultTooltipPositioner.INSTANCE);
			}
		}
	}

	protected Component getTitleText() {
		return Component.literal("Origins");
	}

	private void renderOriginWindow(GuiGraphics graphics, int mouseX, int mouseY) {
		RenderSystem.enableBlend();
		this.renderWindowBackground(graphics, 16, 0);
		if (this.origin.isBound()) {
			this.renderOriginContent(graphics, mouseX, mouseY);
		}
        graphics.blit(WINDOW, this.guiLeft, this.guiTop, 0, 0, windowWidth, windowHeight);
		if (this.origin.isBound()) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 5);
			this.renderOriginName(graphics);
			RenderSystem.setShaderTexture(0, WINDOW);
			this.renderOriginImpact(graphics, mouseX, mouseY);
            graphics.pose().popPose();
			Component title = this.getTitleText();
			graphics.drawCenteredString(this.font, title.getString(), this.width / 2, this.guiTop - 15, 0xFFFFFF);
		}
		RenderSystem.disableBlend();
	}

	private void renderOriginImpact(GuiGraphics graphics, int mouseX, int mouseY) {
		Impact impact = this.getCurrentOrigin().get().getImpact();
		int impactValue = impact.getImpactValue();
		int wOffset = impactValue * 8;
		for (int i = 0; i < 3; i++) {
			if (i < impactValue) {
				graphics.blit(WINDOW, this.guiLeft + 128 + i * 10, this.guiTop + 19, windowWidth + wOffset, 16, 8, 8);
			} else {
                graphics.blit(WINDOW, this.guiLeft + 128 + i * 10, this.guiTop + 19, windowWidth, 16, 8, 8);
			}
		}
		if (mouseX >= this.guiLeft + 128 && mouseX <= this.guiLeft + 158
			&& mouseY >= this.guiTop + 19 && mouseY <= this.guiTop + 27) {
			Component ttc = Component.translatable(Origins.MODID + ".gui.impact.impact").append(": ").append(impact.getTextComponent());
            graphics.renderTooltip(this.font, ttc, mouseX, mouseY);
		}
	}

	private void renderOriginName(GuiGraphics graphics) {
		FormattedText originName = this.font.substrByWidth(this.getCurrentOrigin().get().getName(), windowWidth - 36);
		graphics.drawString(this.font, originName.getString(), this.guiLeft + 39, this.guiTop + 19, 0xFFFFFF);
		ItemStack is = this.getCurrentOrigin().get().getIcon();
		graphics.renderItem(is, this.guiLeft + 15, this.guiTop + 15);
	}

	private void renderWindowBackground(GuiGraphics graphics, int offsetYStart, int offsetYEnd) {
		int border = 13;
		int endX = this.guiLeft + windowWidth - border;
		int endY = this.guiTop + windowHeight - border;
		for (int x = this.guiLeft; x < endX; x += 16) {
			for (int y = this.guiTop + offsetYStart; y < endY + offsetYEnd; y += 16) {
                graphics.blit(WINDOW, x, y, windowWidth, 0, Math.max(16, endX - x), Math.max(16, endY + offsetYEnd - y));
			}
		}
	}

	@Override
	public boolean mouseScrolled(double x, double y, double z) {
		boolean retValue = super.mouseScrolled(x, y, z);
		int np = this.scrollPos - (int) z * 4;
		this.scrollPos = np < 0 ? 0 : Math.min(np, this.currentMaxScroll);
		return retValue;
	}

	private void renderOriginContent(GuiGraphics graphics, int mouseX, int mouseY) {
		int textWidth = windowWidth - 48;
		// Without this code, the text may not cover the whole width of the window
		// if the scrollbar isn't shown. However, with this code, you'll see 1 frame
		// of misaligned text because the text length (and whether scrolling is enabled)
		// is only evaluated on first render. :(
        /*if(!canScroll()) {
            textWidth += 12;
        }*/

		Origin origin = this.getCurrentOrigin().get();
		int x = this.guiLeft + 18;
		int y = this.guiTop + 50;
		int startY = y;
		int endY = y - 72 + windowHeight;
		y -= this.scrollPos;

		Component orgDesc = origin.getDescription();
		List<FormattedCharSequence> descLines = this.font.split(orgDesc, textWidth);
		for (FormattedCharSequence line : descLines) {
			if (y >= startY - 18 && y <= endY + 12) {
                graphics.drawString(this.font, line, x + 2, y - 6, 0xCCCCCC, false);
			}
			y += 12;
		}

		if (this.isOriginRandom) {
			List<FormattedCharSequence> drawLines = this.font.split(this.randomOriginText, textWidth);
			for (FormattedCharSequence line : drawLines) {
				y += 12;
				if (y >= startY - 24 && y <= endY + 12) {
                    graphics.drawString(this.font, line, x + 2, y, 0xCCCCCC, false);
				}
			}
			y += 14;
		} else {
			Registry<ConfiguredPower<?, ?>> powers = ApoliAPI.getPowers();
			// eruto patch: 飛ばした能力を1回だけ記録に出す。
			//
			// ⚠⚠ **元の実装は、能力を黙って飛ばす。** ログも代わりの表示も出ないので、
			// 画面には「説明文まで出て、そこで終わる」としか見えない。
			// ⚠ **当部は実際にこれで詰まった**（珠の画面に能力が1つも並ばない・全種族全職業）。
			// ⚠ 読むだけでは原因を詰め切れなかったので、飛ばした理由を機械に言わせる。
			//
			// ⚠ 画面は毎フレーム描かれるので、**同じ種族について1回だけ**出す。
			int skippedUnbound = 0, skippedHidden = 0, skippedNoId = 0, shown = 0;
			// ⚠⚠ **どの能力が解決できていないかを控える**（2026-09-01 追加）。
			//    ⚠ 未結合の Holder でも `unwrap()` は **id の側（左）を返す**ので、
			//    ⚠ **名前は取れる**。⚠ 件数だけでは「どれが」を追えず、2度手間になった。
			java.util.List<String> unboundIds = new java.util.ArrayList<>();
			List<Holder<ConfiguredPower<?, ?>>> rawPowers = origin.getValidPowers().toList();
			for (Holder<ConfiguredPower<?, ?>> raw : rawPowers) {
				// ⚠⚠ **当部の直し（2026-09-01）: 結合していない能力を、id で引き直す。**
				//
				//    ⚠ **段5 と同じ形を1段下でやる。** 段5 は**種族**を層から引き直したが、
				//    ⚠⚠ **種族が抱えている能力の Holder は引き直していなかった。**
				//
				//    ⚠ **なぜ要るか（実機で測った・2026-09-01）**: 同じ jar・同じデータで
				//    ⚠⚠ **16:36 の走行は 26 件が結合せず、16:58 の走行は 0 件**だった。
				//    ⚠ **間欠**——`S2CDynamicRegistryPacket.handle` が `start == 0` のとき
				//    `instance.reset(key)` で**レジストリのオブジェクトごと**作り直すので、
				//    ⚠ **種族を復号した後に能力のレジストリが作り直されると、
				//    その種族が抱えている Holder は永久に古い世代を指したまま**になる。
				//
				//    ⚠ 画面には「説明文で終わっている」としか見えない
				//    （シュルクなら「硬い皮膚」だけ・ネコ獣人なら3つだけ、が実際に出た）。
				//
				//    ⚠ 引き直せないとき（id が引けない／レジストリに無い）は**元のまま**進み、
				//    ⚠ 下の記録が名指しする。⚠⚠ **黙って落とさない。**
				Holder<ConfiguredPower<?, ?>> holder = raw;
				if (!holder.isBound()) {
					Optional<ResourceKey<ConfiguredPower<?, ?>>> key = raw.unwrap().left();
					if (key.isPresent()) {
						Optional<Holder.Reference<ConfiguredPower<?, ?>>> fresh =
								powers.getHolder(key.get());
						if (fresh.isPresent() && fresh.get().isBound()) {
							holder = fresh.get();
							Origins.LOGGER.warn("[eruto] re-resolved stale power {}",
									key.get().location());
						}
					}
				}
				if (!holder.isBound()) {
					skippedUnbound++;
					holder.unwrap().ifLeft(k -> unboundIds.add(k.location().toString()));
					continue;
				}
				if (holder.get().getData().hidden()) {
					skippedHidden++;
					continue;
				}
				Optional<ResourceLocation> id = holder.unwrap().map(Optional::of, powers::getResourceKey).map(ResourceKey::location);
				if (id.isEmpty()) {
					skippedNoId++;
					continue;
				}
				shown++;
				ConfiguredPower<?, ?> p = holder.get();
				FormattedCharSequence name = Language.getInstance().getVisualOrder(this.font.substrByWidth(p.getData().getName().withStyle(ChatFormatting.UNDERLINE), textWidth));
				Component desc = p.getData().getDescription();
				List<FormattedCharSequence> drawLines = this.font.split(desc, textWidth);
				if (y >= startY - 24 && y <= endY + 12) {
					graphics.drawString(this.font, name, x, y, 0xFFFFFF, false);
					int tw = this.font.width(name);
					Collection<Badge> badges = BadgeManager.getPowerBadges(id.get());
					int xStart = x + tw + 4;
					int bi = 0;
					for (Badge badge : badges) {
						RenderedBadge renderedBadge = new RenderedBadge(p, badge, xStart + 10 * bi, y - 1);
						this.renderedBadges.add(renderedBadge);
						graphics.blit(badge.spriteId(), xStart + 10 * bi, y - 1, 0, 0, 9, 9, 9, 9);
						bi++;
					}
				}
				for (FormattedCharSequence line : drawLines) {
					y += 12;
					if (y >= startY - 24 && y <= endY + 12) {
                        graphics.drawString(this.font, line, x + 2, y, 0xCCCCCC, false);
					}
				}
				y += 14;
			}
			// eruto patch: この種族について1回だけ、内訳を記録に出す（上の注記を参照）。
			this.logPowerTally(origin, rawPowers.size(), shown, skippedUnbound, skippedHidden,
					skippedNoId, unboundIds);
		}
		y += this.scrollPos;
		this.currentMaxScroll = y - 14 - (this.guiTop + 158);
		if (this.currentMaxScroll < 0) {
			this.currentMaxScroll = 0;
		}
	}

	/** eruto patch: 一度出した内訳を控えておく（下の {@link #logPowerTally} が使う）。 */
	private static final java.util.Set<String> LOGGED_TALLIES = new java.util.HashSet<>();

	/**
	 * eruto patch: 能力を何件飛ばしたかを、種族ごとに1回だけ記録へ出す。
	 *
	 * <p>⚠⚠ 上流は3つの理由（{@code !isBound()} ／ {@code hidden} ／ id が引けない）で
	 * 能力を飛ばすが、⚠ <b>どれも黙って飛ばす</b>。画面には「説明文で終わっている」としか
	 * 見えず、⚠ <b>ログにも1行も出ない</b>。
	 *
	 * <p>⚠ 画面は毎フレーム描かれるので、<b>同じ種族について1回だけ</b>出す。
	 * ⚠ 飛ばした件数が 0 のときは何も言わない（鳴り続ける記録は本物を埋める）。
	 */
	private void logPowerTally(Origin origin, int raw, int shown, int unbound, int hidden,
			int noId, java.util.List<String> unboundIds) {
		// 鳴らす条件は2つ:
		//
		//   ⑴ 解決できない／id が引けない が1件でもある
		//        … ⚠ 上流が黙って飛ばす形。
		//   ⑵ ⚠⚠ **一覧そのものが空**（raw == 0）
		//        … ⚠ 2026-08-27 の走行で `S2CDynamicRegistryPacket.decode` の中の
		//          `ImmutableList$Builder.add(null)` が NullPointerException になり、
		//          ⚠⚠ **一覧の要素が null になる**のを1回つかまえている（間欠）。
		//          ⚠ **そのときは飛ばした件数が全部 0 になる**ので、⑴ だけでは鳴らない。
		//
		// ⚠ `hidden` だけなら黙る（当部が意図して隠している分）。
		// ⚠ `origins:human` は本当に能力 0 なので ⑵ で1度だけ鳴るが、
		//   ⚠ **出る行に raw も shown も載るので、読めば正常と分かる**（黙らせない）。
		if (unbound == 0 && noId == 0 && raw != 0)
			return;
		// ⚠⚠ **逆引きの答えを鵜呑みにしない**（2026-09-01）。
		//    ⚠ `getKey(origin)` は**そのオブジェクトがレジストリに居るか**を引く。
		//    ⚠⚠ 実際に `一覧 6〜18 本` の種族について `origins:empty` が出た——
		//    ⚠ **描いている実体が、いまのレジストリの物ではない**ことを示している。
		//    ⚠ だから**種族が自分で名乗る名前も併記する**（逆引きに頼らない2本目の入口）。
		// ⚠⚠ **`getKey` は「見つからない」を `null` ではなく既定のキーで返す**（2026-09-01）。
		//    `OriginRegisters.java:42` が `.setDefaultKey(Origins.identifier("empty"))` を
		//    指定しているため、⚠ **登録されていない実体を引くと `origins:empty` が返る。**
		//    ⚠ 実際、⚠⚠ **一覧 6〜18 本の種族について `origins:empty` が出た**——
		//    ⚠ 「逆引きが壊れている」ではなく「**その実体はいまのレジストリに居ない**」が正しい読み。
		//    ⚠ 紛らわしいので、⚠ **居るかどうかを別の欄で直接出す。**
		Object key = OriginsAPI.getOriginsRegistry().getKey(origin);
		boolean inRegistry = OriginsAPI.getOriginsRegistry().stream()
				.anyMatch(o -> o == origin);
		// ⚠⚠ **診断の記録は英語と id だけにする**（2026-09-01・依頼者の指示）。
		//    「⚠ 私が読むためのものは英語、⚠ **あなたと部員に見せるものは日本語**」。
		//    ⚠ **画面に出る文は日本語のまま**——ここはログだけの話。
		//
		//    ⚠ **なぜ表示名を出さないか**: 表示名は翻訳された日本語で、
		//    ⚠ Minecraft のログの出力は機械の既定の文字コードを使う（当部の Windows は cp932）。
		//    ⚠⚠ **UTF-8 前提の道具で引くと読めない**——2026-09-01 に私がそれで
		//    「0 件＝正常」と誤報した。⚠ **翻訳の鍵なら ASCII なので、どの道具でも引ける。**
		String selfName;
		try {
			net.minecraft.network.chat.ComponentContents c = origin.getName().getContents();
			selfName = (c instanceof net.minecraft.network.chat.contents.TranslatableContents t)
					? t.getKey()                       // 例: origin.origins.human.name
					: "(not-translatable)";
		} catch (Exception e) {
			selfName = "(name-failed:" + e.getClass().getSimpleName() + ")";
		}
		// ⚠ 画面は毎フレーム描かれるので、**同じ内訳につき1回だけ**出す。
		if (!LOGGED_TALLIES.add(key + "/" + selfName + "/" + inRegistry + "/" + raw + "/"
				+ shown + "/" + unbound + "/" + hidden + "/" + noId))
			return;
		Origins.LOGGER.warn(
				"[eruto] origin key={} name={} inRegistry={} powers raw={} shown={} "
						+ "unbound={} hidden={} noId={}",
				key, selfName, inRegistry, raw, shown, unbound, hidden, noId);
		// ⚠⚠ **どれが解決できていないかを名指しする。** 件数だけでは追えない。
		if (!unboundIds.isEmpty()) {
			Origins.LOGGER.warn("[eruto]   unbound powers: {}", String.join(", ", unboundIds));
			// ⚠⚠ **能力のレジストリに在るかを1件ずつ聞く**（2026-09-01）。
			//
			//    ⚠ **なぜ要るか**: 「結合していない」には2つの原因が在り、
			//    ⚠ **直し方が正反対**なのに、⚠⚠ **画面からは見分けが付かない**:
			//
			//      ⓐ レジストリにその id が**無い**
			//         … ⚠ 読み込みか同期で落ちている。⚠ **混ぜ方（データ）の問題。**
			//      ⓑ レジストリに**在るのに**結合していない
			//         … ⚠ 種族を復号した時点でまだ登録されていなかった。
			//         ⚠⚠ **順序の問題**で、データを直しても直らない。
			//
			//    ⚠ 2026-09-01 に、ⓐとⓑのどちらかを4回推測して4回とも決められなかった。
			//    ⚠ **推測を3回外したら機械に聞く**（machine-global-rules の引き金）。
			int inReg = 0;
			StringBuilder absent = new StringBuilder();
			for (String id : unboundIds) {
				boolean has = false;
				try {
					net.minecraft.resources.ResourceLocation rl =
							new net.minecraft.resources.ResourceLocation(id);
					has = io.github.edwinmindcraft.apoli.api.ApoliAPI.getPowers()
							.containsKey(rl);
				} catch (Exception ignored) {
					// ⚠ 引けないときは「無い」側に数える（甘く見ない）
				}
				if (has) {
					inReg++;
				} else {
					absent.append(absent.length() == 0 ? "" : ", ").append(id);
				}
			}
			Origins.LOGGER.warn(
					"[eruto]   power registry: total={} unbound-but-present={} absent={}",
					io.github.edwinmindcraft.apoli.api.ApoliAPI.getPowers().size(),
					inReg, absent.length() == 0 ? "(none)" : absent);
		}
	}

	private class RenderedBadge {
		private final ConfiguredPower<?, ?> powerType;
		private final Badge badge;
		private final int x;
		private final int y;

		public RenderedBadge(ConfiguredPower<?, ?> powerType, Badge badge, int x, int y) {
			this.powerType = powerType;
			this.badge = badge;
			this.x = x;
			this.y = y;
		}

		public boolean hasTooltip() {
			return this.badge.hasTooltip();
		}

		public List<ClientTooltipComponent> getTooltipComponents(Font textRenderer, int widthLimit) {
			return this.badge.getTooltipComponents(this.powerType, widthLimit, OriginDisplayScreen.this.time, textRenderer);
		}
	}
}
