package com.example.keyconflictpicker.core;

import net.minecraft.client.KeyMapping;

import java.lang.reflect.Field;

/**
 * 对 {@link KeyMapping} 私有状态的受控访问。
 * NeoForge 运行时使用 Mojang 官方映射，字段名在开发环境与生产环境一致。
 */
final class KeyMappingAccess {

    private static final Field CLICK_COUNT = findField("clickCount");
    private static final Field IS_DOWN = findField("isDown");

    private KeyMappingAccess() {
    }

    private static Field findField(String name) {
        try {
            Field field = KeyMapping.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (Throwable t) {
            return null;
        }
    }

    /** 注入一次点击（等效于一次真实按下）。 */
    static void addClick(KeyMapping mapping) {
        if (CLICK_COUNT != null) {
            try {
                CLICK_COUNT.setInt(mapping, CLICK_COUNT.getInt(mapping) + 1);
            } catch (Throwable ignored) {
            }
        }
    }

    /** 强制设置按下状态，用于模拟按下脉冲与接管期间的抑制。 */
    static void setDown(KeyMapping mapping, boolean down) {
        if (IS_DOWN != null) {
            try {
                IS_DOWN.setBoolean(mapping, down);
            } catch (Throwable ignored) {
            }
        }
    }

    /** 排空该绑定已累积的点击，阻止其在接管期间被误触发。 */
    static void drain(KeyMapping mapping) {
        while (mapping.consumeClick()) {
            // 排空
        }
    }
}
