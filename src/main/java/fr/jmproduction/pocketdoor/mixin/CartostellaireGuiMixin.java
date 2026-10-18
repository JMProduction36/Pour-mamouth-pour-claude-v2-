package fr.jmproduction.pocketdoor.mixin;

import fr.jmproduction.pocketdoor.client.CartostellaireClient;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevents hotbar, crosshair and other in-game HUD elements during the shot. */
@Mixin(Gui.class)
public final class CartostellaireGuiMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
    private void pocketdoor$hideHud(PoseStack poseStack, float partialTick, CallbackInfo ci) {
        if (CartostellaireClient.isRunning()) ci.cancel();
    }
}
