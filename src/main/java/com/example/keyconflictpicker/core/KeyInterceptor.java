package com.example.keyconflictpicker.core;

import com.example.keyconflictpicker.api.ConflictEntry;
import com.example.keyconflictpicker.api.GatherConflictEntriesEvent;
import com.example.keyconflictpicker.api.KeyMappingEntry;
import com.example.keyconflictpicker.client.DebugBindings;
import com.example.keyconflictpicker.client.gui.ConflictSelectScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * 按键拦截器：
 * <ol>
 *   <li>按下存在冲突的键时进入「接管」状态，排空所有相关绑定的点击并抑制其按下状态；</li>
 *   <li>长按超过阈值：按键仍按住时直接弹出选择界面；</li>
 *   <li>短按松手：有记忆则直接执行记忆项，否则弹出选择界面。</li>
 * </ol>
 * 仅在没有界面打开（游戏内状态）时拦截，避免干扰聊天框等 GUI 内按键。
 */
@EventBusSubscriber(Dist.CLIENT)
public final class KeyInterceptor {

    private static int takenKey = -1;
    private static long pressTime;
    private static ConflictRegistry.Group takenGroup;

    private KeyInterceptor() {
    }

    /** 收集某冲突组的条目列表：KeyMapping 自动包装 + 触发扩展事件 + 过滤不可用项。 */
    public static List<ConflictEntry> entriesFor(ConflictRegistry.Group group) {
        List<ConflictEntry> entries = new ArrayList<>();
        for (KeyMapping mapping : group.mappings()) {
            entries.add(new KeyMappingEntry(mapping));
        }
        NeoForge.EVENT_BUS.post(new GatherConflictEntriesEvent(group.keyCode(), group.modifier(), entries));
        return entries.stream().filter(ConflictEntry::isAvailable).toList();
    }

    @SubscribeEvent
    static void onKey(InputEvent.Key event) {
        if (event.getAction() == GLFW.GLFW_PRESS) {
            onPress(event.getKey());
        } else if (event.getAction() == GLFW.GLFW_RELEASE) {
            onRelease(event.getKey());
        }
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Pre event) {
        ActionInvoker.tick();
        PendingScreens.tick();
        DebugBindings.tick();
        if (takenKey == -1) {
            return;
        }
        // 有界面被其他途径打开时，放弃接管
        if (Minecraft.getInstance().screen != null) {
            clearTakeover();
            return;
        }
        suppress();
        if (System.currentTimeMillis() - pressTime >= RememberedStore.holdTimeMs()) {
            ConflictRegistry.Group group = takenGroup;
            clearTakeover();
            openPicker(group);
        }
    }

    private static void onPress(int keyCode) {
        if (Minecraft.getInstance().screen != null) {
            return;
        }
        ConflictRegistry.groupFor(keyCode, true).ifPresent(group -> {
            ConflictRegistry.markLast(group);
            takenKey = keyCode;
            pressTime = System.currentTimeMillis();
            takenGroup = group;
            suppress();
        });
    }

    private static void onRelease(int keyCode) {
        if (keyCode != takenKey) {
            return;
        }
        long heldMs = System.currentTimeMillis() - pressTime;
        ConflictRegistry.Group group = takenGroup;
        clearTakeover();
        if (heldMs >= RememberedStore.holdTimeMs()) {
            // tick 中未来得及弹窗的边界情况：松手时补弹
            openPicker(group);
        } else {
            tryExecuteRemembered(group);
        }
    }

    /** 排空点击并抑制按下状态，阻止绑定在接管期间被误触发。 */
    private static void suppress() {
        if (takenGroup == null) {
            return;
        }
        for (KeyMapping mapping : takenGroup.mappings()) {
            KeyMappingAccess.drain(mapping);
            KeyMappingAccess.setDown(mapping, false);
        }
    }

    private static void clearTakeover() {
        takenKey = -1;
        takenGroup = null;
    }

    /** 短按松手：有记忆且条目仍可用则直接执行，否则弹窗。 */
    private static void tryExecuteRemembered(ConflictRegistry.Group group) {
        String entryId = RememberedStore.get(group.id());
        if (entryId != null) {
            for (ConflictEntry entry : entriesFor(group)) {
                if (entry.id().equals(entryId)) {
                    ActionInvoker.runEntry(entry);
                    return;
                }
            }
            // 记忆的条目已不存在（模组被移除等），清除旧记忆
            RememberedStore.forget(group.id());
        }
        openPicker(group);
    }

    /** 打开选择界面（延迟到下一 tick，避免被聊天关闭等覆盖）。 */
    public static void openPicker(ConflictRegistry.Group group) {
        if (group == null) {
            return;
        }
        PendingScreens.open(() -> new ConflictSelectScreen(group));
    }
}
