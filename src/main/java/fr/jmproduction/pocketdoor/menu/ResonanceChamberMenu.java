package fr.jmproduction.pocketdoor.menu;

import fr.jmproduction.pocketdoor.Content;
import fr.jmproduction.pocketdoor.recipe.ResonanceRecipes;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ResonanceChamberMenu extends AbstractContainerMenu {
    // Slot coordinates are the top-left of the 16x16 item area; the rendered slot border is x-1,y-1.
    // The 18x18 rectangles are centered in the two 32px inner frame openings.
    private static final int INPUT_X = 36, OUTPUT_X = 191, MACHINE_Y = 53;
    // 9*18 = 162px, centered in the 256px artwork: (256-162)/2 = 47.
    private static final int INV_X = 47, INV_Y = 117, HOTBAR_Y = 171, SLOT_STEP = 18;
    private final Container container;
    private final ContainerData data;

    public ResonanceChamberMenu(int id, Inventory inventory) { this(id, inventory, new SimpleContainer(2), new SimpleContainerData(4)); }
    public ResonanceChamberMenu(int id, Inventory inventory, Container container, ContainerData data) {
        super(Content.RESONANCE_CHAMBER_MENU, id); this.container=container; this.data=data; checkContainerSize(container,2);
        addSlot(new Slot(container,0,INPUT_X,MACHINE_Y) { @Override public boolean mayPlace(ItemStack s) { return ResonanceRecipes.find(s)!=null; } });
        addSlot(new Slot(container,1,OUTPUT_X,MACHINE_Y) { @Override public boolean mayPlace(ItemStack s) { return false; } });
        for(int row=0;row<3;row++) for(int col=0;col<9;col++) addSlot(new Slot(inventory,col+row*9+9,INV_X+col*SLOT_STEP,INV_Y+row*SLOT_STEP));
        for(int col=0;col<9;col++) addSlot(new Slot(inventory,col,INV_X+col*SLOT_STEP,HOTBAR_Y));
        addDataSlots(data);
    }
    @Override public ItemStack quickMoveStack(Player player,int index) {
        if(index<0||index>=slots.size()||!slots.get(index).hasItem()) return ItemStack.EMPTY;
        Slot slot=slots.get(index); ItemStack result=slot.getItem().copy();
        boolean moved=index<2?moveItemStackTo(result,2,slots.size(),true):moveItemStackTo(result,0,1,false);
        if(!moved)return ItemStack.EMPTY; slot.set(result.isEmpty()?ItemStack.EMPTY:result); if(!result.isEmpty())slot.setChanged(); return result;
    }
    @Override public boolean stillValid(Player player){return container.stillValid(player);}
    public int progress(){return data.get(0);} public int maxProgress(){return data.get(1);} public int stage(){return data.get(2);}
}
