package fr.jmproduction.pocketdoor.network;

import fr.jmproduction.pocketdoor.PocketDoorMod;
import fr.jmproduction.pocketdoor.data.PocketDoorSavedData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Server networking plus the server-authoritative meteorite event. */
public final class ModMessages {
    public static final ResourceLocation OPEN_POCKET_DOOR = id("open_pocket_door");
    public static final ResourceLocation START_DIALOGUE = id("start_dialogue");
    public static final ResourceLocation DIALOGUE_FINISHED = id("dialogue_finished");
    public static final ResourceLocation SUMMON_METEORITE = id("summon_meteorite");
    public static final ResourceLocation CARTOSTELLAIRE_START = id("cartostellaire_start");
    public static final ResourceLocation VEILLEUR_ANOMALY = id("veilleur_anomaly");
    public static final ResourceLocation VEILLEUR_METEORITE = id("veilleur_meteorite");
    public static final ResourceLocation VEILLEUR_METEORITE_FINISHED = id("veilleur_meteorite_finished");
    private static final Map<UUID, Integer> COOLDOWNS = new HashMap<>();
    private static final Set<UUID> ARMED_PLAYERS = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Long> ARMED_POS = new ConcurrentHashMap<>();
    private static final Set<UUID> ACTIVE_DIALOGUES = new HashSet<>();
    private static final List<Meteorite> METEORITES = new ArrayList<>();
    private static final List<ImpactAnimation> IMPACTS = new ArrayList<>();
    private static final List<Pending> PENDING = new ArrayList<>();
    private record Pending(ServerPlayer player, int ticks) {}

    private static final class Meteorite {
        final ServerPlayer player; final ServerLevel level;
        final int x, z, groundY, radius, startX, startZ, startY, flightTicks;
        final double dx, dy, dz;
        int age;
        Meteorite(ServerPlayer p, int x, int z, int groundY, int radius, int startX, int startZ, int startY, int flightTicks) {
            this.player = p; this.level = (ServerLevel) p.getLevel(); this.x = x; this.z = z; this.groundY = groundY;
            this.radius = radius; this.startX = startX; this.startZ = startZ; this.startY = startY; this.flightTicks = flightTicks;
            this.dx = x - startX; this.dy = (groundY - radius / 2.0) - startY; this.dz = z - startZ;
        }
        double px() { return startX + dx * age / (double) flightTicks; }
        double py() { return startY + dy * age / (double) flightTicks; }
        double pz() { return startZ + dz * age / (double) flightTicks; }
    }

    private static final class ImpactAnimation {
        final ServerLevel level; final ServerPlayer focus; final double x, y, z; int age;
        ImpactAnimation(ServerLevel level, ServerPlayer focus, double x, double y, double z) { this.level = level; this.focus = focus; this.x = x; this.y = y; this.z = z; }
    }

    private static ResourceLocation id(String path) { return new ResourceLocation(PocketDoorMod.MOD_ID, path); }

