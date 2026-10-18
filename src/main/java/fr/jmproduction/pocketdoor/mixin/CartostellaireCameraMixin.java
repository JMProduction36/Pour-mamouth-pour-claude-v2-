package fr.jmproduction.pocketdoor.mixin;

import fr.jmproduction.pocketdoor.client.CartostellaireClient;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies the cinematic after vanilla Camera.setup, so vanilla cannot overwrite it. */
@Mixin(Camera.class)
public final class CartostellaireCameraMixin {
    @Inject(method = "setup", at = @At("TAIL"), require = 0)
    private void pocketdoor$camera(BlockGetter level, Entity entity, boolean detached, boolean mirror,
                                   float partialTick, CallbackInfo ci) {
        Vec3 position = CartostellaireClient.cameraPosition(partialTick);
        if (position == null) return;
        CartostellaireCameraAccessor camera = (CartostellaireCameraAccessor) (Object) this;
        camera.pocketdoor$setDetached(true);
        camera.pocketdoor$setPosition(position);
        camera.pocketdoor$setRotation(CartostellaireClient.cameraYaw(),
                CartostellaireClient.cameraPitch(partialTick));
    }
}
