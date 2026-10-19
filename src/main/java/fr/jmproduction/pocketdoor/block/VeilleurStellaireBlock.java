package fr.jmproduction.pocketdoor.block;

import fr.jmproduction.pocketdoor.network.ModMessages;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;

/** Powered anomaly detector. The server stores arming on the player's persistent NBT. */
public final class VeilleurStellaireBlock extends Block {
    public VeilleurStellaireBlock(Properties properties) { super(properties); registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.POWERED, false)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(BlockStateProperties.POWERED); }
    @Override public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) { if (!level.isClientSide) updatePower(state, level, pos); super.onPlace(state, level, pos, old, moved); }
    @Override public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moved) { if (!level.isClientSide) updatePower(state, level, pos); super.neighborChanged(state, level, pos, block, from, moved); }
    private void updatePower(BlockState state, Level level, BlockPos pos) { boolean powered=level.hasNeighborSignal(pos); if (powered != state.getValue(BlockStateProperties.POWERED)) { level.setBlock(pos,state.setValue(BlockStateProperties.POWERED,powered),3); level.playSound(null,pos,powered?SoundEvents.CONDUIT_ACTIVATE:SoundEvents.CONDUIT_DEACTIVATE,SoundSource.BLOCKS,1.0F,1.0F); } }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) { boolean powered=state.getValue(BlockStateProperties.POWERED); player.sendSystemMessage(Component.literal(powered ? "Le Veilleur stellaire est alimenté, il peut détecter les fluctuations dans l'univers" : "Le Veilleur stellaire a besoin d'alimentation pour détecter des anomalies")); if (powered) ModMessages.armWatcher((net.minecraft.server.level.ServerPlayer) player, pos); }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