    public static void registerC2SPackets() {
        ServerPlayNetworking.registerGlobalReceiver(SUMMON_METEORITE, (server, player, handler, buf, responseSender) -> server.execute(() -> { player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Météorite invoquée")); summonMeteorite(player); }));
        ServerPlayNetworking.registerGlobalReceiver(VEILLEUR_METEORITE, (server, player, handler, buf, responseSender) -> server.execute(() -> { if (isArmed(player)) { clearArmed(player); summonMeteorite(player); } }));
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
        ACTIVE_DIALOGUES.remove(id); COOLDOWNS.remove(id); PENDING.removeIf(p -> p.player().getUUID().equals(id));
        METEORITES.removeIf(m -> m.player.getUUID().equals(id));
        ARMED_PLAYERS.remove(id);
        ARMED_POS.remove(id);
    }

    private static void summonMeteorite(ServerPlayer player) {
        UUID id = player.getUUID();
        if (ACTIVE_DIALOGUES.contains(id) || COOLDOWNS.getOrDefault(id, 0) > 0 || METEORITES.stream().anyMatch(m -> m.player.getUUID().equals(id))) return;
        RandomSource random = player.getLevel().random;
        double angle = random.nextDouble() * Math.PI * 2.0;
        int distance = 90 + random.nextInt(31);
        int x = Mth.floor(player.getX() + Math.cos(angle) * distance);
        int z = Mth.floor(player.getZ() + Math.sin(angle) * distance);
        // The destination is intentionally outside the player's immediate view distance.
        // hasChunkAt(...) only tests whether it is already loaded; it does not load it.
        // The old early return therefore discarded almost every summon before METEORITES.add().
        ServerLevel level = (ServerLevel) player.getLevel();
        level.getChunkSource().getChunk(x >> 4, z >> 4, ChunkStatus.FULL, true);
        int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
        int radius = 7 + random.nextInt(4);
        // The incoming line is a random diagonal, independent of the player's look direction.
        double travelAngle = angle + (random.nextBoolean() ? Math.PI : 0.0) + (random.nextDouble() - 0.5) * 0.9;
        int offset = 112 + random.nextInt(17);
        int startX = Mth.floor(x - Math.cos(travelAngle) * offset);
        int startZ = Mth.floor(z - Math.sin(travelAngle) * offset);
        int startY = Math.min(level.getMaxBuildHeight() - 8, ground + 120);
        METEORITES.add(new Meteorite(player, x, z, ground, radius, startX, startZ, startY, 120 + random.nextInt(21)));
        COOLDOWNS.put(id, 20 * 15);
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Météorite en approche — trajectoire chargée."));
        level.playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 12.0F, 0.65F);
    }

    private static void tick(MinecraftServer server) {
        COOLDOWNS.replaceAll((id, n) -> Math.max(0, n - 1));
        for (int i = METEORITES.size() - 1; i >= 0; i--) {
            Meteorite m = METEORITES.get(i);
            if (!m.player.isAlive() || m.player.getLevel() != m.level) { METEORITES.remove(i); continue; }
            double px = m.px(), py = m.py(), pz = m.pz();
            orientPlayerTo(m.player, px, py, pz);
            flightParticles(m, px, py, pz);
            m.age++;
            if (m.age >= m.flightTicks) { impact(m); METEORITES.remove(i); }
        }
        for (int i = IMPACTS.size() - 1; i >= 0; i--) {
            ImpactAnimation effect = IMPACTS.get(i);
            if (effect.focus.isAlive() && effect.focus.getLevel() == effect.level) {
                orientPlayerTo(effect.focus, effect.x, effect.y, effect.z);
            }
            impactParticles(effect); effect.age++;
            if (effect.age > 36) IMPACTS.remove(i);
        }
        for (int i = PENDING.size() - 1; i >= 0; i--) { Pending p = PENDING.get(i); if (p.ticks() <= 1) { sendStartDialogue(p.player()); PENDING.remove(i); } else PENDING.set(i, new Pending(p.player(), p.ticks() - 1)); }
    }

    /** Sends directly to the triggering player so particles are visible even when the
     * trajectory is outside the normal server particle broadcast radius. */
    private static void sendParticles(ServerLevel level, ServerPlayer focus, ParticleOptions particle,
                                       double x, double y, double z, int count,
                                       double dx, double dy, double dz, double speed) {
        for (ServerPlayer recipient : level.players()) {
            if (recipient == focus || recipient.distanceToSqr(x, y, z) <= 256.0 * 256.0) {
                level.sendParticles(recipient, particle, true, x, y, z, count, dx, dy, dz, speed);
            }
        }
    }

    private static void flightParticles(Meteorite m, double x, double y, double z) {
        // A large, explicit particle core is used instead of a client-only visual entity:
        // it is reliable across 1.19.2 clients and is sent with overrideLimiter=true.
        Vec3 direction = new Vec3(m.dx, m.dy, m.dz).normalize();
        Vec3 side = Math.abs(direction.y) < 0.92 ? direction.cross(new Vec3(0, 1, 0)).normalize()
                : direction.cross(new Vec3(1, 0, 0)).normalize();
        Vec3 up = side.cross(direction).normalize();
        RandomSource r = m.level.random;
        // Four thick, wide rings: deliberately large while keeping each packet count modest.
        for (int ring = 0; ring < 6; ring++) {
            double radius = 3.0 + ring * 0.95;
            int points = 28 + ring * 6;
            double phase = m.age * 0.16 + ring * 0.7;
            for (int i = 0; i < points; i++) {
                double a = phase + (Math.PI * 2.0 * i / points);
                double wobble = 0.82 + r.nextDouble() * 0.30;
                double ox = side.x * Math.cos(a) * radius * wobble + up.x * Math.sin(a) * radius * wobble;
                double oy = side.y * Math.cos(a) * radius * wobble + up.y * Math.sin(a) * radius * wobble;
                double oz = side.z * Math.cos(a) * radius * wobble + up.z * Math.sin(a) * radius * wobble;
                ParticleOptions kind = (i + ring + m.age) % 9 == 0 ? ParticleTypes.LAVA
                        : ((i + m.age) % 7 == 0 ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME);
                sendParticles(m.level, m.player, kind, x + ox, y + oy, z + oz, 1, .12, .12, .12, .015);
            }
        }
        // A much larger fireball core (all individual calls stay well below 600).
        sendParticles(m.level, m.player, ParticleTypes.FLAME, x, y, z, 160, 2.4, 2.4, 2.4, .035);
        sendParticles(m.level, m.player, ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 80, 1.9, 1.9, 1.9, .028);
        sendParticles(m.level, m.player, ParticleTypes.LAVA, x, y, z, 36, 1.65, 1.65, 1.65, 0.0);
        sendParticles(m.level, m.player, ParticleTypes.EXPLOSION_EMITTER, x, y, z, m.age % 5 == 0 ? 1 : 0, 0, 0, 0, 0);
        // Long, thick tail: several points behind the moving core, not only one puff.
        for (int i = 1; i <= 8; i++) {
            double tail = i * 2.35;
            sendParticles(m.level, m.player, ParticleTypes.LARGE_SMOKE, x - direction.x * tail, y - direction.y * tail,
                    z - direction.z * tail, 6, 1.25 + i * .08, 1.25 + i * .08, 1.25 + i * .08, .022);
            if (i % 2 == 0) sendParticles(m.level, m.player, ParticleTypes.ASH, x - direction.x * tail,
                    y - direction.y * tail, z - direction.z * tail, 3, 1.5, 1.5, 1.5, .014);
        }
        if (m.age % 12 == 0) m.level.playSound(null, new BlockPos(Mth.floor(x), Mth.floor(y), Mth.floor(z)),
                SoundEvents.GHAST_SHOOT, SoundSource.HOSTILE, 8.0F, 0.7F + m.level.random.nextFloat() * 0.35F);
    }

    private static void impact(Meteorite m) {
        BlockPos center = new BlockPos(m.x, Mth.floor(m.groundY - m.radius * 0.45), m.z);
        // Keep the destination available for the impact tick too; otherwise the old
        // hasChunkAt guard could silently delete a meteorite at the end of flight.
        m.level.getChunkSource().getChunk(center.getX() >> 4, center.getZ() >> 4, ChunkStatus.FULL, true);
        carveCrater(m, center);
        buildBody(m, center);
        launchDebris(m, center);
        // The meteorite body is complete at this point. Carve a small, enclosed
        // 1x1x2 chamber at its real centre (centerY is deliberately below the
        // impact surface) and only then install the pocket door.
        placePocketDoorChamber(m, center);
        m.level.playSound(null, center, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 10.0F, 0.55F);
        m.level.playSound(null, center, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 10.0F, 0.65F);
        // Large visual blast; the gameplay explosion never alters blocks.
        sendParticles(m.level, m.player, ParticleTypes.EXPLOSION_EMITTER, center.getX() + .5, center.getY() + .5, center.getZ() + .5, 6, 2.5, 2.5, 2.5, 0);
        m.level.explode(null, center.getX() + .5, center.getY(), center.getZ() + .5, m.radius * 1.35F, false, Explosion.BlockInteraction.NONE);
        IMPACTS.add(new ImpactAnimation(m.level, m.player, center.getX() + .5, center.getY() + .5, center.getZ() + .5));
        m.player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Impact de météorite !"));
        // Explicit completion lets the client release cinematic locks immediately.
        ServerPlayNetworking.send(m.player, VEILLEUR_METEORITE_FINISHED, PacketByteBufs.empty());
    }

    private static void placePocketDoorChamber(Meteorite m, BlockPos center) {
        // The exact registered block is PocketDoorMod.POCKET_DOOR (Content.java
        // does not declare a pocket-door block). The chamber is intentionally
        // one block wide so the meteorite shell remains around it on every side.
        BlockPos lower = new BlockPos(center.getX(), center.getY(), center.getZ());
        BlockPos upper = new BlockPos(center.getX(), center.getY() + 1, center.getZ());
        if (!m.level.hasChunkAt(lower) || !m.level.hasChunkAt(upper)) return;

        // Clear both cells after buildBody(), ensuring the cavity cannot be
        // overwritten by meteorite generation.
        m.level.setBlock(lower, Blocks.AIR.defaultBlockState(), 3);
        m.level.setBlock(upper, Blocks.AIR.defaultBlockState(), 3);

        Direction facing = m.player.getDirection().getOpposite();
        BlockState door = PocketDoorMod.POCKET_DOOR.defaultBlockState();
        if (door.hasProperty(HorizontalDirectionalBlock.FACING)) {
            door = door.setValue(HorizontalDirectionalBlock.FACING, facing);
        }
        if (door.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            m.level.setBlock(lower, door.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER), 3);
            m.level.setBlock(upper, door.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER), 3);
        } else {
            // PocketDoorBlock is a single-cell block in the available sources;
            // retain one air block above it for the requested two-block cavity.
            m.level.setBlock(lower, door, 3);
        }
    }

