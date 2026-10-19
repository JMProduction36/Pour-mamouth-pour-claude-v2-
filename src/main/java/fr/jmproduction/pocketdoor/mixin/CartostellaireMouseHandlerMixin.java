package fr.jmproduction.pocketdoor.mixin;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;

/** Intentionally empty: the anomaly never captures mouse rotation or scrolling. */
@Mixin(MouseHandler.class)
public final class CartostellaireMouseHandlerMixin {
    private CartostellaireMouseHandlerMixin() {}
}
