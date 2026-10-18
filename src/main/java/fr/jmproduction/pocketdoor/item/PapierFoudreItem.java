package fr.jmproduction.pocketdoor.item;

import fr.jmproduction.pocketdoor.Content;
import fr.jmproduction.pocketdoor.entity.PapierFoudreProjectile;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public class PapierFoudreItem extends Item {
    public static final int CHARGE_TICKS = 30;
    public static final int MAX_USE_DURATION = 72000;

    public PapierFoudreItem(Properties properties) {
        super(properties);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return MAX_USE_DURATION;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player) || level.isClientSide) {
            return;
        }
        int usedTicks = MAX_USE_DURATION - timeLeft;
        if (usedTicks < CHARGE_TICKS) {
            return;
        }

        PapierFoudreProjectile projectile = new PapierFoudreProjectile(level, player);
        ItemStack projectileStack = new ItemStack(Content.PAPIER_FOUDRE);
        projectileStack.getOrCreateTag().putInt("CustomModelData", 1);
        projectile.setItem(projectileStack);
        projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.5F, 1.0F);
        level.addFreshEntity(projectile);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS,
                0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }

    public static float charge(ItemStack stack, Level level, Player player) {
        if (!player.isUsingItem() || player.getUseItem() != stack) {
            return 0.0F;
        }
        int usedTicks = MAX_USE_DURATION - player.getUseItemRemainingTicks();
        if (usedTicks >= CHARGE_TICKS) return 1.0F;
        if (usedTicks >= 20) return 0.6666667F;
        if (usedTicks >= 10) return 0.33333334F;
        return 0.0F;
    }
}