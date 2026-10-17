package fr.jmproduction.pocketdoor.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import fr.jmproduction.pocketdoor.menu.ResonanceChamberMenu;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class ResonanceChamberScreen extends AbstractContainerScreen<ResonanceChamberMenu> {
    private static final ResourceLocation TEX = new ResourceLocation("pocketdoor", "textures/gui/resonance_chamber.png");
    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 200;

    public ResonanceChamberScreen(ResonanceChamberMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = TEXTURE_WIDTH;
        imageHeight = TEXTURE_HEIGHT;
        titleLabelX = 0;
        titleLabelY = 0;
        inventoryLabelX = 0;
        inventoryLabelY = 0;
    }

    @Override
    protected void renderBg(PoseStack poseStack, float partialTick, int mouseX, int mouseY) {
        int left = (width - imageWidth) / 2;
        int top = (height - imageHeight) / 2;
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, TEX);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        GuiComponent.blit(poseStack, left, top, 0, 0, TEXTURE_WIDTH, TEXTURE_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        int max = menu.maxProgress();
        if (max > 0) {
            int filled = Math.min(29, 29 * menu.progress() / max);
            // Kept in the upper control band, clear of the altar and inventory artwork.
            GuiComponent.fill(poseStack, left + 107, top + 20, left + 136, top + 23, 0x5536d9e8);
            GuiComponent.fill(poseStack, left + 107, top + 20, left + 107 + filled, top + 23, 0xff36d9e8);
        }
    }

    @Override
    protected void renderLabels(PoseStack poseStack, int mouseX, int mouseY) {
        // The artwork contains its own labels; keep the dynamic label in the top band.
        if (menu.stage() > 0) {
            font.draw(poseStack, "RESONANCE", 101, 28, 0xff9debf2);
        }
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        renderBackground(poseStack);
        super.render(poseStack, mouseX, mouseY, partialTick);
        renderTooltip(poseStack, mouseX, mouseY);
    }
}
