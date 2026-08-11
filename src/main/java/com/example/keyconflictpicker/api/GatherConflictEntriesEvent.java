package com.example.keyconflictpicker.api;

import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.client.settings.KeyModifier;

import java.util.List;

/**
 * 构建冲突条目列表时在游戏事件总线（{@code NeoForge.EVENT_BUS}）上触发。
 * <p>
 * 其他模组可以通过监听该事件来：
 * <ul>
 *   <li>为自己的条目附加图标 / 提示文本 / 可用性判断；</li>
 *   <li>追加纯 API 条目（无对应 KeyMapping 的自定义动作）；</li>
 *   <li>从列表中移除不想暴露的条目。</li>
 * </ul>
 * 示例：
 * <pre>{@code
 * NeoForge.EVENT_BUS.addListener((GatherConflictEntriesEvent event) -> {
 *     if (event.getKeyCode() == GLFW.GLFW_KEY_G) {
 *         event.getEntries().add(new MyCustomEntry());
 *     }
 * });
 * }</pre>
 */
public class GatherConflictEntriesEvent extends Event {

    private final int keyCode;
    private final KeyModifier keyModifier;
    private final List<ConflictEntry> entries;

    public GatherConflictEntriesEvent(int keyCode, KeyModifier keyModifier, List<ConflictEntry> entries) {
        this.keyCode = keyCode;
        this.keyModifier = keyModifier;
        this.entries = entries;
    }

    /** 发生冲突的物理键（GLFW keycode）。 */
    public int getKeyCode() {
        return keyCode;
    }

    /** 生效的修饰键。 */
    public KeyModifier getKeyModifier() {
        return keyModifier;
    }

    /** 可修改的条目列表。 */
    public List<ConflictEntry> getEntries() {
        return entries;
    }
}
