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
    /** The sequence is intentionally delayed after returning to the five-system map. */
    private static final int INITIAL_DELAY_TICKS = 90; // 4.5 seconds
    private static final int FLUCTUATION_START = INITIAL_DELAY_TICKS;
    private static final int DETECTION_START = FLUCTUATION_START + 90; // 4.5 seconds
    private static final int TRAVEL_TICKS = 150; // 7.5 seconds; second message remains visible during travel
    private static final int ARRIVAL_TICK = DETECTION_START + TRAVEL_TICKS;

    private static boolean armed;
    private static boolean visitedSystem;
    private static boolean started;
    private static int ticks = -1;
    private static boolean returnRequested;
    private static int returnWait;
    private static boolean meteoriteSent;
    private static int targetSystem;
    private static int targetPlanet;
    private static boolean markerArrived;
    private static boolean planetTraveling;
    private static int planetTicks;
    private static final Random TARGET_RANDOM = new Random();

    /** Receives the server arm notification; it does not start the sequence by itself. */
    public static void arm() {
        armed = true;
        visitedSystem = false;
        started = false;
        ticks = -1;
        returnRequested = false;
        returnWait = 0;
        meteoriteSent = false;
        markerArrived = false;
        planetTraveling = false;
        planetTicks = 0;
        targetSystem = TARGET_RANDOM.nextInt(5);
        targetPlanet = TARGET_RANDOM.nextInt(4);
    }

    public static boolean isPending() { return armed && !started; }
    public static int phaseTicks() { return ticks; }
    public static boolean isFluctuation() { return ticks >= FLUCTUATION_START && ticks < DETECTION_START; }
    public static boolean isDetected() { return ticks >= DETECTION_START && ticks < ARRIVAL_TICK; }
    public static int targetSystem() { return targetSystem; }
    public static int targetPlanet() { return targetPlanet; }
    public static boolean markerArrived() { return markerArrived; }
    public static boolean planetTraveling() { return planetTraveling; }
    public static float markerProgress() {
        if (ticks < DETECTION_START) return 0.0F;
        return Math.min(1.0F, (ticks - DETECTION_START) / (float) TRAVEL_TICKS);
    }
    public static float planetProgress() {
        if (!planetTraveling) return 0.0F;
        return Math.min(1.0F, planetTicks / (float) TRAVEL_TICKS);
    }

    /** A system visit is required before a return to the overview can start the timer. */
    public static void onOverview(boolean overview, int selectedSystem) {
        if (!overview) visitedSystem = true;
        if (armed && visitedSystem && !started && overview) {
            started = true;
            ClientPlayNetworking.send(ModMessages.VEILLEUR_SEQUENCE_STARTED, PacketByteBufs.empty());
            armed = false; // consume the client-side arm exactly once
            ticks = 0;
            markerArrived = false;
        }
    }

    /** Starts the second leg only after the player opens the selected target system. */
    public static void onSystemSelected(int system) {
        if (markerArrived && !planetTraveling && system == targetSystem) {
            planetTraveling = true;
            planetTicks = 0;
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
        if (planetTraveling && planetTicks < TRAVEL_TICKS) planetTicks++;
        if (returnRequested && !CartostellaireClient.isRunning() && returnWait > 0 && --returnWait == 0 && !meteoriteSent) {
            meteoriteSent = true;
            ClientPlayNetworking.send(ModMessages.VEILLEUR_METEORITE, PacketByteBufs.empty());
            clearSequence();
        }
    }

    private static void play(Minecraft mc, SoundEvent sound, float pitch) {
        mc.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, 1.0F));
    }

    public static void requestReturn() {
        if (!returnRequested && (armed || started)) {
            returnRequested = true;
            returnWait = 30;
            armed = false;
            started = false;
            ticks = -1;
            markerArrived = true;
        }
    }

    private static void clearSequence() {
        armed = false;
        visitedSystem = false;
        started = false;
        ticks = -1;
        returnRequested = false;
        returnWait = 0;
        markerArrived = true;
        planetTraveling = false;
        planetTicks = 0;
    }

    public static void reset() { clearSequence(); meteoriteSent = false; }

    public static Component message() {
        // The atmospheric message belongs to the planet leg, not to the system-marker arrival.
        // Keep it hidden until the red square is close to the selected planet.
        if (planetTraveling && planetProgress() >= 0.90F) {
            return Component.literal("Entrée de l'anomalie dans l'atmosphère de la planète " + planetName());
        }
        if (isDetected()) return Component.literal("Anomalie détectée");
        if (isFluctuation()) return Component.literal("Fluctuation détectée");
        return Component.empty();
    }
    private static String systemName() { return CartostellaireScreen.systemName(targetSystem); }
    private static String planetName() { return CartostellaireScreen.planetName(targetSystem, targetPlanet); }
    private AnomalyController() {}
}
