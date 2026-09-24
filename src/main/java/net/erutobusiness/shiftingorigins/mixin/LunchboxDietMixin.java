package net.erutobusiness.shiftingorigins.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.edwinmindcraft.apoli.common.power.PreventItemActionPower;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 弁当箱（SolApplePie の弁当袋・弁当箱・金の弁当箱）に、<b>持ち主が口にできる物だけ</b>を選ばせる
 * （2026-09-24・あなたの判断「飛ばせるのいいね」）。
 *
 * <p>⚠⚠ <b>上流の弁当箱は食事の制限を見ない。</b>中身を選ぶ {@code getBestFoodSlot} は
 * 「食べ物で、空でない物のうち、SolApplePie の点が一番上がる物」を選ぶだけで、
 * ⚠ 選んだ物を {@code ItemStack.finishUsingItem} で<b>直に</b>食べさせる。
 * ⚠ Apoli の「使わせない」（肉食・菜食）は<b>右クリックの入口</b>で止める作りなので、
 * ⚠⚠ <b>弁当箱を通すと素通りする</b>。
 *
 * <p>⚠ 上流はその穴を「制限食の種族には弁当箱を使わせない」で塞ぐつもりでいた
 * （{@code processRightClick} が {@code Origins.hasRestrictedDiet} を見て何もしない）。
 * ⚠ ただしその判定は、種族を文字にした物に {@code "[origins:vegetarian]"} などが含まれるかで見る。
 * ⚠⚠ <b>2026-09-24 の実測で、エルフ（菜食は種族を選んだときに後から付く）は弁当箱から肉を食べられた</b>
 * （台本 {@code origins-diet-lunchbox}。記録は
 * {@code mod-notes/solapplepie.md}）。
 *
 * <p>⚠ ここでは2か所だけ差し替える:
 * <ol>
 *   <li>{@code getBestFoodSlot} の「食べ物か」に、⚠ <b>その人が普段この物を口にできるか</b>
 *       （{@link PreventItemActionPower#isUsagePrevented}）を足す。⚠ <b>食べ終わりの処理も同じ所を通る</b>ので、
 *       選ぶ所と食べる所がずれない</li>
 *   <li>{@code processRightClick} の「制限食か」を、⚠ <b>「口にできる物が1つも入っていないか」</b>に置き換える。
 *       ⚠ 何も選べないときは食べる動きを始めない（空振りで 32 ティック待たせない）</li>
 * </ol>
 *
 * <p>⚠ 判定は Apoli の能力そのもの（肉食・菜食ほか「使わせない」全部）を借りる。
 * ⚠ <b>当部で「肉か」を書き直さない</b>——普段の右クリックと弁当箱で答えがずれるのを防ぐ。
 *
 * <p>⚠ 弁当箱そのものは肉食に止められる（箱が「食べ物」を名乗るため）。
 * ⚠ 箱は {@code origins:ignore_diet} に入れて通している（{@code origins_setup/build.py}）。
 *
 * <p>⚠ {@code @Pseudo}: SolApplePie を抜いた日に、当て先が無いまま起動が落ちないようにする。
 */
@Pseudo
@Mixin(targets = "com.tarinoita.solsweetpotato.item.foodcontainer.FoodContainerItem", remap = false)
public abstract class LunchboxDietMixin {

  @Shadow
  public static ItemStackHandler getInventory(final ItemStack stack) {
    throw new AssertionError();
  }

  @Shadow
  public static int getBestFoodSlot(final ItemStackHandler inventory, final Player player) {
    throw new AssertionError();
  }

  @WrapOperation(method = "getBestFoodSlot", at = @At(value = "INVOKE",
      target = "Lnet/minecraft/world/item/ItemStack;isEdible()Z", remap = true))
  private static boolean shiftingorigins$onlyWhatTheyEat(final ItemStack food,
      final Operation<Boolean> original, @Local(argsOnly = true) final Player player) {
    return original.call(food) && !PreventItemActionPower.isUsagePrevented(player, food);
  }

  // ⚠ `original` は呼ばない——上流の「制限食か」は、ここで置き換える問いと別物なので要らない。
  @WrapOperation(method = "processRightClick", at = @At(value = "INVOKE",
      target = "Lcom/tarinoita/solsweetpotato/integration/Origins;hasRestrictedDiet(Lnet/minecraft/world/entity/player/Player;)Z"))
  private boolean shiftingorigins$nothingToEat(final Player player,
      final Operation<Boolean> original, @Local(argsOnly = true) final InteractionHand hand) {
    final ItemStackHandler inventory = getInventory(player.getItemInHand(hand));
    return inventory == null || getBestFoodSlot(inventory, player) < 0;
  }
}
