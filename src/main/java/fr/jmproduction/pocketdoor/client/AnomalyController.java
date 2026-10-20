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
    private static final int FLUCTUATION_START = 60, DETECTION_START = 120;
    private static final int TRAVEL_TICKS = 150, ARRIVAL_TICK = DETECTION_START + TRAVEL_TICKS;
    private static final int SAFETY_TIMEOUT_TICKS = 20 * 20;
    private static boolean pending, started, returnRequested, meteoriteSent, markerArrived;
    private static int ticks = -1, returnWait, totalTicks;
    private static int targetSystem, targetPlanet;
    private static final Random TARGET_RANDOM = new Random();

    public static void arm() { pending=true; started=false; ticks=-1; returnRequested=false; returnWait=0; meteoriteSent=false; markerArrived=false; totalTicks=0; targetSystem=TARGET_RANDOM.nextInt(5); targetPlanet=TARGET_RANDOM.nextInt(4); }
    public static boolean isPending(){return pending;} public static int phaseTicks(){return ticks;}
    public static boolean isFluctuation(){return ticks>=FLUCTUATION_START&&ticks<DETECTION_START;} public static boolean isDetected(){return ticks>=DETECTION_START;}
    public static int targetSystem(){return targetSystem;} public static int targetPlanet(){return targetPlanet;} public static boolean markerArrived(){return markerArrived;}
    public static float markerProgress(){return ticks<DETECTION_START?0.0F:Math.min(1.0F,(ticks-DETECTION_START)/(float)TRAVEL_TICKS);}
    public static void onOverview(boolean overview,int selectedSystem){if(pending&&!started&&overview){started=true;ticks=0;markerArrived=false;}}
    public static void tick(Minecraft mc){
        if (++totalTicks > SAFETY_TIMEOUT_TICKS) { CartostellaireClient.forceRelease(mc); reset(); return; }
        if(started&&ticks>=0&&ticks<ARRIVAL_TICK){ticks++; if(isFluctuation()&&ticks%5==0)play(mc,SoundEvents.NOTE_BLOCK_BELL,ticks%10==0?1.85F:1.55F); else if(isDetected()&&ticks%5==0)play(mc,(ticks/5)%2==0?SoundEvents.NOTE_BLOCK_BASS:SoundEvents.NOTE_BLOCK_DIDGERIDOO,(ticks/5)%2==0?.72F:.58F); if(ticks>=ARRIVAL_TICK)markerArrived=true;}
        if(started&&ticks>=ARRIVAL_TICK)markerArrived=true;
        if(returnRequested&&!CartostellaireClient.isRunning()&&returnWait>0&&--returnWait==0&&!meteoriteSent){meteoriteSent=true;ClientPlayNetworking.send(ModMessages.VEILLEUR_METEORITE,PacketByteBufs.empty());clearSequence();}
    }
    private static void play(Minecraft mc,SoundEvent sound,float pitch){mc.getSoundManager().play(SimpleSoundInstance.forUI(sound,pitch,1.0F));}
    public static void requestReturn(){if(!returnRequested&&(pending||started)){returnRequested=true;returnWait=30;pending=false;started=false;ticks=-1;markerArrived=true;}}
    private static void clearSequence(){pending=false;started=false;ticks=-1;returnRequested=false;returnWait=0;markerArrived=true;totalTicks=0;}
    public static void reset(){clearSequence();meteoriteSent=false;}
    public static void onMeteoriteFinished(Minecraft mc){CartostellaireClient.forceRelease(mc);reset();}
    public static Component message(){if(isDetected())return Component.literal("Anomalie détectée");if(isFluctuation())return Component.literal("Fluctuation détectée");return Component.empty();}
    private AnomalyController(){}
}
