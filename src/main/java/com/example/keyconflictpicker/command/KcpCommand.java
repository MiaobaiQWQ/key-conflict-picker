package com.example.keyconflictpicker.command;

import com.example.keyconflictpicker.api.ConflictEntry;
import com.example.keyconflictpicker.core.ConflictRegistry;
import com.example.keyconflictpicker.core.KeyInterceptor;
import com.example.keyconflictpicker.core.RememberedStore;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * 客户端指令 /kcp：除按键触发外，所有功能均可用指令操作。
 * 客户端指令不依赖服务端，单人/服务器均可使用。
 */
public final class KcpCommand {

    private KcpCommand() {
    }

    /** 按键名补全：所有冲突组涉及的物理键。 */
    private static final SuggestionProvider<CommandSourceStack> KEY_SUGGESTIONS =
            (context, builder) -> {
                List<String> names = new ArrayList<>();
                for (ConflictRegistry.Group group : ConflictRegistry.scan(false)) {
                    names.add(shortKeyName(group.keyCode()));
                }
                return SharedSuggestionProvider.suggest(names.stream().distinct().toList(), builder);
            };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("kcp")
                .then(Commands.literal("list").executes(context -> list(context.getSource())))
                .then(Commands.literal("pick")
                        .then(Commands.literal("last").executes(context -> pickLast(context.getSource())))
                        .then(Commands.argument("key", StringArgumentType.word())
                                .suggests(KEY_SUGGESTIONS)
                                .executes(context -> pick(context.getSource(),
                                        StringArgumentType.getString(context, "key")))))
                .then(Commands.literal("enable")
                        .then(Commands.literal("last").executes(context -> enableLast(context.getSource())))
                        .then(Commands.argument("key", StringArgumentType.word())
                                .suggests(KEY_SUGGESTIONS)
                                .executes(context -> enable(context.getSource(),
                                        StringArgumentType.getString(context, "key")))))
                .then(Commands.literal("disable")
                        .then(Commands.literal("last").executes(context -> disableLast(context.getSource())))
                        .then(Commands.argument("key", StringArgumentType.word())
                                .suggests(KEY_SUGGESTIONS)
                                .executes(context -> disable(context.getSource(),
                                        StringArgumentType.getString(context, "key")))))
                .then(Commands.literal("set")
                        .then(Commands.argument("key", StringArgumentType.word())
                                .suggests(KEY_SUGGESTIONS)
                                .then(Commands.argument("entry", StringArgumentType.greedyString())
                                        .executes(context -> set(context.getSource(),
                                                StringArgumentType.getString(context, "key"),
                                                StringArgumentType.getString(context, "entry"))))))
                .then(Commands.literal("forget")
                        .executes(context -> forgetAll(context.getSource()))
                        .then(Commands.argument("key", StringArgumentType.word())
                                .suggests(KEY_SUGGESTIONS)
                                .executes(context -> forget(context.getSource(),
                                        StringArgumentType.getString(context, "key"))))));
    }

    private static int list(CommandSourceStack source) {
        List<ConflictRegistry.Group> groups = ConflictRegistry.scan(false);
        if (groups.isEmpty()) {
                        source.sendFailure(Component.translatable("keyconflictpicker.cmd.list.empty"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("keyconflictpicker.cmd.list.header", groups.size()), false);
        for (ConflictRegistry.Group group : groups) {
            StringBuilder bindings = new StringBuilder();
            for (int i = 0; i < group.mappings().size(); i++) {
                if (i > 0) {
                    bindings.append(", ");
                }
                bindings.append(Component.translatable(group.mappings().get(i).getName()).getString());
            }
            String line = group.keyDisplayName().getString() + " -> " + bindings;
            MutableComponent component = Component.literal("  " + line);
            if (RememberedStore.isKeyEnabled(group.keyCode(), group.modifier())) {
                component.append(Component.translatable("keyconflictpicker.cmd.list.enabled"));
            }
            source.sendSuccess(() -> component, false);
        }
        return groups.size();
    }

    private static int pick(CommandSourceStack source, String keyName) {
        int keyCode = parseKeyCode(keyName);
        if (keyCode == -1) {
            source.sendFailure(Component.translatable("keyconflictpicker.cmd.pick.unknown_key", keyName));
            return 0;
        }
        Optional<ConflictRegistry.Group> group = ConflictRegistry.groupForAnyModifier(keyCode, false);
        if (group.isEmpty()) {
            source.sendFailure(Component.translatable("keyconflictpicker.cmd.pick.no_conflict", keyName));
            return 0;
        }
        ConflictRegistry.markLast(group.get());
        KeyInterceptor.openPicker(group.get());
        source.sendSuccess(() -> Component.translatable("keyconflictpicker.cmd.pick.opened", keyName), false);
        return 1;
    }

    private static int pickLast(CommandSourceStack source) {
        Optional<ConflictRegistry.Group> group = ConflictRegistry.lastGroup();
        if (group.isEmpty()) {
            source.sendFailure(Component.translatable("keyconflictpicker.cmd.pick.no_last"));
            return 0;
        }
        KeyInterceptor.openPicker(group.get());
        source.sendSuccess(() -> Component.translatable("keyconflictpicker.cmd.pick.opened",
                group.get().keyDisplayName().getString()), false);
        return 1;
    }

    private static int enable(CommandSourceStack source, String keyName) {
        int keyCode = parseKeyCode(keyName);
        if (keyCode == -1) {
            source.sendFailure(Component.translatable("keyconflictpicker.cmd.pick.unknown_key", keyName));
            return 0;
        }
        Optional<ConflictRegistry.Group> group = ConflictRegistry.groupForAnyModifier(keyCode, false);
        if (group.isEmpty()) {
            source.sendFailure(Component.translatable("keyconflictpicker.cmd.pick.no_conflict", keyName));
            return 0;
        }
        if (RememberedStore.enableKey(keyCode, group.get().modifier())) {
            source.sendSuccess(() -> Component.translatable("keyconflictpicker.cmd.enable.ok", keyName), false);
        } else {
            source.sendSuccess(() -> Component.translatable("keyconflictpicker.cmd.enable.already", keyName), false);
        }
        return 1;
    }

    private static int enableLast(CommandSourceStack source) {
        Optional<ConflictRegistry.Group> group = ConflictRegistry.lastGroup();
        if (group.isEmpty()) {
            source.sendFailure(Component.translatable("keyconflictpicker.cmd.pick.no_last"));
            return 0;
        }
        return enable(source, shortKeyName(group.get().keyCode()));
    }

    private static int disable(CommandSourceStack source, String keyName) {
        int keyCode = parseKeyCode(keyName);
        if (keyCode == -1) {
            source.sendFailure(Component.translatable("keyconflictpicker.cmd.pick.unknown_key", keyName));
            return 0;
        }
        Optional<ConflictRegistry.Group> group = ConflictRegistry.groupForAnyModifier(keyCode, false);
        if (group.isEmpty()) {
            source.sendFailure(Component.translatable("keyconflictpicker.cmd.pick.no_conflict", keyName));
            return 0;
        }
        if (RememberedStore.disableKey(keyCode, group.get().modifier())) {
            source.sendSuccess(() -> Component.translatable("keyconflictpicker.cmd.disable.ok", keyName), false);
            return 1;
        }
        source.sendFailure(Component.translatable("keyconflictpicker.cmd.disable.none", keyName));
        return 0;
    }

    private static int disableLast(CommandSourceStack source) {
        Optional<ConflictRegistry.Group> group = ConflictRegistry.lastGroup();
        if (group.isEmpty()) {
            source.sendFailure(Component.translatable("keyconflictpicker.cmd.pick.no_last"));
            return 0;
        }
        return disable(source, shortKeyName(group.get().keyCode()));
    }

    private static int set(CommandSourceStack source, String keyName, String entryArg) {
        int keyCode = parseKeyCode(keyName);
        if (keyCode == -1) {
            source.sendFailure(Component.translatable("keyconflictpicker.cmd.pick.unknown_key", keyName));
            return 0;
        }
        Optional<ConflictRegistry.Group> group = ConflictRegistry.groupForAnyModifier(keyCode, false);
        if (group.isEmpty()) {
            source.sendFailure(Component.translatable("keyconflictpicker.cmd.pick.no_conflict", keyName));
            return 0;
        }
        List<ConflictEntry> entries = KeyInterceptor.entriesFor(group.get());
        ConflictEntry selected = matchEntry(entries, entryArg);
        if (selected == null) {
            source.sendFailure(Component.translatable("keyconflictpicker.cmd.set.invalid", entryArg, entries.size()));
            return 0;
        }
        RememberedStore.remember(group.get().id(), selected.id());
        Component keyDisplay = group.get().keyDisplayName();
        Component entryDisplay = selected.displayName();
        source.sendSuccess(() -> Component.translatable("keyconflictpicker.cmd.set.ok", keyDisplay, entryDisplay), false);
        return 1;
    }

    private static int forget(CommandSourceStack source, String keyName) {
        int keyCode = parseKeyCode(keyName);
        if (keyCode == -1) {
            source.sendFailure(Component.translatable("keyconflictpicker.cmd.pick.unknown_key", keyName));
            return 0;
        }
        Optional<ConflictRegistry.Group> group = ConflictRegistry.groupForAnyModifier(keyCode, false);
        if (group.isPresent() && RememberedStore.forget(group.get().id())) {
            source.sendSuccess(() -> Component.translatable("keyconflictpicker.cmd.forget.one", keyName), false);
            return 1;
        }
        source.sendFailure(Component.translatable("keyconflictpicker.cmd.forget.none"));
        return 0;
    }

    private static int forgetAll(CommandSourceStack source) {
        int count = RememberedStore.forgetAll();
        if (count == 0) {
            source.sendFailure(Component.translatable("keyconflictpicker.cmd.forget.all_empty"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("keyconflictpicker.cmd.forget.all", count), false);
        return count;
    }

    /** 条目匹配：支持 1 起始的序号、条目 id 或显示名的模糊匹配。 */
    private static ConflictEntry matchEntry(List<ConflictEntry> entries, String arg) {
        try {
            int index = Integer.parseInt(arg.trim());
            if (index >= 1 && index <= entries.size()) {
                return entries.get(index - 1);
            }
            return null;
        } catch (NumberFormatException ignored) {
            // 按名称匹配
        }
        String lower = arg.toLowerCase(Locale.ROOT);
        for (ConflictEntry entry : entries) {
            if (entry.id().toLowerCase(Locale.ROOT).contains(lower)
                    || entry.displayName().getString().toLowerCase(Locale.ROOT).contains(lower)) {
                return entry;
            }
        }
        return null;
    }

    /**
     * 解析按键名：支持简写（如 {@code space}、{@code g}）与完整名（如 {@code key.keyboard.space}）。
     * 无法识别时返回 -1。
     */
    private static int parseKeyCode(String name) {
        String normalized = name.toLowerCase(Locale.ROOT);
        String full = normalized.startsWith("key.") || normalized.startsWith("mouse.")
                ? normalized
                : "key.keyboard." + normalized;
        InputConstants.Key key = InputConstants.getKey(full);
        return key == InputConstants.UNKNOWN ? -1 : key.getValue();
    }

    /** 物理键的简短可读名，用于补全与提示。 */
    private static String shortKeyName(int keyCode) {
        String name = InputConstants.getKey(keyCode, 0).getName();
        if (name.startsWith("key.keyboard.")) {
            return name.substring("key.keyboard.".length());
        }
        return name;
    }
}
