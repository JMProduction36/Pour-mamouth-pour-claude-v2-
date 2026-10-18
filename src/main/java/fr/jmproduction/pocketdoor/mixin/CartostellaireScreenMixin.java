package fr.jmproduction.pocketdoor.mixin;

import fr.jmproduction.pocketdoor.client.CartostellaireClient;
import fr.jmproduction.pocketdoor.client.CartostellaireScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevents inventory or any other screen from opening during the cinematic. */
@Mixin(Minecraft.class)
public final class CartostellaireScreenMixin {
    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true, require = 0)
    private void pocketdoor$blockScreens(Screen screen, CallbackInfo ci) {
        if (CartostellaireClient.isRunning() && screen != null && !(screen instanceof CartostellaireScreen)) ci.cancel();
    }
}
