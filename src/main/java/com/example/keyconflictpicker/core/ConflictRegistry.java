package com.example.keyconflictpicker.core;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 冲突扫描：把游戏中所有已绑定的 {@link KeyMapping} 按 (物理键, 修饰键) 分组，
 * 组内成员数大于等于 2 即视为一组冲突。
 * 键位可以随时被用户修改，因此每次按需重新扫描（开销很小）。
 */
public final class ConflictRegistry {

    /** 一组冲突：同一物理键 + 同一修饰键下的多个按键绑定。 */
    public record Group(int keyCode, KeyModifier modifier, List<KeyMapping> mappings) {

        /** 组唯一标识，用于持久化记忆。绑定集合变化时哈希自动变化，旧记忆随之失效。 */
        public String id() {
            List<String> ids = new ArrayList<>();
            for (KeyMapping mapping : mappings) {
                ids.add(mapping.getCategory() + "|" + mapping.getName());
            }
            ids.sort(String::compareTo);
            return keyCode + ":" + modifier.name() + ":" + Integer.toHexString(String.join(";", ids).hashCode());
        }

        /** 物理键的显示名，如「空格」「G」。 */
        public Component keyDisplayName() {
            return InputConstants.getKey(keyCode, 0).getDisplayName();
        }
    }

    private static volatile int lastConflictKeyCode = -1;
    private static volatile KeyModifier lastConflictModifier = KeyModifier.NONE;

    private ConflictRegistry() {
    }

    /**
     * 扫描全部冲突组。
     *
     * @param contextSensitive true 时按当前上下文（{@code KeyConflictContext.isActive()}）过滤，
     *                         供实时按键拦截使用；false 时不过滤，供指令在聊天界面中使用。
     */
    public static List<Group> scan(boolean contextSensitive) {
        Map<String, List<KeyMapping>> byKey = new HashMap<>();
        for (KeyMapping mapping : Minecraft.getInstance().options.keyMappings) {
            if (mapping.isUnbound()) {
                continue;
            }
            if (contextSensitive && !mapping.getKeyConflictContext().isActive()) {
                continue;
            }
            String key = mapping.getKey().getValue() + "|" + mapping.getKeyModifier().name();
            byKey.computeIfAbsent(key, k -> new ArrayList<>()).add(mapping);
        }
        List<Group> groups = new ArrayList<>();
        for (Map.Entry<String, List<KeyMapping>> entry : byKey.entrySet()) {
            List<KeyMapping> mappings = entry.getValue();
            if (mappings.size() < 2) {
                continue;
            }
            // 纯基础按键（移动/跳跃等）之间的冲突不参与弹窗拦截
            if (mappings.stream().allMatch(VanillaKeys::isBase)) {
                continue;
            }
            String[] parts = entry.getKey().split("\\|");
            groups.add(new Group(Integer.parseInt(parts[0]), KeyModifier.valueOf(parts[1]), List.copyOf(mappings)));
        }
        return groups;
    }

    /** 按物理键查找冲突组（匹配当前修饰键）。 */
    public static Optional<Group> groupFor(int keyCode, boolean contextSensitive) {
        KeyModifier modifier = currentModifier();
        return scan(contextSensitive).stream()
                .filter(group -> group.keyCode() == keyCode && group.modifier() == modifier)
                .findFirst();
    }

    /** 按物理键查找冲突组（忽略修饰键，供指令使用）。 */
    public static Optional<Group> groupForAnyModifier(int keyCode, boolean contextSensitive) {
        return scan(contextSensitive).stream()
                .filter(group -> group.keyCode() == keyCode)
                .findFirst();
    }

    /** 当前按下的修饰键。 */
    public static KeyModifier currentModifier() {
        long window = Minecraft.getInstance().getWindow().getWindow();
        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL)) {
            return KeyModifier.CONTROL;
        }
        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT)) {
            return KeyModifier.SHIFT;
        }
        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_ALT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_ALT)) {
            return KeyModifier.ALT;
        }
        return KeyModifier.NONE;
    }

    /** 记录最近一次冲突，供 /kcp pick last 使用。 */
    public static void markLast(Group group) {
        lastConflictKeyCode = group.keyCode();
        lastConflictModifier = group.modifier();
    }

    public static Optional<Group> lastGroup() {
        int keyCode = lastConflictKeyCode;
        if (keyCode == -1) {
            return Optional.empty();
        }
        return scan(false).stream()
                .filter(group -> group.keyCode() == keyCode && group.modifier() == lastConflictModifier)
                .findFirst();
    }
}
