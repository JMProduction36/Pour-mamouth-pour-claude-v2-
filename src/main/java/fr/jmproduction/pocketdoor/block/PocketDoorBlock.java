package fr.jmproduction.pocketdoor.block;

import fr.jmproduction.pocketdoor.network.ModMessages;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PocketDoorBlock extends HorizontalDirectionalBlock {
    private static final VoxelShape NORTH_SHAPE = Shapes.box(0, 0, 0, 1, 1, 0.625);
    private static final VoxelShape SOUTH_SHAPE = Shapes.box(0, 0, 0.375, 1, 1, 1);
    private static final VoxelShape EAST_SHAPE = Shapes.box(0.375, 0, 0, 1, 1, 1);
    private static final VoxelShape WEST_SHAPE = Shapes.box(0, 0, 0, 0.625, 1, 1);

    public PocketDoorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> SOUTH_SHAPE;
            case EAST -> EAST_SHAPE;
            case WEST -> WEST_SHAPE;
            default -> NORTH_SHAPE;
        };
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            if (!ModMessages.isFinished(serverPlayer)) {
                ModMessages.sendStartDialogue(serverPlayer);
                serverPlayer.addEffect(new MobEffectInstance(
                        MobEffects.DARKNESS, Integer.MAX_VALUE, 0, false, false));
            } else {
                player.displayClientMessage(Component.literal(
                        "Cette porte ne semble pas r\u00e9agir \u00e0 votre pr\u00e9sence pour l'instant."), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}