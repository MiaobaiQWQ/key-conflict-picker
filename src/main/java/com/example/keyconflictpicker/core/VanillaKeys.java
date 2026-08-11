package com.example.keyconflictpicker.core;

import net.minecraft.client.KeyMapping;

import java.util.Set;

/**
 * 原版基础按键（移动、跳跃等核心操作）的翻译键集合。
 * 纯基础按键之间的冲突（通常由用户误设导致，且这类键需要持续按住）不参与弹窗拦截；
 * 只有当冲突组里存在基础按键以外的绑定（例如某模组按键与跳跃冲突）时才会介入。
 */
final class VanillaKeys {

    /** 基础移动与核心操作键。 */
    private static final Set<String> BASE_NAMES = Set.of(
            "key.forward", "key.back", "key.left", "key.right",
            "key.jump", "key.sneak", "key.sprint",
            "key.attack", "key.use", "key.pickItem");

    private VanillaKeys() {
    }

    /** 基础按键名：key.forward..key.right、跳跃/潜行/疾跑、攻击/使用/选择方块、快捷栏 1-9。 */
    static boolean isBase(KeyMapping mapping) {
        String name = mapping.getName();
        return BASE_NAMES.contains(name) || name.startsWith("key.hotbar.");
    }
}
