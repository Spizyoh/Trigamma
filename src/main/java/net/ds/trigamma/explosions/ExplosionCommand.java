package net.ds.trigamma.explosions;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerLevel;

public class ExplosionCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("trigamma")
                        .then(Commands.literal("explode")
                                .then(Commands.argument("radius", FloatArgumentType.floatArg(1f))
                                        .then(Commands.argument("resolution", IntegerArgumentType.integer(1))
                                                .executes(ctx -> execute(ctx))))
        ));
    }



    private static int execute(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();

        // grab the arguments you passed in
        float radius = FloatArgumentType.getFloat(ctx, "radius");
        int resolution = IntegerArgumentType.getInteger(ctx, "resolution");

        // grab the center from whoever ran the command
        double cx = source.getPosition().x;
        double cy = source.getPosition().y;
        double cz = source.getPosition().z;


        ExplosionHandler handler = new ExplosionHandler();

        handler.castExplosionRays(level, cx, cy, cz, radius, resolution);

        return 1; // 1 = success in Brigadier
    }

}
