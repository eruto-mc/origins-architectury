package net.erutobusiness.shiftingorigins.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.client.extensions.common.IClientMobEffectExtensions;

/**
 * 浮遊のアイコンに、<b>バニラの浮遊の絵をそのまま使う</b>。
 *
 * <p>⚠ <b>絵を同梱していない</b>——実行時にバニラの絵を指すだけなので、
 * Mojang の素材を配らずに済む。⚠ 見た目も本家と完全に同じになる。
 *
 * <p>⚠ 名前のほうは {@code HoverEffect.getDescriptionId()} が
 * {@code effect.minecraft.levitation} を指しているので、<b>訳も全言語ぶんバニラのものが出る</b>。
 * ⚠ 当部が lang を持たないので、<b>ずれようが無い</b>。
 *
 * <p>⚠ <b>なぜ本家と同じ見た目でよいか</b>: 上がり方の計算もバニラの浮遊そのものだから
 * （{@code HoverMixin}）。⚠ 違うのは「他の MOD に剥がされないこと」だけ。
 *
 * <h2>⚠⚠ ここだけでは足りない（2026-09-06 に足した）</h2>
 *
 * ⚠ <b>この口を通るのはバニラの2か所だけ</b>——{@code Gui.renderEffects}（画面端）と
 * {@code EffectRenderingInventoryScreen}（持ち物の画面）。
 * ⚠⚠ <b>JEI（JEED）は {@code MobEffectTextureManager} を直に引く</b>ので、
 * この口を通らず <b>{@code shiftingorigins:hover} という名前の絵</b>を探しに行き、
 * ⚠ 見つからず<b>黒紫の格子</b>を出していた。
 *
 * <p>⚠ 直したのは <b>{@code assets/minecraft/atlases/mob_effects.json}</b>——
 * バニラの絵に {@code shiftingorigins:hover} という<b>別名を1つ足すだけ</b>で、
 * ⚠ <b>絵は1枚も同梱していない</b>（上の方針のまま）。
 *
 * <p>⚠ <b>アトラスの定義は重なる</b>（置き換わらない）ことを実物で確かめてある——
 * このパックの MOD 15 本が {@code assets/minecraft/atlases/} に足していて、
 * バニラのブロックは壊れていない。⚠ バニラの {@code mob_effects} は
 * {@code {"type":"directory","source":"mob_effect","prefix":""}} なので、
 * ⚠ <b>絵の名前は「名前空間:ファイル名」</b>（例: {@code parcool:inexhaustible}）。
 */
public final class HoverEffectClient implements IClientMobEffectExtensions {

  public static final HoverEffectClient INSTANCE = new HoverEffectClient();

  private HoverEffectClient() {
  }

  private static TextureAtlasSprite vanillaSprite() {
    return Minecraft.getInstance().getMobEffectTextures().get(MobEffects.LEVITATION);
  }

  @Override
  public boolean renderGuiIcon(MobEffectInstance instance, Gui gui, GuiGraphics graphics,
                               int x, int y, float z, float alpha) {
    graphics.blit(x + 3, y + 3, 0, 18, 18, vanillaSprite());
    return true;
  }

  @Override
  public boolean renderInventoryIcon(MobEffectInstance instance,
                                     EffectRenderingInventoryScreen<?> screen,
                                     GuiGraphics graphics, int x, int y, int blitOffset) {
    graphics.blit(x + 6, y + 7, 0, 18, 18, vanillaSprite());
    return true;
  }
}
