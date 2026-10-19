package fr.jmproduction.pocketdoor;

import fr.jmproduction.pocketdoor.block.ResonanceChamberBlock;


import fr.jmproduction.pocketdoor.block.CartostellaireBlock;
import fr.jmproduction.pocketdoor.block.VeilleurStellaireBlock;
import fr.jmproduction.pocketdoor.block.entity.ResonanceChamberBlockEntity;
import fr.jmproduction.pocketdoor.entity.PapierFoudreProjectile;
import fr.jmproduction.pocketdoor.item.PapierFoudreItem;
import fr.jmproduction.pocketdoor.menu.ResonanceChamberMenu;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;

public final class Content {
    public static final Block TERTARIUM_ORE = registerBlock(
            "tertarium_ore",
            new Block(BlockBehaviour.Properties.of(Material.STONE)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F)));

    public static final Block DEEPSLATE_TERTARIUM_ORE = registerBlock(
            "deepslate_tertarium_ore",
            new Block(BlockBehaviour.Properties.of(Material.STONE)
                    .requiresCorrectToolForDrops()
                    .strength(4.5F)));

    public static final Item TERTARIUM_ORE_ITEM = registerItem(
            "tertarium_ore",
            new BlockItem(TERTARIUM_ORE,
                    new Item.Properties().tab(CreativeModeTab.TAB_BUILDING_BLOCKS)));

    public static final Item RAW_TERTARIUM = registerItem(
            "raw_tertarium",
            new Item(new Item.Properties().tab(CreativeModeTab.TAB_MATERIALS)));

    public static final Item DEEPSLATE_TERTARIUM_ORE_ITEM = registerItem(
            "deepslate_tertarium_ore",
            new BlockItem(DEEPSLATE_TERTARIUM_ORE,
                    new Item.Properties().tab(CreativeModeTab.TAB_BUILDING_BLOCKS)));

    public static final Item PURIFIED_TERTARIUM_CRYSTAL = registerItem(
            "purified_tertarium_crystal",
            new Item(new Item.Properties().tab(CreativeModeTab.TAB_MATERIALS)));

    public static final Block CARTOSTELLAIRE = registerBlock(
            "cartostellaire",
            new CartostellaireBlock(BlockBehaviour.Properties.of(Material.STONE)
                    .requiresCorrectToolForDrops()
                    .strength(5.0F)));

    public static final Block VEILLEUR_STELLAIRE = registerBlock("veilleur_stellaire", new VeilleurStellaireBlock(BlockBehaviour.Properties.of(Material.STONE).requiresCorrectToolForDrops().strength(5.0F)));
    public static final Item VEILLEUR_STELLAIRE_ITEM = registerItem("veilleur_stellaire", new BlockItem(VEILLEUR_STELLAIRE, new Item.Properties().tab(CreativeModeTab.TAB_DECORATIONS).rarity(Rarity.EPIC)));

    public static final Item CARTOSTELLAIRE_ITEM = registerItem(
            "cartostellaire",
            new BlockItem(CARTOSTELLAIRE,
                    new Item.Properties()
                            .tab(CreativeModeTab.TAB_DECORATIONS)
                            .rarity(Rarity.EPIC)));

    public static final Block RESONANCE_CHAMBER = registerBlock(
            "resonance_chamber",
            new ResonanceChamberBlock(BlockBehaviour.Properties.of(Material.STONE)
                    .requiresCorrectToolForDrops()
                    .strength(5.0F)
                    .lightLevel(ResonanceChamberBlock::light)));

    public static final Item RESONANCE_CHAMBER_ITEM = registerItem(
            "resonance_chamber",
            new BlockItem(RESONANCE_CHAMBER,
                    new Item.Properties().tab(CreativeModeTab.TAB_DECORATIONS)));

    public static final BlockEntityType<ResonanceChamberBlockEntity> RESONANCE_CHAMBER_ENTITY =
            Registry.register(
                    Registry.BLOCK_ENTITY_TYPE,
                    id("resonance_chamber"),
                    FabricBlockEntityTypeBuilder.create(
                            ResonanceChamberBlockEntity::new,
                            RESONANCE_CHAMBER).build(null));

    public static final MenuType<ResonanceChamberMenu> RESONANCE_CHAMBER_MENU =
            Registry.register(
                    Registry.MENU,
                    id("resonance_chamber"),
                    new MenuType<>(ResonanceChamberMenu::new));

    public static final Item PAPIER_FOUDRE = registerItem(
            "papier_foudre",
            new PapierFoudreItem(new Item.Properties()
                    .stacksTo(16)
                    .rarity(Rarity.RARE)
                    .tab(CreativeModeTab.TAB_MISC)));

    public static final EntityType<PapierFoudreProjectile> PAPIER_FOUDRE_PROJECTILE =
            Registry.register(
                    Registry.ENTITY_TYPE,
                    id("papier_foudre_projectile"),
                    FabricEntityTypeBuilder.<PapierFoudreProjectile>create(
                            MobCategory.MISC,
                            (type, level) -> new PapierFoudreProjectile(type, level))
                            .dimensions(EntityDimensions.fixed(0.25F, 0.25F))
                            .trackRangeBlocks(4)
                            .trackedUpdateRate(10)
                            .build());

    private static Block registerBlock(String name, Block block) {
        return Registry.register(Registry.BLOCK, id(name), block);
    }

    private static Item registerItem(String name, Item item) {
        return Registry.register(Registry.ITEM, id(name), item);
    }

    public static ResourceLocation id(String name) {
        return new ResourceLocation(PocketDoorMod.MOD_ID, name);
    }

    private Content() {
    }
}
