package fr.jmproduction.pocketdoor.client;

import com.mojang.blaze3d.vertex.PoseStack;
import fr.jmproduction.pocketdoor.PocketDoorMod;
import fr.jmproduction.pocketdoor.network.ModMessages;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.RandomSource;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance.Attenuation;

import java.util.List;

public final class DialogueController extends Screen {
    private static final String[][] SPEECH = {
            {"Tiens,", "tiens,", "tiens...", "un dokipa..."},
            {"Qui je suis?"},
            {"Hmph...", "Si jeune,", "si naif."},
            {"Rendez la moi."},
            {"Rendez la moi."},
            {"Rendez la moi!!!"}
    };

    private static final String[] RESPONSES = {
            "Qui êtes vous ?",
            "Qui êtes vous?",
            "De quoi parlez vous?",
            "Qui êtes vous ?",
            "Qui êtes vous ?",
            ""
    };

    private static final int[] AUDIO_TICKS = {150, 65, 149, 71, 70, 72};

    private static final SoundEvent[] VOICE = {
            PocketDoorMod.REPLIQUE_1_SILGO,
            PocketDoorMod.REPLIQUE_2_SILGO,
            PocketDoorMod.REPLIQUE_3_SILGO,
            PocketDoorMod.REPLIQUE_4_SILGO,
            PocketDoorMod.REPLIQUE_5_SILGO,
            PocketDoorMod.REPLIQUE_6_SILGO
    };

    private static DialogueController current;
    private int line;
    private int remaining;
    private Button responseButton;

    private DialogueController() {
        super(Component.literal("Pocket Door"));
        line = 0;
        remaining = AUDIO_TICKS[0];
        responseButton = null;
    }

    public static void start() {
        if (current != null) {
            return;
        }
        current = new DialogueController();
        Minecraft.getInstance().setScreen(current);
        current.startAudio();
    }

    private void startAudio() {
        remaining = AUDIO_TICKS[line];
        Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(
                VOICE[line].getLocation(),
                SoundSource.AMBIENT,
                1.0F,
                1.0F,
                RandomSource.create(),
                false,
                0,
                Attenuation.NONE,
                0.0F,
                0.0F,
                0.0F,
                true
        ));
        updateButtonState();
    }

    @Override
    protected void init() {
        responseButton = null;
        if (line < RESPONSES.length - 1) {
            responseButton = addRenderableWidget(new Button(
                    width / 2 - 100,
                    height - 55,
                    200,
                    20,
                    Component.literal(RESPONSES[line]),
                    button -> chooseResponse()
            ));
            updateButtonState();
        }
    }

    private void updateButtonState() {
        if (responseButton != null) {
            responseButton.active = remaining <= 0;
            responseButton.visible = remaining <= 0;
        }
    }

    private void chooseResponse() {
        if (responseButton == null || !responseButton.active) {
            return;
        }
        line++;
        remaining = AUDIO_TICKS[line];
        clearWidgets();
        init();
        startAudio();
    }

    @Override
    public void tick() {
        if (remaining > 0) {
            remaining--;
            updateButtonState();
            if (remaining == 0 && line == AUDIO_TICKS.length - 1) {
                finish();
            }
        }
    }

    private void finish() {
        ClientPlayNetworking.send(ModMessages.DIALOGUE_FINISHED, PacketByteBufs.empty());
        current = null;
        Minecraft.getInstance().setScreen(null);
    }

    private String getCurrentSegment() {
        String[] segments = SPEECH[line];
        if (segments.length == 1) {
            return segments[0];
        }

        int totalWeight = 0;
        for (String segment : segments) {
            totalWeight += Math.max(1, segment.length());
        }

        int elapsed = Math.max(0, AUDIO_TICKS[line] - remaining);
        int cumulative = 0;
        for (String segment : segments) {
            cumulative += Math.max(1, (int) Math.ceil(
                    (double) AUDIO_TICKS[line] * Math.max(1, segment.length()) / totalWeight));
            if (elapsed < cumulative) {
                return segment;
            }
        }
        return segments[segments.length - 1];
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        super.render(poseStack, mouseX, mouseY, partialTick);

        Component title = Component.literal(getCurrentSegment())
                .withStyle(style -> style.withBold(true).withColor(ChatFormatting.BLUE));
        int availableWidth = Math.max(1, width - 40);
        int regionTop = 8;
        int regionBottom = Math.max(9, height - 67);
        int heightAvail = regionBottom - regionTop;
        List<FormattedCharSequence> lines = font.split(title, Math.max(1, availableWidth / 4));
        int widest = 1;
        for (FormattedCharSequence line : lines) {
            widest = Math.max(widest, font.width(line));
        }
        int blockHeight = Math.max(1, lines.size() * font.lineHeight);
        float scale = Math.min(4.0F, Math.min(
                availableWidth / (float) widest,
                heightAvail / (float) blockHeight));
        float top = Math.max(regionTop, Math.min(
                regionBottom - blockHeight * scale,
                (height - blockHeight * scale) / 2.0F - 20.0F));

        poseStack.pushPose();
        poseStack.translate(width / 2.0F, top, 0.0F);
        poseStack.scale(scale, scale, scale);
        for (int index = 0; index < lines.size(); index++) {
            FormattedCharSequence line = lines.get(index);
            font.drawShadow(poseStack, line, -font.width(line) / 2.0F,
                    index * font.lineHeight, 0x5555FF);
        }
        poseStack.popPose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}