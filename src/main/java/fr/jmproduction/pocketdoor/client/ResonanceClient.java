package fr.jmproduction.pocketdoor.client;

import fr.jmproduction.pocketdoor.Content;
import fr.jmproduction.pocketdoor.item.PapierFoudreItem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import fr.jmproduction.pocketdoor.network.ModMessages;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public final class ResonanceClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MenuScreens.register(Content.RESONANCE_CHAMBER_MENU, ResonanceChamberScreen::new);
        ClientPlayNetworking.registerGlobalReceiver(ModMessages.CARTOSTELLAIRE_START, (client, handler, buf, responseSender) -> {
            net.minecraft.core.BlockPos cartostellairePos = buf.readBlockPos();
            client.execute(() -> CartostellaireClient.start(cartostellairePos));
        });
        ClientTickEvents.END_CLIENT_TICK.register(CartostellaireClient::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> CartostellaireClient.reset(client)));
        EntityRendererRegistry.register(Content.PAPIER_FOUDRE_PROJECTILE, ThrownItemRenderer::new);

        // This must be registered on the client for the item-model overrides to be evaluated.
        ItemProperties.register(
                Content.PAPIER_FOUDRE,
                new ResourceLocation("pocketdoor", "charge"),
                (stack, level, entity, seed) -> entity instanceof Player player
                        ? PapierFoudreItem.charge(stack, level, player)
                        : 0.0F
        );
    }
}
