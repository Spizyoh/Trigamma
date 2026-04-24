package net.ds.trigamma.explosions;

public class ExplosionRenderData {
    public double x, y, z;
    public float radius;
    public int ticksAlive;
    public int maxTicks;

    public ExplosionRenderData(double x, double y, double z, float radius, int maxTicks) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.radius = radius;
        this.maxTicks = maxTicks;
    }
}