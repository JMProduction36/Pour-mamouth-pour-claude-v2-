package fr.jmproduction.pocketdoor.client;

import fr.jmproduction.pocketdoor.network.ModMessages;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import org.lwjgl.glfw.GLFW;

public class KeybindingsHandler implements ClientModInitializer {
    public static KeyMapping OPEN;
    public static KeyMapping METEORITE;

    @Override
    public void onInitializeClient() {
        OPEN = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.pocketdoor.open", GLFW.GLFW_KEY_U, "category.pocketdoor"));
        METEORITE = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.pocketdoor.meteorite", GLFW.GLFW_KEY_M, "category.pocketdoor"));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN.consumeClick()) ClientPlayNetworking.send(ModMessages.OPEN_POCKET_DOOR, PacketByteBufs.empty());
            while (METEORITE.consumeClick()) {
                if (client.player != null) client.player.displayClientMessage(net.minecraft.network.chat.Component.literal("[Pocket Door] M détectée : envoi du paquet météorite…"), true);
                ClientPlayNetworking.send(ModMessages.SUMMON_METEORITE, PacketByteBufs.empty());
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(ModMessages.START_DIALOGUE, (client, handler, buf, responseSender) -> client.execute(DialogueController::start));
    }
}
