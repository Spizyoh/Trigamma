package net.ds.trigamma.inventory.gui;
import net.ds.trigamma.inventory.ModMenus;
import net.ds.trigamma.inventory.recipes.AnvilTier;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

public class CustomAnvilMenu extends AbstractContainerMenu {
    private final Inventory playerInventory;
    private final AnvilTier anvilTier;

    public CustomAnvilMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, buffer.readEnum(AnvilTier.class));
    }

    public CustomAnvilMenu(int containerId, Inventory playerInventory, AnvilTier anvilTier) {
        super(ModMenus.CUSTOM_ANVIL_MENU.get(), containerId);
        this.playerInventory = playerInventory;
        this.anvilTier = anvilTier;
    }

    public AnvilTier getAnvilTier() {
        return anvilTier;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}