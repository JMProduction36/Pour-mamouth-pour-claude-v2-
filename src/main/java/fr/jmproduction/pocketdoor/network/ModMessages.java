package fr.jmproduction.pocketdoor.network;

import fr.jmproduction.pocketdoor.PocketDoorMod;
import fr.jmproduction.pocketdoor.data.PocketDoorSavedData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.levelgen.Heightmap;
import java.util.ArrayList;
import java.util.List;

public final class ModMessages {
    public static final ResourceLocation OPEN_POCKET_DOOR = id("open_pocket_door");
    public static final ResourceLocation START_DIALOGUE = id("start_dialogue");
    public static final ResourceLocation DIALOGUE_FINISHED = id("dialogue_finished");
    private static final List<Pending> PENDING = new ArrayList<>();
    private static final List<BlindnessTask> BLINDNESS_TASKS = new ArrayList<>();
    private record Pending(ServerPlayer player, int ticks) {}
    private record BlindnessTask(ServerPlayer player, int ticks) {}
    private static ResourceLocation id(String path) { return new ResourceLocation(PocketDoorMod.MOD_ID, path); }

    public static void registerC2SPackets() {
        ServerPlayNetworking.registerGlobalReceiver(OPEN_POCKET_DOOR, (server, player, handler, buffer, responseSender) -> server.execute(() -> PENDING.add(new Pending(player, 60))));
        ServerPlayNetworking.registerGlobalReceiver(DIALOGUE_FINISHED, (server, player, handler, buffer, responseSender) -> server.execute(() -> {
            PocketDoorSavedData.get(player.getLevel()).markFinished(player.getUUID());
            clearDialogueEffects(player);
        }));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> server.execute(() -> {
            clearDialogueEffects(handler.player);
            PENDING.removeIf(p -> p.player() == handler.player);
            BLINDNESS_TASKS.removeIf(t -> t.player() == handler.player);
        }));
        ServerTickEvents.END_SERVER_TICK.register(ModMessages::tick);
    }

    public static void scheduleDialogueBlindness(ServerPlayer player, int delayTicks) {
        BLINDNESS_TASKS.removeIf(task -> task.player() == player);
        BLINDNESS_TASKS.add(new BlindnessTask(player, delayTicks));
    }

    private static void tick(MinecraftServer server) {
        for (int i = BLINDNESS_TASKS.size() - 1; i >= 0; i--) {
            BlindnessTask task = BLINDNESS_TASKS.get(i);
            if (!task.player().isAlive() || task.player().connection == null) { BLINDNESS_TASKS.remove(i); continue; }
            if (task.ticks() <= 1) {
                task.player().addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.BLINDNESS, Integer.MAX_VALUE, 0, false, false));
                BLINDNESS_TASKS.remove(i);
            } else BLINDNESS_TASKS.set(i, new BlindnessTask(task.player(), task.ticks() - 1));
        }
        for (int i = PENDING.size() - 1; i >= 0; i--) {
            Pending pending = PENDING.get(i);
            if (pending.ticks() <= 1) { place(pending.player()); PENDING.remove(i); }
            else PENDING.set(i, new Pending(pending.player(), pending.ticks() - 1));
        }
    }

    private static void clearDialogueEffects(ServerPlayer player) {
        player.removeEffect(MobEffects.DARKNESS);
        player.removeEffect(MobEffects.BLINDNESS);
        BLINDNESS_TASKS.removeIf(task -> task.player() == player);
    }

    private static void place(ServerPlayer player) {
        ServerLevel level = player.getLevel(); Direction facing = player.getDirection().getOpposite();
        BlockPos position = player.blockPosition().relative(player.getDirection(), 6);
        position = new BlockPos(position.getX(), level.getHeight(Heightmap.Types.MOTION_BLOCKING, position.getX(), position.getZ()), position.getZ());
        level.setBlock(position, PocketDoorMod.POCKET_DOOR.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, facing), 3);
        level.sendParticles(ParticleTypes.FLAME, position.getX() + .5, position.getY() + 1, position.getZ() + .5, 12, .3, .5, .3, .02);
    }
    public static boolean isFinished(ServerPlayer player) { return PocketDoorSavedData.get(player.getLevel()).isFinished(player.getUUID()); }
    public static void sendStartDialogue(ServerPlayer player) { ServerPlayNetworking.send(player, START_DIALOGUE, PacketByteBufs.empty()); }
    private ModMessages() {}
}
