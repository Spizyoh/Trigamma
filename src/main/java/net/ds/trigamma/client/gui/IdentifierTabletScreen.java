// net/ds/trigamma/client/gui/IdentifierTabletScreen.java
package net.ds.trigamma.client.gui;

import net.ds.trigamma.inventory.fluid.IMatter;
import net.ds.trigamma.inventory.fluid.MatterPhase;
import net.ds.trigamma.inventory.fluid.MatterRegistry;
import net.ds.trigamma.network.SelectMatterPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class IdentifierTabletScreen extends Screen {

    private static final int WINDOW_WIDTH = 236;
    private static final int WINDOW_HEIGHT = 210;

    private final InteractionHand hand;
    private MatterPhase activeTab = MatterPhase.FLUID;

    private EditBox searchBox;
    private MatterSelectionList list;

    private int guiLeft;
    private int guiTop;

    private Button fluidTabButton;
    private Button gasTabButton;

    public IdentifierTabletScreen(InteractionHand hand) {
        super(Component.translatable("gui.trigamma.identifier_tablet.title"));
        this.hand = hand;
    }

    @Override
    protected void init() {
        this.guiLeft = (this.width - WINDOW_WIDTH) / 2;
        this.guiTop = (this.height - WINDOW_HEIGHT) / 2;

        this.fluidTabButton = this.addRenderableWidget(Button.builder(
                Component.translatable("gui.trigamma.identifier_tablet.tab.fluids"),
                btn -> switchTab(MatterPhase.FLUID)
        ).bounds(guiLeft, guiTop, WINDOW_WIDTH / 2, 20).build());

        this.gasTabButton = this.addRenderableWidget(Button.builder(
                Component.translatable("gui.trigamma.identifier_tablet.tab.gases"),
                btn -> switchTab(MatterPhase.GAS)
        ).bounds(guiLeft + WINDOW_WIDTH / 2, guiTop, WINDOW_WIDTH / 2, 20).build());

        this.searchBox = new EditBox(this.font, guiLeft + 8, guiTop + 26, WINDOW_WIDTH - 16, 18,
                Component.translatable("gui.trigamma.identifier_tablet.search"));
        this.searchBox.setHint(Component.translatable("gui.trigamma.identifier_tablet.search"));
        this.searchBox.setResponder(s -> refreshList());
        this.addRenderableWidget(this.searchBox);
        this.setInitialFocus(this.searchBox);

        this.list = new MatterSelectionList(this.minecraft, WINDOW_WIDTH, WINDOW_HEIGHT - 96, guiTop + 52, 20);
        this.list.setX(guiLeft);
        this.addRenderableWidget(this.list);

        refreshList();
        updateTabStyles();
    }

    private void switchTab(MatterPhase phase) {
        this.activeTab = phase;
        this.searchBox.setValue("");
        updateTabStyles();
        refreshList();
    }

    private void updateTabStyles() {
        this.fluidTabButton.active = this.activeTab != MatterPhase.FLUID;
        this.gasTabButton.active = this.activeTab != MatterPhase.GAS;
    }

    private void refreshList() {
        String query = this.searchBox.getValue().trim().toLowerCase();

        List<IMatter> filtered = new ArrayList<>();
        for (IMatter matter : MatterRegistry.getAllMatter().values()) {
            if (matter.phase() != this.activeTab) continue;

            if (!query.isEmpty()) {
                String localizedName = I18n.get(matter.translationKey()).toLowerCase();
                boolean matchesLocalized = localizedName.contains(query);
                boolean matchesRawId = matter.id().getPath().toLowerCase().contains(query);
                if (!matchesLocalized && !matchesRawId) continue;
            }

            filtered.add(matter);
        }
        filtered.sort(Comparator.comparing(m -> I18n.get(m.translationKey())));

        this.list.setEntries(filtered);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(guiLeft, guiTop, guiLeft + WINDOW_WIDTH, guiTop + WINDOW_HEIGHT, 0xCC101010);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, guiTop - 12, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void selectMatter(IMatter matter) {
        PacketDistributor.sendToServer(new SelectMatterPayload(this.hand, matter.id()));
        this.onClose();
    }

    private class MatterSelectionList extends ObjectSelectionList<MatterEntry> {
        public MatterSelectionList(Minecraft mc, int width, int height, int top, int itemHeight) {
            super(mc, width, height, top, itemHeight);
        }

        public void setEntries(List<IMatter> matterList) {
            this.clearEntries();
            for (IMatter matter : matterList) {
                this.addEntry(new MatterEntry(matter));
            }
        }

        @Override
        public int getRowWidth() {
            return WINDOW_WIDTH - 16;
        }

        @Override
        protected int getScrollbarPosition() {
            return this.getX() + WINDOW_WIDTH - 6;
        }
    }

    private class MatterEntry extends ObjectSelectionList.Entry<MatterEntry> {
        private final IMatter matter;

        public MatterEntry(IMatter matter) {
            this.matter = matter;
        }

        @Override
        public Component getNarration() {
            return Component.translatable(matter.translationKey());
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean hovering, float partialTick) {
            if (hovering) {
                graphics.fill(left, top, left + width, top + height, 0x40FFFFFF);
            }
            int swatch = height - 6;
            graphics.fill(left + 4, top + 3, left + 4 + swatch, top + 3 + swatch, 0xFF000000 | matter.color());
            graphics.drawString(IdentifierTabletScreen.this.font,
                    Component.translatable(matter.translationKey()),
                    left + swatch + 10, top + (height - 8) / 2, 0xFFFFFF);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            IdentifierTabletScreen.this.selectMatter(matter);
            return true;
        }
    }
}