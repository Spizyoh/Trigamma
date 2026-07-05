package net.ds.trigamma.inventory.gui;
import net.ds.trigamma.inventory.ModMenus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

public class CustomAnvilMenu extends AbstractContainerMenu {
    private final Inventory playerInventory;

    // Client-side constructor
    public CustomAnvilMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory);
    }

    // Server-side constructor
    public CustomAnvilMenu(int containerId, Inventory playerInventory) {
        super(ModMenus.CUSTOM_ANVIL_MENU.get(), containerId); // .get() now works safely!
        this.playerInventory = playerInventory;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return ItemStack.EMPTY; // No specific slots to shift-click into
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
