package fr.jmproduction.pocketdoor.block.entity;

import fr.jmproduction.pocketdoor.Content;
import fr.jmproduction.pocketdoor.block.ResonanceChamberBlock;
import fr.jmproduction.pocketdoor.menu.ResonanceChamberMenu;
import fr.jmproduction.pocketdoor.recipe.ResonanceRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class ResonanceChamberBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {
    private final NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
    private int progress;
    private int maxProgress;
    private int stageTimer;
    private ResonanceChamberBlock.Stage stage = ResonanceChamberBlock.Stage.INACTIVE;

    private final ContainerData data = new ContainerData() {
        @Override public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> maxProgress;
                case 2 -> stage.ordinal();
                case 3 -> stageTimer;
                default -> 0;
            };
        }
        @Override public void set(int index, int value) {
            if (index == 0) progress = value;
            else if (index == 1) maxProgress = value;
            else if (index == 2) setStage(ResonanceChamberBlock.Stage.values()[Math.max(0, Math.min(3, value))]);
            else if (index == 3) stageTimer = value;
        }
        @Override public int getCount() { return 4; }
    };

    public ResonanceChamberBlockEntity(BlockPos pos, BlockState state) {
        super(Content.RESONANCE_CHAMBER_ENTITY, pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ResonanceChamberBlockEntity blockEntity) {
        if (level.isClientSide) return;
        ItemStack input = blockEntity.items.get(0);
        ResonanceRecipes.Entry recipe = ResonanceRecipes.find(input);
        if (recipe == null || !blockEntity.canAcceptOutput(recipe.output())) {
            blockEntity.resetProcess();
            return;
        }
        if (blockEntity.stage == ResonanceChamberBlock.Stage.INACTIVE) {
            blockEntity.maxProgress = recipe.ticks();
            blockEntity.stageTimer = 0;
            blockEntity.setStage(ResonanceChamberBlock.Stage.ACTIVATING);
        }
        if (blockEntity.stage == ResonanceChamberBlock.Stage.ACTIVATING) {
            if (++blockEntity.stageTimer >= 40) {
                blockEntity.stageTimer = 0;
                blockEntity.setStage(ResonanceChamberBlock.Stage.RESONATING);
            }
        } else if (blockEntity.stage == ResonanceChamberBlock.Stage.RESONATING) {
            blockEntity.maxProgress = recipe.ticks();
            if (++blockEntity.progress >= blockEntity.maxProgress) {
                blockEntity.progress = 0;
                blockEntity.stageTimer = 0;
                blockEntity.setStage(ResonanceChamberBlock.Stage.COMPLETE);
            }
        } else if (blockEntity.stage == ResonanceChamberBlock.Stage.COMPLETE
                && ++blockEntity.stageTimer >= 20) {
            blockEntity.stageTimer = 0;
            blockEntity.finish(recipe.output());
            blockEntity.setStage(ResonanceChamberBlock.Stage.INACTIVE);
        }
        blockEntity.setChanged();
    }

    private boolean canAcceptOutput(ItemStack result) {
        ItemStack output = items.get(1);
        return output.isEmpty()
                || (ItemStack.isSameItemSameTags(output, result)
                && output.getCount() + result.getCount() <= output.getMaxStackSize());
    }

    private void finish(ItemStack result) {
        if (items.get(1).isEmpty()) items.set(1, result.copy());
        else items.get(1).grow(result.getCount());
        items.get(0).shrink(1);
    }

    private void resetProcess() {
        if (stage != ResonanceChamberBlock.Stage.INACTIVE || progress != 0 || stageTimer != 0) {
            setStage(ResonanceChamberBlock.Stage.INACTIVE);
        }
        progress = 0;
        maxProgress = 0;
        stageTimer = 0;
    }

    private void setStage(ResonanceChamberBlock.Stage newStage) {
        if (stage == newStage) return;
        stage = newStage;
        if (level != null) {
            level.setBlock(worldPosition, getBlockState().setValue(ResonanceChamberBlock.STAGE, newStage), 3);
        }
    }

    public ContainerData data() { return data; }

    @Override public void load(CompoundTag tag) {
        super.load(tag);
        ContainerHelper.loadAllItems(tag, items);
        progress = tag.getInt("Progress");
        maxProgress = tag.getInt("MaxProgress");
        stageTimer = tag.getInt("StageTimer");
        stage = ResonanceChamberBlock.Stage.values()[Math.max(0, Math.min(3, tag.getInt("Stage")))];
    }

    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items);
        tag.putInt("Progress", progress);
        tag.putInt("MaxProgress", maxProgress);
        tag.putInt("Stage", stage.ordinal());
        tag.putInt("StageTimer", stageTimer);
    }

    @Override public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Nullable @Override public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? new int[]{1} : new int[]{0};
    }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot == 0 && ResonanceRecipes.find(stack) != null;
    }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == 1; }
    @Override public int getContainerSize() { return items.size(); }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) setChanged();
        return result;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) { return items.set(slot, ItemStack.EMPTY); }
    @Override public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize());
        setChanged();
    }
    @Override public boolean stillValid(Player player) {
        return level != null
                && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(
                worldPosition.getX() + 0.5,
                worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5) <= 64.0;
    }
    @Override public void clearContent() {
        items.clear();
        setChanged();
    }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == 0 && ResonanceRecipes.find(stack) != null;
    }
    @Override public int getMaxStackSize() { return 64; }
    @Override public Component getDisplayName() { return Component.translatable("container.pocketdoor.resonance_chamber"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ResonanceChamberMenu(id, inventory, this, data);
    }
}
