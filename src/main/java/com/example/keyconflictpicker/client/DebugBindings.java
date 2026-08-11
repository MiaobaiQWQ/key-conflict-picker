package com.example.keyconflictpicker.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

/**
 * 调试绑定：设置系统属性 {@code -Dkeyconflictpicker.debugBindings=true} 后，
 * 注册一个绑定到空格（与跳跃冲突）的测试按键，方便在开发环境中验证全部流程。
 */
public final class DebugBindings {

    private static KeyMapping debugA;

    private DebugBindings() {
    }

    public static boolean enabled() {
        return Boolean.getBoolean("keyconflictpicker.debugBindings");
    }

    public static void register(RegisterKeyMappingsEvent event) {
        if (!enabled()) {
            return;
        }
        debugA = new KeyMapping(
                "key.keyconflictpicker.debug_a",
                GLFW.GLFW_KEY_SPACE,
                "key.categories.keyconflictpicker");
        event.register(debugA);
    }

    public static void tick() {
        if (debugA != null && debugA.consumeClick() && Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.displayClientMessage(
                    Component.translatable("keyconflictpicker.debug.message"), true);
        }
    }
}
