package fr.jmproduction.pocketdoor.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/** Client-only cinematic controller. The server remains authoritative for activation. */
public final class CartostellaireClient {
    private static final int ASCENT_TICKS = 200;
    private static final int RETURN_SPEED = 8;
    private static final double MAX_HEIGHT_ABOVE_BLOCK = 400.0D;
    private static int ticks;
    private static boolean running;
    private static boolean returning;
    private static Vec3 origin = Vec3.ZERO;
    private static double maximumCameraY;
    private static float originYaw;
    private static float originPitch;
    private static boolean previousHideGui;
    private static boolean anomalyArmed;
    public static void armAnomaly() { anomalyArmed = true; }
    public static boolean consumeAnomaly() { boolean v=anomalyArmed; anomalyArmed=false; return v; }

    public static void start(BlockPos cartostellairePos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || running || cartostellairePos == null) return;
        origin = Vec3.atCenterOf(cartostellairePos);
        maximumCameraY = cartostellairePos.getY() + MAX_HEIGHT_ABOVE_BLOCK;
        originYaw = mc.player.getYRot();
        originPitch = mc.player.getXRot();
        previousHideGui = mc.options.hideGui;
        mc.options.hideGui = true;
        ticks = 0;
        returning = false;
        running = true;
        freezePlayer(mc);
    }

    public static void tick(Minecraft mc) {
        if (!running) return;
        if (mc.player == null) {
            resetState(mc);
            return;
        }
        freezePlayer(mc);
        if (!returning && ticks < ASCENT_TICKS) ticks++;
        else if (returning && ticks > 0) {
            ticks = Math.max(0, ticks - RETURN_SPEED);
            if (ticks == 0) stop(mc);
        }
        if (!returning && ticks >= ASCENT_TICKS && mc.screen == null)
            mc.setScreen(new CartostellaireScreen());
    }

    /** Reassert all movement state every tick; this also clears queued key presses. */
    private static void freezePlayer(Minecraft mc) {
        if (mc.player == null) return;
        setDown(mc.options.keyUp, false); setDown(mc.options.keyDown, false);
        setDown(mc.options.keyLeft, false); setDown(mc.options.keyRight, false);
        setDown(mc.options.keyJump, false); setDown(mc.options.keyShift, false);
        setDown(mc.options.keySprint, false); setDown(mc.options.keyAttack, false);
        setDown(mc.options.keyUse, false); setDown(mc.options.keyInventory, false);
        setDown(mc.options.keyDrop, false); setDown(mc.options.keySwapOffhand, false);
        setDown(mc.options.keyPickItem, false);
        for (KeyMapping key : mc.options.keyHotbarSlots) setDown(key, false);
        mc.player.input.forwardImpulse = 0.0F;
        mc.player.input.leftImpulse = 0.0F;
        mc.player.input.jumping = false;
        mc.player.input.shiftKeyDown = false;
        mc.player.setDeltaMovement(Vec3.ZERO);
        mc.player.setYRot(originYaw);
        mc.player.setXRot(originPitch);
        mc.player.setYHeadRot(originYaw);
        mc.player.yRotO = originYaw;
        mc.player.xRotO = originPitch;
        mc.player.noPhysics = true;
    }

    private static void setDown(KeyMapping key, boolean down) { key.setDown(down); }

    public static void beginReturn() { if (running) returning = true; }
    public static boolean isRunning() { return running; }

    private static double progress(float partialTick) {
        double value = returning ? (ticks - partialTick) / (double) ASCENT_TICKS
                : (ticks - 1.0D + partialTick) / (double) ASCENT_TICKS;
        return Math.max(0.0D, Math.min(1.0D, value));
    }
    private static double easeOutCubic(double value) { return 1.0D - Math.pow(1.0D - value, 3.0D); }
    public static Vec3 cameraPosition(float partialTick) {
        if (!running) return null;
        double eased = easeOutCubic(progress(partialTick));
        return new Vec3(origin.x, Math.min(maximumCameraY, origin.y + MAX_HEIGHT_ABOVE_BLOCK * eased), origin.z);
    }
    public static float cameraPitch(float partialTick) { return running ? 90.0F : originPitch; }
    public static float cameraYaw() { return originYaw; }

    private static void stop(Minecraft mc) { resetState(mc); }

    /** Called when the client disconnects so cinematic state cannot leak into a later world. */
    public static void reset(Minecraft mc) { resetState(mc); }

    private static void resetState(Minecraft mc) {
        running = false; returning = false; ticks = 0;
        if (mc.player != null) { mc.player.noPhysics = false; mc.player.setDeltaMovement(Vec3.ZERO); }
        mc.options.hideGui = previousHideGui;
    }
    private CartostellaireClient() {}
}
