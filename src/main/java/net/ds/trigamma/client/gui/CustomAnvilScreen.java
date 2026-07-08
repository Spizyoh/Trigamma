package net.ds.trigamma.client.gui;

import net.ds.trigamma.TriGamma;
import net.ds.trigamma.inventory.gui.CustomAnvilMenu;
import net.ds.trigamma.inventory.recipes.AnvilRecipe;
import net.ds.trigamma.inventory.recipes.AnvilTier;
import net.ds.trigamma.inventory.recipes.ModAnvilRecipes;
import net.ds.trigamma.network.CraftAnvilPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.stream.Collectors;

public class CustomAnvilScreen extends AbstractContainerScreen<CustomAnvilMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(TriGamma.MODID, "textures/gui/custom_anvil.png");

    private EditBox searchBox;
    private Button craftButton;
    private String currentSearch = "";

    // Recipe management fields
    private List<AnvilRecipe> filteredRecipes;
    private AnvilRecipe selectedRecipe = null;
    private int scrollOffset = 0; // For handling longer recipe lists

    private static final int TEXTURE_WIDTH = 256;
    private static final int TEXTURE_HEIGHT = 220;

    public CustomAnvilScreen(CustomAnvilMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 256;
        this.imageHeight = 220;
        this.filteredRecipes = List.of();
    }

    @Override
    protected void init() {
        super.init();

        // Search Bar placement
        this.searchBox = new EditBox(this.font, this.leftPos + 10, this.topPos + 18, 100, 12, Component.literal(""));
        this.searchBox.setResponder(text -> {
            this.currentSearch = text;
            this.updateFilteredRecipes();
        });
        this.addWidget(this.searchBox);

        // Craft Button
        this.craftButton = Button.builder(Component.literal("Craft"), button -> this.attemptCraft())
                .bounds(this.leftPos + 180, this.topPos + 180, 60, 20)
                .build();
        this.addRenderableWidget(this.craftButton);
        this.craftButton.active = false; // Disabled until a recipe is chosen

        this.updateFilteredRecipes();
    }

    private void updateFilteredRecipes() {
        String query = currentSearch.toLowerCase();
        AnvilTier currentTier = this.menu.getAnvilTier();

        this.filteredRecipes = ModAnvilRecipes.RECIPES.stream()
                .filter(recipe -> currentTier.canCraft(recipe.requiredTier()))
                .filter(recipe -> query.isEmpty() || I18n.get(recipe.translationKey()).toLowerCase().contains(query))
                .collect(Collectors.toList());

        if (selectedRecipe != null && !filteredRecipes.contains(selectedRecipe)) {
            selectedRecipe = null;
            this.craftButton.active = false;
        }
    }

    private void attemptCraft() {
        if (selectedRecipe != null) {
            // Send the payload to the server containing the id of the active selected recipe card!
            PacketDistributor.sendToServer(new CraftAnvilPayload(selectedRecipe.id()));

            this.updateFilteredRecipes();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Detect clicking inside the yellow selection window zone
        int listStartX = this.leftPos + 12;
        int listStartY = this.topPos + 36;

        if (mouseX >= listStartX && mouseX <= listStartX + 120 && mouseY >= listStartY && mouseY <= listStartY + 150) {
            int clickedIndex = (int) ((mouseY - listStartY) / 20) + scrollOffset;
            if (clickedIndex >= 0 && clickedIndex < filteredRecipes.size()) {
                this.selectedRecipe = filteredRecipes.get(clickedIndex);
                this.craftButton.active = true;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // Allows scrolling the recipe menu with the mouse wheel
        if (scrollY < 0 && scrollOffset + 7 < filteredRecipes.size()) {
            scrollOffset++;
        } else if (scrollY > 0 && scrollOffset > 0) {
            scrollOffset--;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.searchBox.render(graphics, mouseX, mouseY, partialTick);

        // Render the selectable items text inside the yellow background canvas box
        int renderY = this.topPos + 48;
        for (int i = scrollOffset; i < Math.min(scrollOffset + 7, filteredRecipes.size()); i++) {
            AnvilRecipe recipe = filteredRecipes.get(i);
            int itemX = this.leftPos + 15;

            // Draw a subtle highlighting rectangle behind the active selection
            if (recipe == selectedRecipe) {
                graphics.fill(itemX - 2, renderY - 2, itemX + 115, renderY + 16, 0x440000FF);
            }

            // Draw the target item output icon and name
            graphics.renderFakeItem(recipe.previewOutput(), itemX, renderY);
            graphics.drawString(
                    this.font,
                    Component.translatable(recipe.translationKey()),
                    itemX + 22,
                    renderY + 4,
                    0x404040,
                    false
            );
            renderY += 20;
        }

        // Render the HBM Material Costs Side Panel
        renderMaterialCostPanel(graphics);

        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Leave this empty if you want to completely hide both the Title and "Inventory" text:
        // super.renderLabels(graphics, mouseX, mouseY);
        // Or keep it enabled so the labels respect the coordinates we set in the constructor:
        // super.renderLabels(graphics, mouseX, mouseY);
    }

    private void renderMaterialCostPanel(GuiGraphics graphics) {
        int panelX = this.leftPos + this.imageWidth;
        int panelY = this.topPos;
        int panelWidth = 140;
        int panelHeight = 150;

        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xFF0D1B2A);
        graphics.fill(panelX + 2, panelY + 2, panelX + panelWidth - 2, panelY + panelHeight - 2, 0xFF1B263B);

        if (selectedRecipe == null) {
            graphics.drawString(this.font, "No recipe selected", panelX + 8, panelY + 12, 0x8D99AE, false);
            return;
        }

        graphics.drawString(this.font, Component.translatable(selectedRecipe.translationKey()), panelX + 8, panelY + 8, 0x3A86FF, false);

        int outputY = panelY + 26;
        graphics.drawString(this.font, "Outputs:", panelX + 8, outputY, 0x8DCAF0, false);
        outputY += 14;

        for (ItemStack output : selectedRecipe.outputs()) {
            String formattedText = output.getHoverName().getString() + " x" + output.getCount();

            graphics.renderFakeItem(output, panelX + 8, outputY - 4);
            graphics.drawString(this.font, formattedText, panelX + 28, outputY, 0xE0E1DD, false);

            outputY += 18;
        }

        int costY = outputY + 8;
        graphics.drawString(this.font, "Material Costs:", panelX + 8, costY, 0x3A86FF, false);
        costY += 18;

        for (AnvilRecipe.IngredientCost ingredient : selectedRecipe.ingredients()) {
            String displayName = ingredient.item().getHoverName().getString();
            String formattedText = displayName + " x" + ingredient.count();

            boolean hasEnough = hasPlayerMaterials(ingredient.item(), ingredient.count());
            int textColor = hasEnough ? 0x00FF00 : 0xFF4D4D;

            graphics.renderFakeItem(ingredient.item(), panelX + 8, costY - 4);
            graphics.drawString(this.font, formattedText, panelX + 28, costY, textColor, false);

            costY += 18;
        }
    }

    private boolean hasPlayerMaterials(ItemStack requiredItem, int requiredAmount) {
        int count = 0;
        for (ItemStack stack : this.minecraft.player.getInventory().items) {
            if (!stack.isEmpty() && ItemStack.isSameItem(stack, requiredItem)) {
                count += stack.getCount();
            }
        }
        return count >= requiredAmount;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }
}
