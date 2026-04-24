package net.ds.trigamma.commands;

import net.ds.trigamma.explosions.ExplosionCommand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber
public class ModCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        ExplosionCommand.register(event.getDispatcher());
    }
}