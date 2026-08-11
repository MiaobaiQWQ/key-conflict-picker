package com.example.keyconflictpicker;

import net.neoforged.fml.common.Mod;

/**
 * 按键冲突选择器（Key Conflict Picker）。
 * 客户端专用模组：当多个按键绑定共用同一物理键时，弹出选择界面让用户决定本次按键的行为。
 */
@Mod(KeyConflictPickerMod.MODID)
public class KeyConflictPickerMod {

    public static final String MODID = "keyconflictpicker";

    public KeyConflictPickerMod() {
        // 所有逻辑通过 @EventBusSubscriber 自动注册（见 client / core / command 包）
    }
}
