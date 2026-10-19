package fr.jmproduction.pocketdoor.client;

import fr.jmproduction.pocketdoor.network.ModMessages;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import java.util.Random;

/** Client-only state machine for the Veilleur stellaire anomaly. */
public final class AnomalyController {
    private static final int FLUCTUATION_START = 60;
    private static final int DETECTION_START = 120;
    private static final int TRAVEL_TICKS = 150; // 7.5 seconds at 20 TPS
    private static final int ARRIVAL_TICK = DETECTION_START + TRAVEL_TICKS;
    private static boolean pending;
    private static boolean started;
    private static int ticks = -1;
    private static boolean returnRequested;
    private static int returnWait;
    private static boolean meteoriteSent;
    private static int targetSystem;
    private static int targetPlanet;
    private static boolean markerArrived;
    private static final Random TARGET_RANDOM = new Random();

    public static void arm() {
        pending = true; started = false; ticks = -1; returnRequested = false;
        returnWait = 0; meteoriteSent = false; markerArrived = false;
        // Select both legs once, at activation time; the choice never follows the UI selection.
        targetSystem = TARGET_RANDOM.nextInt(5);
        targetPlanet = TARGET_RANDOM.nextInt(4);
    }
    public static boolean isPending() { return pending; }
    public static int phaseTicks() { return ticks; }
    public static boolean isFluctuation() { return ticks >= FLUCTUATION_START && ticks < DETECTION_START; }
    public static boolean isDetected() { return ticks >= DETECTION_START; }
    public static int targetSystem() { return targetSystem; }
    public static int targetPlanet() { return targetPlanet; }
    public static boolean markerArrived() { return markerArrived; }
    public static float markerProgress() {
        if (ticks < DETECTION_START) return 0.0F;
        return Math.min(1.0F, (ticks - DETECTION_START) / (float) TRAVEL_TICKS);
    }

    /** Called by the map once per tick; the timer starts only on the five-system overview. */
    public static void onOverview(boolean overview, int selectedSystem) {
        if (pending && !started && overview) {
            started = true; ticks = 0; markerArrived = false;
        }
    }

    public static void tick(Minecraft mc) {
        if (started && ticks >= 0 && ticks < ARRIVAL_TICK) {
            ticks++;
            if (isFluctuation() && ticks % 5 == 0) {
                play(mc, SoundEvents.NOTE_BLOCK_BELL, ticks % 10 == 0 ? 1.85F : 1.55F);
            } else if (isDetected() && ticks % 5 == 0) {
                play(mc, (ticks / 5) % 2 == 0 ? SoundEvents.NOTE_BLOCK_BASS : SoundEvents.NOTE_BLOCK_DIDGERIDOO,
                        (ticks / 5) % 2 == 0 ? 0.72F : 0.58F);
            }
            if (ticks >= ARRIVAL_TICK) markerArrived = true;
        }
        if (started && ticks >= ARRIVAL_TICK) markerArrived = true;
        if (returnRequested && !CartostellaireClient.isRunning() && returnWait > 0) {
            returnWait--;
            if (returnWait == 0 && !meteoriteSent) {
                meteoriteSent = true;
                ClientPlayNetworking.send(ModMessages.VEILLEUR_METEORITE, PacketByteBufs.empty());
                clearSequence();
            }
        }
    }

    private static void play(Minecraft mc, SoundEvent sound, float pitch) {
        mc.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, 1.0F));
    }

    /** Ends the visual/alarm sequence immediately, but retains the delayed server packet. */
    public static void requestReturn() {
        if (!returnRequested && (pending || started)) {
            returnRequested = true; returnWait = 30; // 1.5 seconds after descent has completed
            pending = false; started = false; ticks = -1; markerArrived = true;
        }
    }
    private static void clearSequence() {
        pending = false; started = false; ticks = -1; returnRequested = false; returnWait = 0; markerArrived = true;
    }
    public static void reset() { clearSequence(); meteoriteSent = false; }
    public static Component message() {
        if (isDetected()) return Component.literal("Anomalie détectée");
        if (isFluctuation()) return Component.literal("Fluctuation détectée");
        return Component.empty();
    }
    private AnomalyController() {}
}
