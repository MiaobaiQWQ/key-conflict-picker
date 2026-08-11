package com.example.keyconflictpicker.client.gui;

import com.example.keyconflictpicker.core.ConflictRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 按键绑定一览界面（/kcp gui）：列出游戏中所有按键绑定，
 * 包含已绑定与未绑定的，标注来源分类、归属模组以及是否处于冲突中。
 */
public class KeyBindingsListScreen extends Screen {

    private final List<KeyMapping> mappings;
    private final Set<String> conflictNames;
    private final int unboundCount;

    private BindingList list;

    public KeyBindingsListScreen() {
        super(Component.translatable("keyconflictpicker.gui.title"));
        List<KeyMapping> all = new ArrayList<>(List.of(Minecraft.getInstance().options.keyMappings));
        all.sort(Comparator.comparing(KeyMapping::getCategory).thenComparing(KeyMapping::getName));
        this.mappings = all;
        this.unboundCount = (int) all.stream().filter(KeyMapping::isUnbound).count();

        Set<String> names = new HashSet<>();
        for (ConflictRegistry.Group group : ConflictRegistry.scan(false)) {
            for (KeyMapping mapping : group.mappings()) {
                names.add(mapping.getName());
            }
        }
        this.conflictNames = names;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        this.list = new BindingList(this.minecraft, this.width, this.height, 34, this.height - 20);
        for (KeyMapping mapping : this.mappings) {
            this.list.add(new BindingEntry(mapping, conflictNames.contains(mapping.getName()), this.font));
        }
        this.addWidget(this.list);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        this.list.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFF);
        graphics.drawCenteredString(this.font,
                Component.translatable("keyconflictpicker.gui.summary", mappings.size(), unboundCount),
                this.width / 2, 22, 0x808080);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private static final class BindingList extends ObjectSelectionList<BindingEntry> {

        BindingList(Minecraft minecraft, int width, int height, int y0, int y1) {
            super(minecraft, width, height, y0, y1);
        }

        void add(BindingEntry entry) {
            this.addEntry(entry);
        }
    }

    private static final class BindingEntry extends ObjectSelectionList.Entry<BindingEntry> {

        private final KeyMapping mapping;
        private final boolean conflict;
        private final Font font;

        BindingEntry(KeyMapping mapping, boolean conflict, Font font) {
            this.mapping = mapping;
            this.conflict = conflict;
            this.font = font;
        }

        @Override
        public Component getNarration() {
            return Component.translatable(mapping.getName());
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean hovering, float partialTick) {
            if (hovering) {
                graphics.fill(left, top, left + width, top + height, 0x40FFFFFF);
            }

            // 名称（冲突中的标黄）
            int nameColor = conflict ? 0xFFC94A : 0xFFFFFF;
            graphics.drawString(this.font, Component.translatable(mapping.getName()), left + 8, top + 7, nameColor, false);

            // 绑定的键 / 未绑定
            Component keyLabel = mapping.isUnbound()
                    ? Component.translatable("keyconflictpicker.gui.unbound")
                    : mapping.getKey().getDisplayName();
            int keyColor = mapping.isUnbound() ? 0x808080 : 0x6FBFFF;
            int keyX = left + width * 2 / 5;
            graphics.drawString(this.font, keyLabel, keyX, top + 7, keyColor, false);

            // 归属模组 / 分类
            String owner = ownerOf(mapping);
            graphics.drawString(this.font, owner,
                    left + width - 12 - this.font.width(owner), top + 7, 0x7F7F7F, false);
        }

        private String ownerOf(KeyMapping mapping) {
            String category = Component.translatable(mapping.getCategory()).getString();
            String name = mapping.getName();
            if (name.startsWith("key.")) {
                int secondDot = name.indexOf('.', "key.".length());
                if (secondDot > "key.".length()) {
                    String candidate = name.substring("key.".length(), secondDot);
                    if (net.neoforged.fml.ModList.get().isLoaded(candidate)) {
                        return candidate + " · " + category;
                    }
                }
            }
            return category;
        }
    }
}
