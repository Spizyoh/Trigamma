package net.ds.trigamma.client;

import net.ds.trigamma.radiation.RadiationSyncPacket;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientPayloadHandler {
    public static void handleData(final RadiationSyncPacket data, final IPayloadContext context) {
        // EnqueueWork ensures this runs on the main client thread (safe for Minecraft logic)
        context.enqueueWork(() -> {
            ClientRadiationData.setLevels(data.externalRads(), data.internalRads());
        });
    }
}