    private static void carveCrater(Meteorite m, BlockPos center) {
        int outer = m.radius + 3;
        for (int dx = -outer; dx <= outer; dx++) for (int dz = -outer; dz <= outer; dz++) for (int dy = -2; dy <= m.radius / 2 + 2; dy++) {
            double ellipsoid = (dx * dx + dz * dz) / (double) (outer * outer) + (dy * dy) / (double) ((m.radius + 2) * (m.radius + 2));
            BlockPos pos = center.offset(dx, dy, dz);
            if (ellipsoid <= 1.0 && m.level.hasChunkAt(pos) && pos.getY() >= m.level.getMinBuildHeight()) m.level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
        RandomSource r = m.level.random;
        for (int i = 0; i < outer * outer * 2; i++) {
            int dx = r.nextInt(outer * 2 + 1) - outer, dz = r.nextInt(outer * 2 + 1) - outer;
            if (dx * dx + dz * dz <= outer * outer && Math.abs(dx * dx + dz * dz - m.radius * m.radius) < outer * 2) {
                BlockPos pos = new BlockPos(m.x + dx, m.groundY, m.z + dz);
                if (m.level.hasChunkAt(pos)) m.level.setBlock(pos, r.nextBoolean() ? Blocks.BLACKSTONE.defaultBlockState() : (r.nextBoolean() ? Blocks.MAGMA_BLOCK.defaultBlockState() : Blocks.NETHERRACK.defaultBlockState()), 3);
            }
        }
    }

    private static void launchDebris(Meteorite m, BlockPos center) {
        // Blocks are placed in air immediately above the impact and converted through the
        // public 1.19.2 FallingBlockEntity.fall(...) factory. No private constructor or
        // reflection is required; the launch positions are deliberately in the air.
        RandomSource r = m.level.random;
        BlockState[] debris = { Blocks.CRYING_OBSIDIAN.defaultBlockState(), Blocks.BASALT.defaultBlockState(),
                Blocks.BLACKSTONE.defaultBlockState(), Blocks.MAGMA_BLOCK.defaultBlockState(),
                Blocks.NETHER_GOLD_ORE.defaultBlockState() };
        int amount = 100 + r.nextInt(51);
        for (int i = 0; i < amount; i++) {
            double angle = r.nextDouble() * Math.PI * 2.0;
            double horizontal = 1.5 + r.nextDouble() * 4.0;
            double launchX = center.getX() + .5 + Math.cos(angle) * horizontal;
            double launchY = center.getY() + 2.0 + r.nextDouble() * 2.5;
            double launchZ = center.getZ() + .5 + Math.sin(angle) * horizontal;
            BlockPos launch = new BlockPos((int) Math.floor(launchX), (int) Math.floor(launchY), (int) Math.floor(launchZ));
            m.level.setBlock(launch, debris[r.nextInt(debris.length)], 3);
            FallingBlockEntity shard = FallingBlockEntity.fall(m.level, launch, m.level.getBlockState(launch));
            shard.setPos(launchX, launchY, launchZ);
            shard.setNoGravity(true);
            shard.setDeltaMovement(Math.cos(angle) * (1.8 + r.nextDouble() * 1.7),
                    1.6 + r.nextDouble() * 1.9, Math.sin(angle) * (1.8 + r.nextDouble() * 1.7));
            shard.dropItem = false;
        }
    }

    private static void buildBody(Meteorite m, BlockPos center) {
        RandomSource random = m.level.random;
        int limit = Mth.ceil(m.radius * 1.25);
        for (int dx = -limit; dx <= limit; dx++) for (int dy = -limit; dy <= limit; dy++) for (int dz = -limit; dz <= limit; dz++) {
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist > m.radius * 1.25) continue;
            double noise = valueNoise(dx, dy, dz);
            double bumps = ((hash(dx * 13 + 7, dy * 17 + 3, dz * 23 + 11) & 31) / 31.0 - .5) * .16;
            if (dist > m.radius * (0.75 + 0.35 * noise + bumps)) continue;
            BlockPos pos = center.offset(dx, dy, dz); if (!m.level.hasChunkAt(pos)) continue;
            double core = dist / Math.max(1, m.radius);
            BlockState state;
            int roll = random.nextInt(100);
            if (core < .42) state = roll < 3 ? Blocks.ANCIENT_DEBRIS.defaultBlockState() : roll < 14 ? Blocks.NETHER_GOLD_ORE.defaultBlockState() : roll < 35 ? Blocks.GILDED_BLACKSTONE.defaultBlockState() : Blocks.MAGMA_BLOCK.defaultBlockState();
            else if (roll < 20) state = Blocks.CRYING_OBSIDIAN.defaultBlockState();
            else if (roll < 36) state = Blocks.OBSIDIAN.defaultBlockState();
            else if (roll < 55) state = Blocks.BLACKSTONE.defaultBlockState();
            else if (roll < 68) state = Blocks.POLISHED_BASALT.defaultBlockState();
            else if (roll < 80) state = Blocks.BASALT.defaultBlockState();
            else if (roll < 92) state = Blocks.NETHERRACK.defaultBlockState();
            else if (roll < 97) state = Blocks.NETHER_GOLD_ORE.defaultBlockState();
            else state = Blocks.MAGMA_BLOCK.defaultBlockState();
            m.level.setBlock(pos, state, 3);
        }
    }

