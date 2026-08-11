package com.example.keyconflictpicker.core;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.function.Supplier;

/**
 * 延迟打开界面：把 {@link Minecraft#setScreen} 推迟到下一个客户端 tick。
 * 这样从聊天框（客户端指令）或按键事件里触发时，不会被聊天界面关闭时的
 * {@code setScreen(null)} 覆盖。
 */
final class PendingScreens {

    private static Supplier<Screen> pending;

    private PendingScreens() {
    }

    static synchronized void open(Supplier<Screen> screen) {
        pending = screen;
    }

    static synchronized void tick() {
        if (pending != null) {
            Supplier<Screen> screen = pending;
            pending = null;
            Minecraft.getInstance().setScreen(screen.get());
        }
    }
}
