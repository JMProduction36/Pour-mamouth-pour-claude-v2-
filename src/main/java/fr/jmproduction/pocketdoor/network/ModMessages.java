package fr.jmproduction.pocketdoor.network;

import fr.jmproduction.pocketdoor.PocketDoorMod;
import fr.jmproduction.pocketdoor.data.PocketDoorSavedData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class ModMessages {
    public static final ResourceLocation OPEN_POCKET_DOOR = id("open_pocket_door");
    public static final ResourceLocation START_DIALOGUE = id("start_dialogue");
    public static final ResourceLocation DIALOGUE_FINISHED = id("dialogue_finished");

    private static final List<Pending> PENDING = new ArrayList<>();
    private static final List<Placement> PLACEMENTS = new ArrayList<>();
    private static final Set<UUID> ACTIVE = new HashSet<>();

    private record Pending(ServerPlayer player, int ticks, boolean blindnessApplied) {
    }

    private record Placement(ServerPlayer player, int ticks) {
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(PocketDoorMod.MOD_ID, path);
    }

    public static void registerC2SPackets() {
        ServerPlayNetworking.registerGlobalReceiver(
                OPEN_POCKET_DOOR,
                (server, player, handler, buffer, responseSender) -> server.execute(
                        () -> PLACEMENTS.add(new Placement(player, 60))
                )
        );

        ServerPlayNetworking.registerGlobalReceiver(
                DIALOGUE_FINISHED,
                (server, player, handler, buffer, responseSender) -> server.execute(
                        () -> {
                            PocketDoorSavedData.get(player.getLevel()).markFinished(player.getUUID());
                            player.removeEffect(MobEffects.DARKNESS);
                            player.removeEffect(MobEffects.BLINDNESS);
                            ACTIVE.remove(player.getUUID());
                        }
                )
        );

        ServerTickEvents.END_SERVER_TICK.register(ModMessages::tick);
    }

    public static void beginFromDoor(ServerPlayer player) {
        beginSequence(player);
    }

    private static void beginSequence(ServerPlayer player) {
        if (!ACTIVE.add(player.getUUID())) {
            return;
        }

        // Audio is server-owned so a duplicate client packet cannot duplicate it.
        player.getLevel().playSound(null, player.blockPosition(), PocketDoorMod.AMBIANCE_SILGO,
                net.minecraft.sounds.SoundSource.AMBIENT, 1.0F, 1.0F);
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, Integer.MAX_VALUE, 0, false, false));
        PENDING.add(new Pending(player, 100, false));
    }

    private static void tick(MinecraftServer server) {
        for (int index = PLACEMENTS.size() - 1; index >= 0; index--) {
            Placement placement = PLACEMENTS.get(index);
            if (!placement.player().isAlive() || placement.player().connection == null) {
                PLACEMENTS.remove(index);
            } else if (placement.ticks() <= 1) {
                place(placement.player());
                PLACEMENTS.remove(index);
            } else {
                PLACEMENTS.set(index, new Placement(placement.player(), placement.ticks() - 1));
            }
        }

        for (int index = PENDING.size() - 1; index >= 0; index--) {
            Pending pending = PENDING.get(index);
            ServerPlayer player = pending.player();

            if (!player.isAlive() || player.connection == null) {
                ACTIVE.remove(player.getUUID());
                PENDING.remove(index);
                continue;
            }

            int ticks = pending.ticks() - 1;
            boolean blindnessApplied = pending.blindnessApplied();
            if (!blindnessApplied && ticks <= 90) {
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, Integer.MAX_VALUE, 0, false, false));
                blindnessApplied = true;
            }

            if (ticks <= 0) {
                sendStartDialogue(player);
                PENDING.remove(index);
            } else {
                PENDING.set(index, new Pending(player, ticks, blindnessApplied));
            }
        }
    }

    private static void place(ServerPlayer player) {
        ServerLevel level = player.getLevel();
        Direction facing = player.getDirection().getOpposite();
        BlockPos position = player.blockPosition().relative(player.getDirection(), 6);
        position = new BlockPos(
                position.getX(),
                level.getHeight(Heightmap.Types.MOTION_BLOCKING, position.getX(), position.getZ()),
                position.getZ()
        );

        level.setBlock(
                position,
                PocketDoorMod.POCKET_DOOR.defaultBlockState().setValue(
                        HorizontalDirectionalBlock.FACING,
                        facing
                ),
                3
        );
        level.sendParticles(
                ParticleTypes.FLAME,
                position.getX() + 0.5,
                position.getY() + 1,
                position.getZ() + 0.5,
                12,
                0.3,
                0.5,
                0.3,
                0.02
        );
    }

    public static boolean isFinished(ServerPlayer player) {
        return PocketDoorSavedData.get(player.getLevel()).isFinished(player.getUUID());
    }

    public static void sendStartDialogue(ServerPlayer player) {
        ServerPlayNetworking.send(player, START_DIALOGUE, PacketByteBufs.empty());
    }

    private ModMessages() {
    }
}