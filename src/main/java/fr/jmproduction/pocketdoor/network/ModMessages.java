package fr.jmproduction.pocketdoor.network;

import fr.jmproduction.pocketdoor.PocketDoorMod;
import fr.jmproduction.pocketdoor.data.PocketDoorSavedData;
import fr.jmproduction.pocketdoor.world.MeteoriteManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.*;

public final class ModMessages {
    public static final ResourceLocation OPEN_POCKET_DOOR = id("open_pocket_door");
    public static final ResourceLocation START_DIALOGUE = id("start_dialogue");
    public static final ResourceLocation DIALOGUE_FINISHED = id("dialogue_finished");
    public static final ResourceLocation SUMMON_METEORITE = id("summon_meteorite");
    private static final Map<UUID, Integer> COOLDOWNS = new HashMap<>();
    private static final Set<UUID> ACTIVE_DIALOGUES = new HashSet<>();
    private static final List<Meteorite> METEORITES = new ArrayList<>();
    private static final List<Pending> PENDING = new ArrayList<>();
    private record Pending(ServerPlayer player, int ticks) {}
    private static final class Meteorite {
        final ServerPlayer player; final ServerLevel level; final int x, z, groundY, radius;
        int centerY; final Map<BlockPos, BlockState> originals = new HashMap<>(); final Set<BlockPos> placed = new HashSet<>();
        Meteorite(ServerPlayer p, int x, int z, int groundY, int radius, int centerY) { this.player=p; this.level=p.getLevel(); this.x=x; this.z=z; this.groundY=groundY; this.radius=radius; this.centerY=centerY; }
    }
    private static ResourceLocation id(String path) { return new ResourceLocation(PocketDoorMod.MOD_ID, path); }

    public static void registerC2SPackets() {
        ServerPlayNetworking.registerGlobalReceiver(SUMMON_METEORITE, (server, player, handler, buf, responseSender) -> server.execute(() -> MeteoriteManager.summon(player)));
        MeteoriteManager.register();
        ServerPlayNetworking.registerGlobalReceiver(OPEN_POCKET_DOOR, (server, player, handler, buf, responseSender) -> server.execute(() -> beginSequence(player)));
        ServerPlayNetworking.registerGlobalReceiver(DIALOGUE_FINISHED, (server, player, handler, buf, responseSender) -> server.execute(() -> {
            PocketDoorSavedData.get(player.getLevel()).markFinished(player.getUUID());
            player.removeEffect(net.minecraft.world.effect.MobEffects.DARKNESS); player.removeEffect(net.minecraft.world.effect.MobEffects.BLINDNESS);
            ACTIVE_DIALOGUES.remove(player.getUUID());
        }));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> server.execute(() -> cleanup(handler.getPlayer().getUUID())));
        ServerTickEvents.END_SERVER_TICK.register(ModMessages::tick);
    }

    private static void cleanup(UUID id) {
        ACTIVE_DIALOGUES.remove(id); COOLDOWNS.remove(id); PENDING.removeIf(p -> p.player().getUUID().equals(id)); MeteoriteManager.disconnect(id);
        for (int i = METEORITES.size() - 1; i >= 0; i--) if (METEORITES.get(i).player.getUUID().equals(id)) { restore(METEORITES.remove(i)); }
    }
    private static void legacySummonMeteorite(ServerPlayer player) {
        UUID id = player.getUUID();
        if (ACTIVE_DIALOGUES.contains(id) || COOLDOWNS.getOrDefault(id, 0) > 0 || METEORITES.stream().anyMatch(m -> m.player.getUUID().equals(id))) return;
        Direction look = player.getDirection(); int x = player.blockPosition().getX() + look.getStepX() * 20; int z = player.blockPosition().getZ() + look.getStepZ() * 20;
        int ground = player.getLevel().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
        Meteorite m = new Meteorite(player, x, z, ground, 7, ground + 7 + 70); COOLDOWNS.put(id, 20 * 15); place(m); METEORITES.add(m);
    }
    private static void tick(MinecraftServer server) {
        COOLDOWNS.replaceAll((id, n) -> Math.max(0, n - 1));
        for (int i = METEORITES.size() - 1; i >= 0; i--) { Meteorite m = METEORITES.get(i); int next = m.centerY - 1;
            if (!loaded(m, next)) continue;
            if (blocked(m, next)) { restore(m); impact(m); METEORITES.remove(i); }
            else if (next <= m.groundY + m.radius) { restore(m); impact(m); METEORITES.remove(i); }
            else { restore(m); m.centerY = next; place(m); }
        }
        for (int i=PENDING.size()-1;i>=0;i--) { Pending p=PENDING.get(i); if (p.ticks()<=1) { sendStartDialogue(p.player()); PENDING.remove(i); } else PENDING.set(i,new Pending(p.player(),p.ticks()-1)); }
    }
    private static boolean loaded(Meteorite m, int centerY) { return m.level.hasChunkAt(new BlockPos(m.x, centerY, m.z)); }
    private static boolean blocked(Meteorite m, int centerY) { int bottom=centerY-m.radius-1; BlockPos p=new BlockPos(m.x,bottom,m.z); BlockState state = m.level.getBlockState(p); return !state.isAir(); }
    private static void place(Meteorite m) { for(int dx=-m.radius;dx<=m.radius;dx++) for(int dy=-m.radius;dy<=m.radius;dy++) for(int dz=-m.radius;dz<=m.radius;dz++) if(dx*dx+dy*dy+dz*dz<=m.radius*m.radius) { BlockPos p=new BlockPos(m.x+dx,m.centerY+dy,m.z+dz); if(!m.level.hasChunkAt(p)) continue; m.originals.putIfAbsent(p,m.level.getBlockState(p)); int d=dx*dx+dy*dy+dz*dz; m.level.setBlock(p,d<=4?Blocks.OBSIDIAN.defaultBlockState():d<=24?Blocks.MAGMA_BLOCK.defaultBlockState():Blocks.BLACKSTONE.defaultBlockState(),3); m.placed.add(p); } }
    private static void restore(Meteorite m) { for(Map.Entry<BlockPos,BlockState> e:m.originals.entrySet()) if(m.level.hasChunkAt(e.getKey())) m.level.setBlock(e.getKey(),e.getValue(),3); m.originals.clear(); m.placed.clear(); }
    private static void impact(Meteorite m) { m.centerY=m.groundY+m.radius; place(m); m.level.playSound(null,new BlockPos(m.x,m.groundY,m.z),SoundEvents.GENERIC_EXPLODE,SoundSource.BLOCKS,2.0F,0.7F); m.level.sendParticles(ParticleTypes.EXPLOSION,m.x+0.5,m.groundY+1,m.z+0.5,8,2,1,2,0.1); Direction facing=m.player.getDirection().getOpposite(); BlockPos door=new BlockPos(m.x,m.groundY+m.radius*2+1,m.z); m.level.setBlock(door,PocketDoorMod.POCKET_DOOR.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING,facing),3); }
    public static void beginFromDoor(ServerPlayer player) { beginSequence(player); }
    private static void beginSequence(ServerPlayer player) { if(ACTIVE_DIALOGUES.add(player.getUUID())) PENDING.add(new Pending(player,100)); }
    public static boolean isFinished(ServerPlayer player) { return PocketDoorSavedData.get(player.getLevel()).isFinished(player.getUUID()); }
    public static void sendStartDialogue(ServerPlayer player) { ServerPlayNetworking.send(player,START_DIALOGUE,PacketByteBufs.empty()); }
    private ModMessages() {}
}
