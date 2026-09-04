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
