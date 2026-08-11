package com.example.keyconflictpicker.api;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 冲突选择界面中的一个条目。
 * <p>
 * 本模组会自动把冲突键上的所有 {@link net.minecraft.client.KeyMapping} 包装为
 * {@link KeyMappingEntry}；其他模组可以通过 {@link GatherConflictEntriesEvent}
 * 追加自定义条目（无需对应 KeyMapping），或修改/隐藏已有条目。
 */
public interface ConflictEntry {

    /**
     * 条目唯一标识，用于持久化"记忆默认选择"。
     * KeyMapping 条目固定为 {@code km:<翻译键>|<分类>}。自定义条目请保证跨会话稳定。
     */
    String id();

    /** 界面中显示的名称。 */
    Component displayName();

    /** 用户选中该条目后执行的动作。 */
    void run();

    /** 可选图标，显示在条目左侧。返回 {@link ItemStack#EMPTY} 表示无图标。 */
    default ItemStack icon() {
        return ItemStack.EMPTY;
    }

    /** 可选悬停提示。 */
    default List<Component> tooltip() {
        return List.of();
    }

    /** 条目当前是否可用（不可用的条目不会出现在界面中）。 */
    default boolean isAvailable() {
        return true;
    }

    /**
     * 可选的来源模组显示名。返回 {@code null} 时由本模组自动探测。
     */
    default String ownerLabel() {
        return null;
    }
}
