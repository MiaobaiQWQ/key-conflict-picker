# Key Conflict Picker（按键冲突选择器）

NeoForge **1.21.1** 客户端模组：当多个按键绑定共用同一个物理键（例如丢弃物品和某模组功能都绑了 G）时，
可以用 `/kcp enable` 对该键开启拦截，之后按下该键会弹出一个**不暂停游戏**的轻量选择界面，
由用户决定本次按键要执行的操作。**默认不拦截任何按键**，按需开启。

## 功能

- **冲突检测**：自动扫描游戏内所有 `KeyMapping`（含其他模组注册的），按 (物理键, 修饰键) 分组检测冲突。
- **不打扰基础操作**：纯原版基础按键（移动 WASD、跳跃、潜行、疾跑、攻击/使用、快捷栏 1-9）之间的冲突不参与拦截；仅当有模组等非基础绑定卷入时才弹窗。
- **按需开启（白名单）**：默认所有冲突都不弹窗，用 `/kcp enable <key>` 对指定键开启拦截，`/kcp disable <key>` 关闭。
- **短按**：已记忆默认选择时直接执行；未记忆时弹出选择界面。
- **长按**（默认 500ms，可在配置中调整）：按键按住期间直接弹出选择界面，方便更换默认。
- **记忆默认**：选择后自动写入 `config/keyconflictpicker.json`，绑定集合变化时自动失效。
- **轻量弹窗**：不暂停游戏，支持鼠标点击与数字键 1-9 快捷选择，Esc 取消。
- **扩展 API**：其他模组可通过 `GatherConflictEntriesEvent` 追加自定义条目、附加图标/提示或隐藏条目。

## 客户端指令 `/kcp`

| 指令 | 说明 |
| --- | --- |
| `/kcp list` | 列出当前所有冲突组（已开启弹窗的会标注） |
| `/kcp enable <key>` | 对指定键开启冲突弹窗（如 `/kcp enable g`），支持 `/kcp enable last` |
| `/kcp disable <key>` | 关闭指定键的冲突弹窗，支持 `/kcp disable last` |
| `/kcp pick <key>` | 手动弹出指定键的选择界面 |
| `/kcp pick last` | 重新弹出最近一次冲突键的选择界面 |
| `/kcp set <key> <entry>` | 直接设定默认操作，`<entry>` 为序号或绑定名 |
| `/kcp forget [key]` | 清除指定键或全部已记忆的默认选择 |

客户端指令不依赖服务端，服务器无需安装本模组。

## 构建与开发

需要 **JDK 21+**（Gradle 工具链会自动解析到 21）。首次使用需生成 Gradle Wrapper：

```bash
gradle wrapper --gradle-version 8.10
```

之后：

```bash
./gradlew build      # 编译并打包，产物在 build/libs
./gradlew runClient  # 启动开发客户端（已自动开启调试按键）
```

开发客户端默认带 `-Dkeyconflictpicker.debugBindings=true`：注册一个绑定到空格的
「调试动作 A」与原版跳跃冲突，进入游戏后按空格（会记录最近冲突），
执行 `/kcp enable last`（或 `/kcp enable space`）后再按空格即可验证完整流程。

## 配置文件

`config/keyconflictpicker.json`：

```json
{
  "holdTimeMs": 500,
  "enabledKeys": [],
  "remembered": {}
}
```

- `holdTimeMs`：长按阈值（毫秒）。
- `enabledKeys`：已开启弹窗拦截的键（由 `/kcp enable` 写入）。
- `remembered`：冲突组 → 默认条目的记忆表，通常无需手动修改。

## 扩展 API（面向模组开发者）

依赖本模组后，在游戏事件总线上监听事件即可：

```java
NeoForge.EVENT_BUS.addListener((GatherConflictEntriesEvent event) -> {
    // event.getKeyCode() / event.getKeyModifier()：发生冲突的键
    // event.getEntries()：可修改的条目列表（已包含该键上所有 KeyMapping）
    event.getEntries().add(new ConflictEntry() {
        @Override public String id() { return "mymod:custom_action"; }
        @Override public Component displayName() { return Component.literal("我的自定义动作"); }
        @Override public void run() { /* 执行动作 */ }
    });
});
```

`ConflictEntry` 可选重写：`icon()`（条目图标）、`tooltip()`（悬停提示）、
`isAvailable()`（可用性）、`ownerLabel()`（来源显示名）。

## 已知限制

- 仅拦截游戏内（无界面打开）状态的按键冲突；GUI 内按键冲突不在当前版本范围内。
- 通过注入点击 + 按下脉冲模拟选中动作，绝大多数 click/isDown 型绑定可正常工作。
