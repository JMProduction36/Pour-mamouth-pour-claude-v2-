package fr.jmproduction.pocketdoor.entity;

import fr.jmproduction.pocketdoor.Content;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;

public class PapierFoudreProjectile extends ThrowableItemProjectile {
    public PapierFoudreProjectile(EntityType<? extends PapierFoudreProjectile> type, Level level) {
        super(type, level);
    }

    public PapierFoudreProjectile(Level level, LivingEntity owner) {
        super(Content.PAPIER_FOUDRE_PROJECTILE, owner, level);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }

    @Override
    protected Item getDefaultItem() {
        return Content.PAPIER_FOUDRE;
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level.isClientSide && level instanceof ServerLevelAccessor) {
            BlockPos impact = blockPosition();
            LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level);
            if (lightning != null) {
                lightning.moveTo(impact.getX() + 0.5D, impact.getY(), impact.getZ() + 0.5D);
                lightning.setVisualOnly(false);
                level.addFreshEntity(lightning);
            }
            if (getOwner() instanceof Player player && !player.getAbilities().instabuild) {
                player.getMainHandItem().shrink(1);
            }
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER,
                    SoundSource.WEATHER, 5.0F, 1.0F);
            discard();
        }
    }
}
