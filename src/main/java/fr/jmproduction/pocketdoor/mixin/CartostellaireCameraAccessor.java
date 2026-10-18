package fr.jmproduction.pocketdoor.mixin;

import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Camera.class)
public interface CartostellaireCameraAccessor {
    @Invoker("setPosition")
    void pocketdoor$setPosition(Vec3 position);

    @Invoker("setRotation")
    void pocketdoor$setRotation(float yRot, float xRot);

    @Accessor("detached")
    void pocketdoor$setDetached(boolean detached);
}
