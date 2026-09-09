package net.erutobusiness.shiftingorigins;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * 相手の MOD が居るときだけ当てる mixin を選り分ける。
 *
 * <p>⚠⚠ <b>Easy Magic を名指しする mixin が2つある</b>（司書の「予告がすべて読める」）。
 * ⚠ 名指しした型が無いまま当てにいくと<b>起動時に落ちる</b>ので、
 * ⚠ <b>読み込まれているかを見てから</b>当てる。
 *
 * <p>⚠ 書き方は {@code io.github.edwinmindcraft.apoli.mixin.ApoliMixinPlugin} に合わせた
 * （同じ jar の中で citadel と ears を同じやり方で外している）。
 *
 * <p>⚠ このクラスは<b>mixin のパッケージの外</b>に置く——
 * 中に置くと注釈処理が mixin と読み違える。
 */
public class ShiftingOriginsMixinPlugin implements IMixinConfigPlugin {

  private static boolean classExists(final String cls) {
    try {
      Class.forName(cls, false, ShiftingOriginsMixinPlugin.class.getClassLoader());
      return true;
    } catch (final ClassNotFoundException e) {
      return false;
    }
  }

  private boolean easyMagicLoaded;

  @Override
  public void onLoad(final String mixinPackage) {
    this.easyMagicLoaded = classExists("fuzs.easymagic.EasyMagic");
  }

  @Override
  public String getRefMapperConfig() {
    return null;
  }

  @Override
  public boolean shouldApplyMixin(final String targetClassName, final String mixinClassName) {
    if (mixinClassName.endsWith(".EasyMagicHintMixin")
        || mixinClassName.endsWith(".EasyMagicSendAccessor")) {
      return this.easyMagicLoaded;
    }
    return true;
  }

  @Override
  public void acceptTargets(final Set<String> myTargets, final Set<String> otherTargets) {
  }

  @Override
  public List<String> getMixins() {
    return null;
  }

  @Override
  public void preApply(final String targetClassName, final ClassNode targetClass,
      final String mixinClassName, final IMixinInfo mixinInfo) {
  }

  @Override
  public void postApply(final String targetClassName, final ClassNode targetClass,
      final String mixinClassName, final IMixinInfo mixinInfo) {
  }
}
