package net.ds.trigamma.explosions;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ExplosionHandler {

    private Vec3 fibonacciSphereDirection(int index, int total) {
        double phi = Math.PI * (3.0 - Math.sqrt(5.0)); // golden angle
        double y = 1.0 - (index / (double)(total - 1)) * 2.0;
        double radiusAtY = Math.sqrt(1.0 - y * y);
        double theta = phi * index;

        double x = Math.cos(theta) * radiusAtY;
        double z = Math.sin(theta) * radiusAtY;

        return new Vec3(x, y, z).normalize();
    }


    private static void castSingleRay(ServerLevel level, double cx, double cy, double cz,
                               Vec3 dir, float radius, Set<Entity> alreadyHit) {
        float power = radius; // starts full, drains as it travels

        // step along the ray in 0.3 block increments (vanilla does this too)
        for (float dist = 0; dist < radius; dist += 0.3f) {

            double x = cx + dir.x * dist;
            double y = cy + dir.y * dist;
            double z = cz + dir.z * dist;

            AABB hitBox = new AABB(x - 0.5, y - 0.5, z - 0.5,
                    x + 0.5, y + 0.5, z + 0.5);

            List<Entity> entities = level.getEntitiesOfClass(Entity.class, hitBox);

            for (Entity entity : entities) {
                if (!alreadyHit.contains(entity)) {
                    alreadyHit.add(entity);  // mark as hit so other rays skip it
                    entity.hurt(level.damageSources().explosion(null, null), power);
                }
            }

            BlockPos pos = BlockPos.containing(x, y, z);
            BlockState state = level.getBlockState(pos);

            if (!state.isAir()) {
                // subtract resistance — getExplosionResistance returns a float
                float resistance = state.getBlock().getExplosionResistance(
                        state, level, pos, /* your Explosion object */ null
                );
                power -= (resistance + 0.3f) * 0.3f; // vanilla formula
            }

            if (power <= 0) break; // ray exhausted

            // ray survived — destroy this block
            if (!state.isAir()) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                // flag 3 = update + notify neighbors
            }
        }
    }


    public static void castExplosionRays(ServerLevel level, double cx, double cy, double cz, float radius, int resolution) {

        Set<Entity> alreadyHit = new HashSet<>();


        for (int i = 0; i < resolution; i++) {
            for (int j = 0; j < resolution; j++) {
                for (int k = 0; k < resolution; k++) {

                    // only cast from the surface of the cube
                    if (i == 0 || i == resolution - 1 ||
                            j == 0 || j == resolution - 1 ||
                            k == 0 || k == resolution - 1) {

                        // convert grid index to -1.0 to +1.0 range
                        double d0 = (float) i / (resolution - 1) * 2.0F - 1.0F;
                        double d1 = (float) j / (resolution - 1) * 2.0F - 1.0F;
                        double d2 = (float) k / (resolution - 1) * 2.0F - 1.0F;

                        // normalize into a unit direction vector
                        double dist = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
                        d0 /= dist;
                        d1 /= dist;
                        d2 /= dist;

                        Vec3 direction = new Vec3(d0, d1, d2);
                        castSingleRay(level, cx, cy, cz, direction, radius, alreadyHit);
                    }
                }
            }
        }
    }




}
