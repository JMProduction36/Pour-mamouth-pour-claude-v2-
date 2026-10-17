package fr.jmproduction.pocketdoor;

import fr.jmproduction.pocketdoor.block.PocketDoorBlock;
import fr.jmproduction.pocketdoor.network.ModMessages;
import fr.jmproduction.pocketdoor.worldgen.TertariumWorldGen;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;

public class PocketDoorMod implements ModInitializer {
    public static final String MOD_ID = "pocketdoor";
    public static final ResourceLocation POCKET_DOOR_ID = new ResourceLocation(MOD_ID, "pocket_door");

    public static final Block POCKET_DOOR = Registry.register(
            Registry.BLOCK,
            POCKET_DOOR_ID,
            new PocketDoorBlock(BlockBehaviour.Properties.of(Material.WOOD).strength(2))
    );
    public static final Item POCKET_DOOR_ITEM = Registry.register(
            Registry.ITEM,
            POCKET_DOOR_ID,
            new BlockItem(POCKET_DOOR, new Item.Properties().tab(CreativeModeTab.TAB_DECORATIONS))
    );

    public static final SoundEvent AMBIANCE_SILGO = sound("ambiance_silgo");
    public static final SoundEvent REPLIQUE_1_SILGO = sound("replique_1_silgo");
    public static final SoundEvent REPLIQUE_2_SILGO = sound("replique_2_silgo");
    public static final SoundEvent REPLIQUE_3_SILGO = sound("replique_3_silgo");
    public static final SoundEvent REPLIQUE_4_SILGO = sound("replique_4_silgo");
    public static final SoundEvent REPLIQUE_5_SILGO = sound("replique_5_silgo");
    public static final SoundEvent REPLIQUE_6_SILGO = sound("replique_6_silgo");

    private static SoundEvent sound(String name) {
        ResourceLocation id = new ResourceLocation(MOD_ID, name);
        return Registry.register(Registry.SOUND_EVENT, id, new SoundEvent(id));
    }

    @Override
    public void onInitialize() {
        ModMessages.registerC2SPackets(); TertariumWorldGen.register();
    }
}
