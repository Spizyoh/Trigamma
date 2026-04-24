package net.ds.trigamma.client;

/**
 * Stores radiation levels locally on the client.
 * This class is physically excluded from Dedicated Servers.
 */
public final class ClientRadiationData {
    private static float externalRads = 0f;
    private static float internalRads = 0f;

    public static void setLevels(float ext, float i) {
        externalRads = ext;
        internalRads = i;
    }

    public static float getExternalRads() { return externalRads; }
    public static float getInternalRads() { return internalRads; }

    public static float getEffectiveDose() {
        return externalRads + (internalRads * 2f);
    }
}