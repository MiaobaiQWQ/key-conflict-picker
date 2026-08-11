package com.example.keyconflictpicker.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 记忆存储 + 模组配置。
 * 文件位于 {@code config/keyconflictpicker.json}：
 * <pre>
 * {
 *   "holdTimeMs": 500,
 *   "remembered": { "&lt;冲突组 id&gt;": "&lt;条目 id&gt;" }
 * }
 * </pre>
 * 冲突组 id 包含绑定集合的哈希，增删模组按键后旧记忆自动失效。
 */
public final class RememberedStore {

    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("keyconflictpicker.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static int holdTimeMs = 500;
    private static final Map<String, String> remembered = new LinkedHashMap<>();
    private static boolean loaded = false;

    private RememberedStore() {
    }

    private static synchronized void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        try {
            if (Files.exists(FILE)) {
                JsonObject root = GSON.fromJson(Files.readString(FILE, StandardCharsets.UTF_8), JsonObject.class);
                if (root.has("holdTimeMs")) {
                    holdTimeMs = Math.max(100, root.get("holdTimeMs").getAsInt());
                }
                if (root.has("remembered")) {
                    root.getAsJsonObject("remembered").entrySet().forEach(
                            entry -> remembered.put(entry.getKey(), entry.getValue().getAsString()));
                }
            }
        } catch (Throwable ignored) {
            // 配置损坏时回退为默认值
        }
    }

    private static synchronized void save() {
        try {
            JsonObject root = new JsonObject();
            root.addProperty("holdTimeMs", holdTimeMs);
            JsonObject map = new JsonObject();
            remembered.forEach(map::addProperty);
            root.add("remembered", map);
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (Throwable ignored) {
        }
    }

    /** 长按阈值（毫秒），超过后按键期间直接弹出选择界面。 */
    public static synchronized int holdTimeMs() {
        ensureLoaded();
        return holdTimeMs;
    }

    /** 查询某冲突组已记忆的条目 id，未记忆返回 null。 */
    public static synchronized String get(String groupId) {
        ensureLoaded();
        return remembered.get(groupId);
    }

    /** 记忆某冲突组的选择。 */
    public static synchronized void remember(String groupId, String entryId) {
        ensureLoaded();
        remembered.put(groupId, entryId);
        save();
    }

    /** 清除某冲突组的记忆，返回是否实际移除了记录。 */
    public static synchronized boolean forget(String groupId) {
        ensureLoaded();
        if (remembered.remove(groupId) != null) {
            save();
            return true;
        }
        return false;
    }

    /** 清除全部记忆，返回清除条数。 */
    public static synchronized int forgetAll() {
        ensureLoaded();
        int count = remembered.size();
        if (count > 0) {
            remembered.clear();
            save();
        }
        return count;
    }
}