    private static double valueNoise(int x, int y, int z) { return (hash(x / 2, y / 2, z / 2) & 1023) / 1023.0; }
    private static int hash(int x, int y, int z) { int h = x * 73428767 ^ y * 912931 ^ z * 42317861; h = (h ^ (h >>> 13)) * 1274126177; return h ^ (h >>> 16); }

    /**
     * Keeps the triggering player looking at the moving meteorite without moving the
     * player.  The server packet is used because the meteorite trajectory is server
     * authoritative; interpolating a small angular step avoids a jarring snap while
     * still correcting the client every tick.
     */
    private static void orientPlayerTo(ServerPlayer player, double targetX, double targetY, double targetZ) {
        double dx = targetX - player.getX();
        double dy = targetY - (player.getY() + player.getEyeHeight());
        double dz = targetZ - player.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        if (horizontal < 1.0E-4 && Math.abs(dy) < 1.0E-4) return;

        float targetYaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        float targetPitch = (float) (-(Mth.atan2(dy, horizontal) * (180.0 / Math.PI)));
        float yaw = player.getYRot() + Mth.wrapDegrees(targetYaw - player.getYRot()) * 0.28F;
        float pitch = Mth.clamp(player.getXRot() + (targetPitch - player.getXRot()) * 0.28F, -90.0F, 90.0F);

        // ServerGamePacketListenerImpl.teleport changes only the view/position packet;
        // supplying the current coordinates prevents this from pulling the player.
        player.connection.teleport(player.getX(), player.getY(), player.getZ(), yaw, pitch);
    }

