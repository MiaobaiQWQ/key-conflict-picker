package com.example.keyconflictpicker.client.gui;

import com.example.keyconflictpicker.api.ConflictEntry;
import com.example.keyconflictpicker.api.KeyMappingEntry;
import com.example.keyconflictpicker.core.ActionInvoker;
import com.example.keyconflictpicker.core.ConflictRegistry;
import com.example.keyconflictpicker.core.KeyInterceptor;
import com.example.keyconflictpicker.core.RememberedStore;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 按键冲突选择界面：不暂停游戏的轻量弹窗。
 * 支持鼠标点击、数字键 1-9 快捷选择，Esc 取消（不执行、不记忆）。
 */
public class ConflictSelectScreen extends Screen {

    private static final int PANEL_WIDTH = 250;
    private static final int ROW_HEIGHT = 24;
    private static final int MAX_ENTRIES = 12;

    /** 原版按键分类的翻译键，用于在没有探测到归属模组时回退显示 Minecraft。 */
    private static final Set<String> VANILLA_CATEGORIES = Set.of(
            "key.categories.movement", "key.categories.misc", "key.categories.multiplayer",
            "key.categories.gameplay", "key.categories.inventory", "key.categories.interface",
            "key.categories.creative", "key.categories.ui");

    private final ConflictRegistry.Group group;
    private final List<ConflictEntry> entries;

    private int panelTop;
    private int panelHeight;

    public ConflictSelectScreen(ConflictRegistry.Group group) {
        super(Component.translatable("keyconflictpicker.screen.title"));
        this.group = group;
        List<ConflictEntry> gathered = KeyInterceptor.entriesFor(group);
        this.entries = gathered.size() > MAX_ENTRIES ? gathered.subList(0, MAX_ENTRIES) : gathered;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        this.panelHeight = 34 + entries.size() * ROW_HEIGHT + 18;
        this.panelTop = Math.max(10, (this.height - this.panelHeight) / 2);
    }

    private int panelLeft() {
        return (this.width - PANEL_WIDTH) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int left = panelLeft();

        // 半透明遮罩 + 面板
        graphics.fill(0, 0, this.width, this.height, 0x40000000);
        graphics.fill(left, panelTop, left + PANEL_WIDTH, panelTop + panelHeight, 0xEE15151C);
        graphics.fill(left, panelTop, left + PANEL_WIDTH, panelTop + 1, 0xFF4A4A55);
        graphics.fill(left, panelTop + panelHeight - 1, left + PANEL_WIDTH, panelTop + panelHeight, 0xFF4A4A55);

        graphics.drawCenteredString(this.font,
                Component.translatable("keyconflictpicker.screen.subtitle", group.keyDisplayName()),
                left + PANEL_WIDTH / 2, panelTop + 8, 0xFFFFFF);

        ConflictEntry hovered = null;
        for (int i = 0; i < entries.size(); i++) {
            ConflictEntry entry = entries.get(i);
            int rowTop = panelTop + 30 + i * ROW_HEIGHT;
            boolean isHovered = mouseX >= left + 6 && mouseX <= left + PANEL_WIDTH - 6
                    && mouseY >= rowTop && mouseY < rowTop + ROW_HEIGHT - 2;
            if (isHovered) {
                graphics.fill(left + 6, rowTop, left + PANEL_WIDTH - 6, rowTop + ROW_HEIGHT - 2, 0x55FFFFFF);
                hovered = entry;
            }

            graphics.drawString(this.font, (i + 1) + ".", left + 12, rowTop + 8, 0x999999, false);

            int textX = left + 28;
            ItemStack icon = entry.icon();
            if (!icon.isEmpty()) {
                graphics.renderItem(icon, left + 26, rowTop + 3);
                textX = left + 48;
            }

            graphics.drawString(this.font, entry.displayName(), textX, rowTop + 8, 0xFFFFFF, false);

            String owner = ownerLabel(entry);
            graphics.drawString(this.font, owner,
                    left + PANEL_WIDTH - 12 - this.font.width(owner), rowTop + 8, 0x7F7F7F, false);
        }

        graphics.drawCenteredString(this.font,
                Component.translatable("keyconflictpicker.screen.hint"),
                left + PANEL_WIDTH / 2, panelTop + panelHeight - 13, 0x808080);

        if (hovered != null && !hovered.tooltip().isEmpty()) {
            graphics.renderTooltip(this.font, hovered.tooltip(), mouseX, mouseY);
        }
    }

    private String ownerLabel(ConflictEntry entry) {
        if (entry.ownerLabel() != null) {
            return entry.ownerLabel();
        }
        if (entry instanceof KeyMappingEntry keyMappingEntry) {
            Optional<ModContainer> container = ModList.get().getModContainerByObject(keyMappingEntry.mapping());
            if (container.isPresent()) {
                return container.get().getModInfo().getDisplayName();
            }
            if (VANILLA_CATEGORIES.contains(keyMappingEntry.mapping().getCategory())) {
                return "Minecraft";
            }
        }
        return I18n.get("keyconflictpicker.owner.unknown");
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        if (keyCode >= GLFW.GLFW_KEY_1 && keyCode <= GLFW.GLFW_KEY_9) {
            int index = keyCode - GLFW.GLFW_KEY_1;
            if (index < entries.size()) {
                select(index);
                return true;
            }
        }
        if (keyCode == group.keyCode()) {
            // 长按弹窗时按键仍处于按住状态，吞掉其重复事件
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int left = panelLeft();
        if (mouseX >= left + 6 && mouseX <= left + PANEL_WIDTH - 6) {
            for (int i = 0; i < entries.size(); i++) {
                int rowTop = panelTop + 30 + i * ROW_HEIGHT;
                if (mouseY >= rowTop && mouseY < rowTop + ROW_HEIGHT - 2) {
                    select(i);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void select(int index) {
        ConflictEntry entry = entries.get(index);
        RememberedStore.remember(group.id(), entry.id());
        this.onClose();
        ActionInvoker.runEntry(entry);
    }
}
