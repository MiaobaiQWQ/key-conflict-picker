package com.example.keyconflictpicker.api;

import com.example.keyconflictpicker.core.ActionInvoker;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;

/**
 * 包装标准 {@link KeyMapping} 的条目。
 * 选中后通过注入点击 + 短暂按下脉冲来模拟一次真实按键，兼容 click 与 isDown 两种消费方式。
 */
public class KeyMappingEntry implements ConflictEntry {

    private final KeyMapping mapping;

    public KeyMappingEntry(KeyMapping mapping) {
        this.mapping = mapping;
    }

    public KeyMapping mapping() {
        return mapping;
    }

    @Override
    public String id() {
        return "km:" + mapping.getName() + "|" + mapping.getCategory();
    }

    @Override
    public Component displayName() {
        return Component.translatable(mapping.getName());
    }

    @Override
    public void run() {
        ActionInvoker.press(mapping);
    }

    @Override
    public String toString() {
        return "KeyMappingEntry[" + id() + "]";
    }
}
