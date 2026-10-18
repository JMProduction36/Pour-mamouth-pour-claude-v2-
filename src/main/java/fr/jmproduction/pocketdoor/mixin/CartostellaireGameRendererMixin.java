package fr.jmproduction.pocketdoor.mixin;

import fr.jmproduction.pocketdoor.client.CartostellaireClient;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hides the first-person hand while the free camera is active. */
@Mixin(GameRenderer.class)
public final class CartostellaireGameRendererMixin {
    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true, require = 0)
    private void pocketdoor$hideFirstPersonHand(PoseStack poseStack, float partialTick, CallbackInfo ci) {
        if (CartostellaireClient.isRunning()) ci.cancel();
    }
}