    private static void impactParticles(ImpactAnimation e) {
        RandomSource r = e.level.random;
        float t = Math.min(1.0F, e.age / 36.0F);
        double shell = 4.0 + t * 45.0;
        // Expanding ground shock ring, sampled as many individual points for a crisp circle.
        int ringPoints = 72 + e.age * 2;
        for (int i = 0; i < ringPoints; i++) {
            double a = Math.PI * 2.0 * i / ringPoints;
            double x = e.x + Math.cos(a) * shell, z = e.z + Math.sin(a) * shell;
            sendParticles(e.level, e.focus, i % 3 == 0 ? ParticleTypes.FLAME : ParticleTypes.LARGE_SMOKE,
                    x, e.y - .35 + r.nextDouble() * .35, z, 1, .16, .25, .16, .025);
        }
        // Several expanding hemispherical shells: only the upper half is emitted.
        for (int layer = 0; layer < 5; layer++) {
            double radius = 5.0 + t * (27.0 + layer * 6.0), yOffset = layer * 1.1;
            int points = 28 + layer * 6;
            for (int i = 0; i < points; i++) {
                double a = Math.PI * 2.0 * i / points;
                double elevation = (Math.PI * .5) * ((i % 7 + 1) / 7.0);
                double horizontal = Math.cos(elevation) * radius;
                double x = e.x + Math.cos(a) * horizontal;
                double y = e.y + yOffset + Math.sin(elevation) * radius;
                double z = e.z + Math.sin(a) * horizontal;
                sendParticles(e.level, e.focus, layer == 1 ? ParticleTypes.LAVA : ParticleTypes.FLAME,
                        x, y, z, 1, .2, .2, .2, .02);
            }
        }
        sendParticles(e.level, e.focus, ParticleTypes.EXPLOSION_EMITTER, e.x, e.y, e.z, e.age < 5 ? 5 : 0, 2.5, 2.5, 2.5, 0);
    }

