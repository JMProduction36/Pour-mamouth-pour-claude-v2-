package fr.jmproduction.pocketdoor.client;
import fr.jmproduction.pocketdoor.Content;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;
public final class ResonanceClient implements ClientModInitializer {
 @Override public void onInitializeClient(){ MenuScreens.register(Content.RESONANCE_CHAMBER_MENU, ResonanceChamberScreen::new); }
}
