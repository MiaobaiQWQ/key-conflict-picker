package com.example.keyconflictpicker.core;

import com.example.keyconflictpicker.api.ConflictEntry;
import com.example.keyconflictpicker.api.KeyMappingEntry;
import net.minecraft.client.KeyMapping;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 动作执行器：把"用户选中的条目"转化为一次真实的按键效果。
 * <p>
 * 对于标准 {@link KeyMapping}：注入一次点击（兼容 {@code consumeClick()} 消费方式），
 * 同时施加一个短暂的按下脉冲（约 150ms，兼容 {@code isDown()} 消费方式，如跳跃）。
 */
public final class ActionInvoker {

    private static final long PULSE_DURATION_MS = 150;

    private record Pulse(KeyMapping mapping, long endTime) {
    }

    private static final Deque<Pulse> pulses = new ArrayDeque<>();

    private ActionInvoker() {
    }

    /** 执行一个条目：KeyMapping 条目走模拟按键，API 条目调用其 {@code run()}。 */
    public static void runEntry(ConflictEntry entry) {
        if (entry instanceof KeyMappingEntry keyMappingEntry) {
            press(keyMappingEntry.mapping());
        } else {
            entry.run();
        }
    }

    /** 对指定绑定模拟一次按键。 */
    public static synchronized void press(KeyMapping mapping) {
        KeyMappingAccess.addClick(mapping);
        KeyMappingAccess.setDown(mapping, true);
        pulses.addLast(new Pulse(mapping, System.currentTimeMillis() + PULSE_DURATION_MS));
    }

    /** 每个客户端 tick 调用，结束到期的按下脉冲。 */
    static synchronized void tick() {
        long now = System.currentTimeMillis();
        while (!pulses.isEmpty() && pulses.peekFirst().endTime() <= now) {
            Pulse pulse = pulses.pollFirst();
            KeyMappingAccess.setDown(pulse.mapping(), false);
        }
    }
}
