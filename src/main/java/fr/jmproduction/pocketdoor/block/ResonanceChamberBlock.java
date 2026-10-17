package fr.jmproduction.pocketdoor.block;

import fr.jmproduction.pocketdoor.Content;
import fr.jmproduction.pocketdoor.block.entity.ResonanceChamberBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

public class ResonanceChamberBlock extends BaseEntityBlock {
    public enum Stage implements StringRepresentable {
        INACTIVE,
        ACTIVATING,
        RESONATING,
        COMPLETE;

        @Override
        public String getSerializedName() {
            return name().toLowerCase();
        }
    }

    public static final EnumProperty<Stage> STAGE = EnumProperty.create("stage", Stage.class);

    public ResonanceChamberBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(STAGE, Stage.INACTIVE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ResonanceChamberBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> blockEntityType
    ) {
        return createTickerHelper(
                blockEntityType,
                Content.RESONANCE_CHAMBER_ENTITY,
                ResonanceChamberBlockEntity::tick
        );
    }

    public static int light(BlockState state) {
        return switch (state.getValue(STAGE)) {
            case INACTIVE -> 0;
            case ACTIVATING -> 5;
            case RESONATING -> 10;
            case COMPLETE -> 15;
        };
    }

    @Override
    public InteractionResult use(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ResonanceChamberBlockEntity blockEntity) {
            player.openMenu(blockEntity);
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean movedByPiston
    ) {
        if (state.getBlock() != newState.getBlock()
                && level.getBlockEntity(pos) instanceof ResonanceChamberBlockEntity blockEntity) {
            Containers.dropContents(level, pos, blockEntity);
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(5) != 0) {
            return;
        }

        Stage stage = state.getValue(STAGE);
        if (stage == Stage.ACTIVATING || stage == Stage.RESONATING) {
            double angle = random.nextDouble() * 6.28;
            double distance = 0.65;
            level.addParticle(
                    stage == Stage.RESONATING ? ParticleTypes.ENCHANT : ParticleTypes.END_ROD,
                    pos.getX() + 0.5 + Math.cos(angle) * distance,
                    pos.getY() + 0.7 + random.nextDouble() * 0.5,
                    pos.getZ() + 0.5 + Math.sin(angle) * distance,
                    0,
                    0.01,
                    0
            );
        }
    }
}
