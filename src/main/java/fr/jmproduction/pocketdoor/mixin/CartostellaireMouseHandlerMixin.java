package fr.jmproduction.pocketdoor.mixin;

import fr.jmproduction.pocketdoor.client.CartostellaireClient;
import fr.jmproduction.pocketdoor.client.CartostellaireScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps mouse input from changing the player or selected hotbar slot during the shot. */
@Mixin(MouseHandler.class)
public final class CartostellaireMouseHandlerMixin {
    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true, require = 0)
    private void pocketdoor$blockRotation(double deltaX, double deltaY, CallbackInfo ci) {
        if (CartostellaireClient.isRunning() && !(Minecraft.getInstance().screen instanceof CartostellaireScreen)) ci.cancel();
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true, require = 0)
    private void pocketdoor$blockScroll(long window, double xOffset, double yOffset, CallbackInfo ci) {
        if (CartostellaireClient.isRunning() && !(Minecraft.getInstance().screen instanceof CartostellaireScreen)) ci.cancel();
    }
}
