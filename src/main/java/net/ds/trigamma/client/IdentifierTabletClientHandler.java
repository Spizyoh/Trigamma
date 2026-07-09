// net/ds/trigamma/client/IdentifierTabletClientHandler.java
package net.ds.trigamma.client;

import net.ds.trigamma.client.gui.IdentifierTabletScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;

public class IdentifierTabletClientHandler {
    public static void openScreen(InteractionHand hand) {
        Minecraft.getInstance().setScreen(new IdentifierTabletScreen(hand));
    }
}