    public static void armWatcher(ServerPlayer player, BlockPos pos) { ARMED_PLAYERS.add(player.getUUID()); ARMED_POS.put(player.getUUID(), pos.asLong()); ServerPlayNetworking.send(player, VEILLEUR_ANOMALY, PacketByteBufs.empty()); }
    private static boolean isArmed(ServerPlayer player) { return ARMED_PLAYERS.contains(player.getUUID()); }
    private static void clearArmed(ServerPlayer player) { ARMED_PLAYERS.remove(player.getUUID()); ARMED_POS.remove(player.getUUID()); }
    public static void beginFromDoor(ServerPlayer player) { beginSequence(player); }
    private static void beginSequence(ServerPlayer player) { if (ACTIVE_DIALOGUES.add(player.getUUID())) PENDING.add(new Pending(player, 100)); }
    public static boolean isFinished(ServerPlayer player) { return PocketDoorSavedData.get(player.getLevel()).isFinished(player.getUUID()); }
    public static void sendStartDialogue(ServerPlayer player) { ServerPlayNetworking.send(player, START_DIALOGUE, PacketByteBufs.empty()); }
    public static void sendCartostellaireStart(ServerPlayer player, BlockPos pos) { FriendlyByteBuf buf = PacketByteBufs.create(); buf.writeBlockPos(pos); ServerPlayNetworking.send(player, CARTOSTELLAIRE_START, buf); }
    private ModMessages() {}
}
