package fr.jmproduction.pocketdoor.client;

import com.mojang.blaze3d.vertex.PoseStack;
import fr.jmproduction.pocketdoor.PocketDoorMod;
import fr.jmproduction.pocketdoor.network.ModMessages;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.client.resources.sounds.SoundInstance.Attenuation;

public final class DialogueController extends Screen {
 private static final String[] SPEECH={"Tiens, tiens, tiens... un dokipa...","Qui je suis?","Hmph... Si jeune, si naif.","Rendez la moi.","Rendez la moi.","Rendez la moi!!!"};
 private static final String[] RESPONSES={"Qui êtes vous ?","Qui êtes vous?","De quoi parlez vous?","Qui êtes vous ?","Qui êtes vous ?",""};
 private static final int[] AUDIO_TICKS={150,65,149,71,70,72};
 private static final SoundEvent[] VOICE={PocketDoorMod.REPLIQUE_1_SILGO,PocketDoorMod.REPLIQUE_2_SILGO,PocketDoorMod.REPLIQUE_3_SILGO,PocketDoorMod.REPLIQUE_4_SILGO,PocketDoorMod.REPLIQUE_5_SILGO,PocketDoorMod.REPLIQUE_6_SILGO};
 private static DialogueController current; private int line,audioTicksRemaining; private Button responseButton;
 private DialogueController(){super(Component.literal("Pocket Door"));}
 public static void start(){if(current!=null)return; current=new DialogueController(); Minecraft.getInstance().setScreen(current); current.startAudio();}
 private void startAudio(){audioTicksRemaining=AUDIO_TICKS[line]; Minecraft mc=Minecraft.getInstance(); mc.gui.setTimes(0,audioTicksRemaining,0); mc.gui.setTitle(Component.literal(SPEECH[line]).withStyle(s->s.withBold(true).withColor(ChatFormatting.BLUE))); mc.getSoundManager().play(new SimpleSoundInstance(VOICE[line].getLocation(),SoundSource.AMBIENT,1F,1F,RandomSource.create(),false,0,Attenuation.NONE,0F,0F,0F,true)); updateButtonState();}
 @Override protected void init(){responseButton=null; if(line<RESPONSES.length-1){responseButton=addRenderableWidget(new Button(width/2-100,height-55,200,20,Component.literal(RESPONSES[line]),b->chooseResponse())); updateButtonState();}}
 private void updateButtonState(){if(responseButton!=null)responseButton.active=audioTicksRemaining<=0;}
 private void chooseResponse(){if(audioTicksRemaining>0||line>=RESPONSES.length-1)return; line++; clearWidgets(); init(); startAudio();}
 @Override public void tick(){if(audioTicksRemaining>0){audioTicksRemaining--;updateButtonState();if(audioTicksRemaining==0&&line==SPEECH.length-1)finish();}}
 private void finish(){Minecraft mc=Minecraft.getInstance();mc.gui.setTitle(Component.empty());ClientPlayNetworking.send(ModMessages.DIALOGUE_FINISHED,net.fabricmc.fabric.api.networking.v1.PacketByteBufs.empty());current=null;mc.setScreen(null);}
 @Override public void render(PoseStack p,int x,int y,float d){super.render(p,x,y,d);}
 @Override public boolean isPauseScreen(){return false;} @Override public boolean shouldCloseOnEsc(){return false;}
}
