package net.ds.trigamma.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.ds.trigamma.radiation.RadiationCapability;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.ds.trigamma.radiation.RadiationSyncPacket;

import java.util.Collection;

/**
 * Adds the /entityrad command for inspecting and modifying radiation on players.
 *
 * Usage:
 *   /entityrad <targets> contaminate <extRads> <intRads>
 *   /entityrad <targets> cleanse
 *
 * Register in your server starting event:
 * <pre>{@code
 *   @SubscribeEvent
 *   public static void onRegisterCommands(RegisterCommandsEvent event) {
 *       EntityRadCommand.register(event.getDispatcher());
 *   }
 * }</pre>
 *
 * Requires operator permission level 2.
 */
public class EntityRadCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("entityrad")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("targets", EntityArgument.players())

                                // ── /entityrad <targets> contaminate <extRads> <intRads> ──
                                .then(Commands.literal("contaminate")
                                        .then(Commands.argument("extRads", FloatArgumentType.floatArg(0f))
                                                .then(Commands.argument("intRads", FloatArgumentType.floatArg(0f))
                                                        .executes(ctx -> contaminate(
                                                                ctx.getSource(),
                                                                EntityArgument.getPlayers(ctx, "targets"),
                                                                FloatArgumentType.getFloat(ctx, "extRads"),
                                                                FloatArgumentType.getFloat(ctx, "intRads")
                                                        ))
                                                )
                                        )
                                )

                                // ── /entityrad <targets> cleanse ──
                                .then(Commands.literal("cleanse")
                                        .executes(ctx -> cleanse(
                                                ctx.getSource(),
                                                EntityArgument.getPlayers(ctx, "targets")
                                        ))
                                )
                        )
        );
    }

    // ── Contaminate ───────────────────────────────────────────────────────────

    private static int contaminate(CommandSourceStack src,
                                   Collection<ServerPlayer> targets,
                                   float extRads,
                                   float intRads) {
        for (ServerPlayer player : targets) {
            RadiationCapability cap = RadiationCapability.get(player);
            cap.addExternalRads(extRads);
            cap.addInternalRads(intRads);
            sync(player, cap);

            src.sendSuccess(() -> Component.literal(
                    String.format("☢ Contaminated %s — ext: +%.1f rads (now %.1f), int: +%.1f rads (now %.1f)",
                            player.getDisplayName().getString(),
                            extRads, cap.getExternalRads(),
                            intRads, cap.getInternalRads())
            ), true);

            // Notify the target if they're not the one running the command
            if (!src.getEntity().equals(player)) {
                player.sendSystemMessage(Component.literal(
                        String.format("☢ You have been contaminated — ext: %.1f rads, int: %.1f rads",
                                cap.getExternalRads(), cap.getInternalRads())
                ));
            }
        }
        return targets.size();
    }

    // ── Cleanse ───────────────────────────────────────────────────────────────

    private static int cleanse(CommandSourceStack src,
                               Collection<ServerPlayer> targets) {
        for (ServerPlayer player : targets) {
            RadiationCapability cap = RadiationCapability.get(player);

            float oldExt = cap.getExternalRads();
            float oldInt = cap.getInternalRads();

            cap.cleanse();
            sync(player, cap);

            src.sendSuccess(() -> Component.literal(
                    String.format("☢ Cleansed %s — removed %.1f ext rads and %.1f int rads",
                            player.getDisplayName().getString(), oldExt, oldInt)
            ), true);

            if (!src.getEntity().equals(player)) {
                player.sendSystemMessage(Component.literal(
                        "☢ You have been cleansed of all radiation."
                ));
            }
        }
        return targets.size();
    }

    // ── Sync helper ───────────────────────────────────────────────────────────

    private static void sync(ServerPlayer player, RadiationCapability cap) {
        PacketDistributor.sendToPlayer(player, new RadiationSyncPacket(
                cap.getExternalRads(),
                cap.getInternalRads()
        ));
    }
}