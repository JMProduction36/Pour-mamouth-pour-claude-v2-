package fr.jmproduction.pocketdoor;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import fr.jmproduction.pocketdoor.block.PocketDoorBlock;
import fr.jmproduction.pocketdoor.network.ModMessages;
import fr.jmproduction.pocketdoor.worldgen.TertariumWorldGen;

public class PocketDoorMod implements ModInitializer {
    public static final String MOD_ID = "pocketdoor";
    public static final ResourceLocation POCKET_DOOR_ID = new ResourceLocation(MOD_ID, "pocket_door");
    public static final ResourceKey<Level> OSSUAIRE = ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(MOD_ID, "ossuaire_flottant"));
    public static final Block POCKET_DOOR = Registry.register(Registry.BLOCK, POCKET_DOOR_ID, new PocketDoorBlock(BlockBehaviour.Properties.of(Material.WOOD).strength(2)));
    public static final Item POCKET_DOOR_ITEM = Registry.register(Registry.ITEM, POCKET_DOOR_ID, new BlockItem(POCKET_DOOR, new Item.Properties().tab(CreativeModeTab.TAB_DECORATIONS)));
    public static final SoundEvent AMBIANCE_SILGO = sound("ambiance_silgo");
    public static final SoundEvent REPLIQUE_1_SILGO = sound("replique_1_silgo");
    public static final SoundEvent REPLIQUE_2_SILGO = sound("replique_2_silgo");
    public static final SoundEvent REPLIQUE_3_SILGO = sound("replique_3_silgo");
    public static final SoundEvent REPLIQUE_4_SILGO = sound("replique_4_silgo");
    public static final SoundEvent REPLIQUE_5_SILGO = sound("replique_5_silgo");
    public static final SoundEvent REPLIQUE_6_SILGO = sound("replique_6_silgo");
    private static SoundEvent sound(String name) { ResourceLocation id = new ResourceLocation(MOD_ID, name); return Registry.register(Registry.SOUND_EVENT, id, new SoundEvent(id)); }
    @Override public void onInitialize() {
        Content.class.getName(); ModMessages.registerC2SPackets(); TertariumWorldGen.register();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> registerCommands(dispatcher));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ServerLevel ossuaire = server.getLevel(OSSUAIRE);
            if (ossuaire == null || ossuaire.players().isEmpty()) return;

            // En 1.19.2, le client ne prend pas l'etat meteo d'un ServerLevel
            // non-Overworld pour le rendu pluie/neige. L'Overworld porte donc
            // l'etat de pluie, puis les evenements sont emis directement aux
            // joueurs qui se trouvent dans la dimension custom.
            ServerLevel overworld = server.overworld();
            overworld.setWeatherParameters(0, 6000, true, false);

            // Re-emettre periodiquement les deux etats evite qu'un joueur qui
            // vient d'entrer, ou qui a rate un paquet, reste sans precipitation.
            if (server.getTickCount() % 20 == 0) {
                ClientboundGameEventPacket started = new ClientboundGameEventPacket(
                        ClientboundGameEventPacket.START_RAINING, 0.0F);
                ClientboundGameEventPacket rainLevel = new ClientboundGameEventPacket(
                        ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, 1.0F);
                for (ServerPlayer player : ossuaire.players()) {
                    player.connection.send(started);
                    player.connection.send(rainLevel);
                }
            }
        });
    }
    private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("pocketdoor").then(Commands.literal("tp").then(Commands.literal("ossuaire").executes(ctx -> {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            ServerLevel target = player.getServer().getLevel(OSSUAIRE);
            if (target == null) return 0;
            player.teleportTo(target, 0.5, 80.0, 0.5, player.getYRot(), player.getXRot());
            return 1;
        }))));
    }
}
