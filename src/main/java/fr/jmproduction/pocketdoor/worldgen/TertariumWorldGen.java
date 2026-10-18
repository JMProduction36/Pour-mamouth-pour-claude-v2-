package fr.jmproduction.pocketdoor.worldgen;
import fr.jmproduction.pocketdoor.PocketDoorMod;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
public final class TertariumWorldGen {
 public static final ResourceKey<PlacedFeature> PLACED=ResourceKey.create(Registry.PLACED_FEATURE_REGISTRY,new ResourceLocation(PocketDoorMod.MOD_ID,"deepslate_tertarium_ore"));
 public static void register(){BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),GenerationStep.Decoration.UNDERGROUND_ORES,PLACED);}
 private TertariumWorldGen(){}
}
