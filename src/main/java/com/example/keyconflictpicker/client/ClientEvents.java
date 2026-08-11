package com.example.keyconflictpicker.client;

import com.example.keyconflictpicker.command.KcpCommand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterClientCommandsEvent;

/** 游戏总线上的客户端事件订阅。 */
@EventBusSubscriber(Dist.CLIENT)
public final class ClientEvents {

    private ClientEvents() {
    }

    @SubscribeEvent
    static void registerClientCommands(RegisterClientCommandsEvent event) {
        KcpCommand.register(event.getDispatcher());
    }
}
