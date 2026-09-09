package net.erutobusiness.shiftingorigins.mixin;

import fuzs.easymagic.config.ServerConfig;
import fuzs.easymagic.world.inventory.ModEnchantmentMenu;
import net.erutobusiness.shiftingorigins.LibrarianEnchanting;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 司書だけ、エンチャントの予告を<b>3段ぶん全部</b>出す。
 *
 * <p>⚠⚠ <b>ここだけは相手の MOD を名指ししている。</b> この構成のエンチャントテーブルは
 * Easy Magic が差し替えており、⚠ <b>予告を描いているのは向こうの画面</b>
 * （{@code ModEnchantmentScreen}）。⚠ バニラの欄（{@code enchantClue}）に何を書いても、
 * ⚠⚠ <b>向こうの一覧には1行も出ない</b>ので、バニラの型だけでは届かない。
 *
 * <p>⚠ 当てる先は {@code getEnchantmentHint(ItemStack, int, EnchantmentHint)} の第3引数。
 * 実物を逆アセンブルして確かめた中身は<b>3択</b>——
 * {@code NONE} は空、{@code SINGLE} は候補から乱数で1つ、
 * {@code ALL} は<b>候補をそのまま全部</b>返す。⚠ <b>司書のときだけ {@code ALL} を渡す。</b>
 *
 * <p>⚠ 候補を作っているのは {@code createEnchantmentInstance} →
 * <b>バニラの {@code EnchantmentMenu.getEnchantmentList}</b>（向こうのアクセサ経由）。
 * ⚠⚠ つまり<b>「本には多く付く」の {@link EnchantmentMenuBookMixin} も同じ経路を通る</b>ので、
 * ⚠ 司書の予告は<b>捨てない側の一覧</b>で出る。
 *
 * <p>⚠ Easy Magic を抜いた日に落ちないよう、
 * {@code ShiftingOriginsMixinPlugin} が<b>読み込まれているときだけ</b>当てる。
 */
@Mixin(value = ModEnchantmentMenu.class, remap = false)
public abstract class EasyMagicHintMixin {

  /** ⚠ 向こうの private な欄。⚠⚠ <b>画面から人を引ける唯一の口</b>なので影で借りる。 */
  @Shadow
  @Final
  private Player player;

  @ModifyVariable(method = "getEnchantmentHint", at = @At("HEAD"), argsOnly = true)
  private ServerConfig.EnchantmentHint shiftingorigins$allClues(
      final ServerConfig.EnchantmentHint hint) {

    return LibrarianEnchanting.readsAllClues(this.player)
        ? ServerConfig.EnchantmentHint.ALL
        : hint;
  }
}
