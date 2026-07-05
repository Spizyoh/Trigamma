package net.ds.trigamma.client.gui;

import net.ds.trigamma.TriGamma;
import net.ds.trigamma.inventory.gui.CustomAnvilMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class CustomAnvilScreen extends AbstractContainerScreen<CustomAnvilMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(TriGamma.MODID, "textures/gui/custom_anvil.png");

    private EditBox searchBox;
    private Button craftButton;
    private String currentSearch = "";
    private Object selectedRecipe = null;

    // Change these to match the actual resolution of your texture file!
    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 220;

    public CustomAnvilScreen(CustomAnvilMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 256;
        this.imageHeight = 220;

        // This moves the player inventory label out of the way.
        // Adjust these or set them to values outside the screen if you want to hide it completely.
        this.titleLabelX = 10;
        this.titleLabelY = 6;
        this.inventoryLabelX = 10;
        this.inventoryLabelY = this.imageHeight - 94; // Standard alignment above player slots
    }

    @Override
    protected void init() {
        super.init();

        this.searchBox = new EditBox(this.font, this.leftPos + 10, this.topPos + 18, 100, 12, Component.literal("Search..."));
        this.searchBox.setResponder(text -> {
            this.currentSearch = text;
            this.updateFilteredRecipes();
        });
        this.addWidget(this.searchBox);

        this.craftButton = Button.builder(Component.literal("Craft"), button -> this.attemptCraft())
                .bounds(this.leftPos + 140, this.topPos + 180, 60, 20)
                .build();
        this.addRenderableWidget(this.craftButton);
    }

    private void updateFilteredRecipes() {}

    private void attemptCraft() {}

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.searchBox.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Leave this empty if you want to completely hide both the Title and "Inventory" text:
        // super.renderLabels(graphics, mouseX, mouseY);

        // Or keep it enabled so the labels respect the coordinates we set in the constructor:
        super.renderLabels(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // Using the blit overload that specifies the file's total dimensions prevents cutting off
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }
}