# 新建"建模界面" TODO（Blockbench 式建模工作台）

> 方向变更：**不改造现有三屏**，另做一个新的建模界面。
> 现有 `ShowBlockScreen / ItemBlockScreen / TextBlockScreen` 保持原样，零风险。
> 状态：**基线已干净、可编译**（上一轮的布局改动已全部回退，`ModeTabBar.java` 已删除）。

---

## 1. 目标

做一个类似 Blockbench 的**建模工作台**，用来把多个方块/物品/文本元素摆成一个模型。

现有三屏是"给方块实体调参"的思路（8 个滑块 + 一个槽位列表，控件贴在屏幕四角）；
新界面是"编辑一个模型"的思路（元素列表 + 属性面板 + 3D 视口 + 工具栏）。

```
┌──────────────────────────────────────────────────────────────┐
│ 模型名        [移动|旋转|缩放]      [复制][导入][导出][保存]  │ 顶栏
├──────────┬──────────────────────────────────┬────────────────┤
│ 材质      │                                  │ 元素列表        │
│ ┌──────┐ │          3D 视口                  │ ┌────────────┐ │
│ │方块   │ │      （世界透出 + 轴线 gizmo）     │ │ #0 橡木原木 │ │
│ │预览   │ │                                  │ │ #1 石砖     │ │
│ └──────┘ │      左键拖动 = 沿轴平移           │ │ #2 …       │ │
│          │      右键拖动 = 绕轴旋转           │ └────────────┘ │
│ <属性>   │                                  │ ────────────── │
│ <状态>   │                                  │ 属性            │
│          │                                  │ 位置 X ▬▬▬▬     │
│          │                                  │ 旋转 X ▬▬▬▬     │
│          │                                  │ 缩放   ▬▬▬▬     │
├──────────┴──────────────────────────────────┴────────────────┤
│ 元素 #1 · 轴 X · 左键=平移 右键=旋转 Shift=微调 Esc=取消       │ 状态栏
└──────────────────────────────────────────────────────────────┘
```

---

## 2. 明确不做（避免范围失控）

- 不引入 UV / 贴图编辑（MC 方块模型没有这个层级）
- 不做顶点级编辑（方块是刚体 + 纹理映射，不是网格）
- 不做顶部下拉菜单（MC `Screen` 无菜单系统）
- 本轮不替换三个旧界面，只新增入口
- 不做视口相机 orbit / 滚轮缩放（第 2 阶段再议）

---

## 3. 可以复用的现成能力（这是新界面成本可控的原因）

| 能力 | 现成类 | 复用方式 |
|---|---|---|
| 世界坐标轴 gizmo 拖拽 | `gui/transform/AxisDragController` | 直接 new，实现 `Host`（世界原点/轴向/缩放） |
| 世界坐标 → 屏幕 | `utils/CameraProjection` | 直接调用 |
| 元素列表（图标 + 多选 + 滚动） | `gui/widget/AbstractTransformIconList` 及三个子类 | 直接放进右面板 |
| 属性行（滑块 + 加减 + 输入框） | `gui/widget/TransformComponent` | 直接放进属性区 |
| 范围/步长/吸附 | `gui/validate/*`、`SliderButton` | 不动 |
| 数据模型与同步 | `AbstractTransformBlockEntity` + `TransformDataOncePacket` | 不动 |
| 分享/导入导出 | `ShareUtils` + `ShareController` 的写法 | 抽出来复用 |

**结论：数据层、拖拽层、控件层全部现成，新界面主要是"外壳 + 布局 + 装配"。**

---

## 4. 待你拍板（影响实现方式）

### 4.1 入口怎么进？

- [ ] **A** 现有 `GuiItem` 加一个修饰键（如 `Shift + 右键`）进新界面，普通右键仍是旧界面
- [ ] **B** 配置项 `useModelingScreen`（默认 false），开启后 `GuiItem` 直接进新界面
- [x] **C** 新物品"建模台"，拿着它对目标方块右键进新界面 ← **已采用**

### 4.2 先支持哪种实体？

- [x] **A** 只做 `ShowBlockEntity`（方块模型，最像"建模"） ← **已采用**

### 4.3 旧界面是否保留

- [ ] **A** 保留，新界面是额外入口（建议）
- [ ] **B** 新界面完成并验收后，把旧入口指向新界面，旧类保留但不再默认打开

---

## 5. 新增文件

| 文件 | 职责 | 规模 |
|---|---|---|
| `gui/modeling/ModelingScreen.java` | 新界面主体：布局、装配、事件 | ~400 行 |
| `gui/modeling/ModelingLayout.java` | 区域计算（header/footer/materials/viewport/outliner/properties），含窄窗口夹取 | ~120 行 |
| `gui/modeling/ModelingChrome.java` | 面板绘制（底色/边框/标题/分隔线/状态栏），纯静态绘制 | ~120 行 |
| `gui/widget/ModeTabBar.java` | 平铺模式标签（替代 `CycleButton` 循环） | ~90 行 |
| `gui/widget/ToolButton.java` | 顶栏图标按钮（悬停高亮 + tooltip + 选中态） | ~70 行 |

**不改动**：三个旧 Screen、`AbstractTransformScreen`、渲染器、数据层、网络包。

---

## 6. 分阶段实施（每阶段都能编译 + 能进游戏看）

### P0.5 · 实心背景 + 自由相机（已完成）
- [x] `ModelingViewport`：GUI 内自绘 3D，`fill` 实心背景 → 清深度 → 临时透视投影 → PoseStack 里放视图变换 → `renderSingleBlock` → 恢复投影
- [x] 相机状态：`yaw / pitch / distance`，中键拖动环绕、滚轮缩放
- [x] 整屏实心底色（`renderBackground` 铺 `CHROME_BG`，世界不再透出）
- [x] 区域改为：左=元素列表 / 右上=元素属性 / 右下=变换属性
- [ ] **待你进游戏确认**：模型是否可见、旋转方向是否符合直觉、缩放范围是否合适

> 关键实现说明（踩过的坑）：
> - 本 mod 的方块几何**不是** BlockEntityRenderer 画的（BER 只画边框/轴线/文字），
>   几何来自 `NeoShowBlockModel` 经 `ModelData(BASE_BLOCK_ENTITY)` 提供。
> - **世界管线在 GUI 里画不出来**：`BlockRenderDispatcher.renderSingleBlock` 依赖世界渲染阶段的
>   着色器与批次状态，放进 `Screen.render` 后一片空白（实测）。
> - 正确通路是**物品形态**：`NeoShowBlockModel.getRenderPasses(ItemStack)` 从
>   `BLOCK_ENTITY_DATA` 的 `Blocks` 列表重建模型，再交给 `ItemRenderer.renderStatic`（GUI 受支持）。
>   几何、`cutoutMipped` 渲染类型、光照都由它处理好。
> - 视图变换全部放进 `PoseStack`（GUI 阶段 modelView 是单位阵），因此不必改投影矩阵，
>   也不会破坏后续 2D 绘制。y 轴按 GUI 习惯取负（`scale(fit, -fit, fit)`）。
> - 物品形态按「变换数据指纹」缓存，数据没变就复用同一 `ItemStack` 实例以命中模型缓存。

### P0 · 空壳（可验证布局）✅ 已完成
- [x] P0.1 `ModelingLayout`：算出 6 个区域，窗口 < 一定尺寸时收缩/夹取
- [x] P0.2 `ModelingChrome`：绘制顶栏、底栏、左/右面板底色与边框、面板标题
- [x] P0.3 `ModelingScreen`：继承 `Screen`，`renderBackground` 空实现（世界透出）
- [x] P0.4 入口 C（新物品 `modeling_wand`）能打开、`Esc` 能关闭
- [x] P0.5 `gradlew compileJava --offline` → BUILD SUCCESSFUL，lang JSON 校验通过
- [ ] P0.6 **待你进游戏截图确认区域划分**（下一步前提）

> 已完成产物：
> - `gui/modeling/ModelingLayout.java`、`ModelingChrome.java`、`ModelingScreen.java`
> - `item/showblocktool/ModelingWandItem.java`
> - `ItemRegistry.MODELING_WAND_ITEM`、`GroupRegistry` 创造模式标签项
> - `assets/yuushya/models/item/modeling_wand.json`（贴图复用 `blueprint.png`）
> - lang 中英各 11 条新 key

### P1 · 元素列表 + 属性面板（重做为 Blockbench 风格）

**第一版（用原版滑块）已废弃**，原因：`TransformComponent` 一行只能放一个轴，8 个分量要 8 行，
横向还被滑块+加减+输入框占满，面板放不下也不像建模工具。

**重写后的方案**：

- [x] `gui/widget/NumberField.java`（新）：紧凑数字框
      - 按下拖动 → 直接改值（每像素对应固定增量，Shift 为 1/10）
      - 单击（未拖动）→ 进入文本输入，Enter 确认 / Esc 取消
      - 滚轮 → 按 step 递增
      - 旋转字段支持越界绕回（wrap）
- [x] `gui/modeling/ModelingElementList.java`（新）：自绘紧凑列表
      - 20px 行高（原来是 45px），图标 + 序号 + 名称
      - 选中/悬停高亮，滚轮滚动，右侧 3px 自绘滚动条
- [x] `gui/modeling/ModelingProperties.java`（重写）：4 行紧凑布局
      ```
      位移   [x][y][z]
      旋转   [x][y][z]
      缩放   [x][y][z]   ← 三轴等比，拖任一个同步
      亮度   [  ]
      ```
- [x] `ModelingScreen`：接入新列表与新字段，输入分发（列表点击 / 字段拖动 / 视口中键）
- [x] 撤销对 `BlockStateIconList` 的耦合改动 —— 新界面不再复用它，旧界面保持零改动
- [x] 编译通过、lang 校验通过
- [ ] **待验收**：拖动数字框改值、单击输入、滚轮、元素切换

### P2 · 视口 gizmo（拖动 / 旋转）

- [x] `AxisDragController` 抽出 `Projector` 接口 —— 投影可插拔（世界相机 / 离屏视口共用一套换算与手感）
- [x] `AxisDragController.Host#handleHalfLength()` —— 手柄长度可配置（视口用 0.55 方块，世界用 1.5）
- [x] `AxisDragController#nearestAxis(..., maxDistance)` —— 拾取半径（视口用 10px，点空白不启动拖拽）
- [x] `ModelingCamera`（新）：yaw/pitch/distance + `baseMatrix` + `itemMatrix`
      - `itemMatrix = baseMatrix · 物品 FIXED 显示变换`，与 `renderStatic` 完全一致
      - 渲染与 gizmo 共用同一份矩阵，杜绝"画在哪 ≠ 点在哪"
- [x] `ModelingGizmo`（新）：常显三轴、悬停高亮、左键平移 / 右键旋转、Shift 精细
      - 中心 `F·(0.5 + pos·scales/16)`，轴 `F·unit`（跟随方块朝向）
      - **注意**：`ShowBlockModel` 的链是 `S · T(pos/16) · R`（矩阵从右往左），
        `T(pos/16)` 会被 `S` 缩放，所以是 `pos·scales/16` 而不是 `pos/16`。
        早期版本误用 `pos/16`，导致 pos≠0 时轴线整体偏移（缩放 0.5、pos=-24 时偏出 0.75 方块）。
      - `axisScale` 返回该轴缩放：平移 `Δpos = blocks·16/scale`，旋转 `δ = blocks/scale_tangent`
- [x] `ModelingViewport` 改为持有 `ModelingCamera`，渲染走 `baseMatrix`
- [x] 视口交互接线：中键环绕 / 滚轮缩放 / 左键平移 / 右键旋转 / 靠近轴 10px 才拾取
- [x] 编译通过
- [ ] **待验收**：
      1. 三条轴线是否与模型对齐（中心在元素中心、方向随方块朝向）
      2. 左键拖轴 → 元素沿该轴平移，数字框同步
      3. 右键拖轴 → 绕该轴旋转
      4. 点空白处不应启动拖拽
      5. 环绕视角后，轴线与拖拽方向是否仍一致

### P3 · 工具栏与模式
- [ ] P3.1 `ModeTabBar` 顶栏平铺
- [ ] P3.2 `ToolButton` 放新增/删除/替换/显隐/复制/粘贴/保存/统计
- [ ] P3.3 快捷键：`1/2/3` 切模式、`Esc` 关闭

### P4 · 导入导出
- [ ] P4.1 接 `ShareUtils` 的复制/粘贴
- [ ] P4.2 保存到 `modellings/` 目录（复用 `EditScreen` 命名对话框）

---

## 7. 语言文件新增

| key | zh_cn | en_us |
|---|---|---|
| `gui.yuushya.modeling.title` | 建模 | Modeling |
| `gui.yuushya.modeling.outliner` | 元素 | Elements |
| `gui.yuushya.modeling.properties` | 属性 | Properties |
| `gui.yuushya.modeling.materials` | 材质 | Material |
| `gui.yuushya.modeTab.slider` | 一般 | Basic |
| `gui.yuushya.modeTab.fine_tune` | 微调 | Fine |
| `gui.yuushya.modeTab.edit` | 输入 | Input |
| `gui.yuushya.transform.axis` | 轴 %s | Axis %s |

> 现有 `gui.showBlockScreen.mode.*` 是整句"当前:一般模式"，做标签太宽，故新增短标签。

---

## 8. 风险

1. **视觉无法自动验证** —— 只有 `compileJava` 可跑，每阶段都要你进游戏截图确认。
2. **窄窗口**（GUI scale 大 / 小分辨率）：`LEFT + RIGHT` 面板可能挤没视口，`ModelingLayout` 必须做收缩与下限保护。
3. **Item 屏的顶栏最挤**（12 个按钮），若做 P3 需要按钮溢出处理（换行或收进"更多"）。
4. **不要改 `AbstractTransformScreen`** —— 它是三个旧界面的基类，改它会连带影响旧界面。新界面所需公共逻辑宁可复制小段代码，也不反向修改基类。
5. 仓库根目录的 `img.png`（参考图）目前是未跟踪文件，**不要提交进仓库**。

---

## 9. 需要你先圈的三件事

1. §4.1 入口：A / B / C ？
2. §4.2 实体范围：A / B / C ？
3. §6 是否按 P0→P4 分阶段验收（每阶段停一次），还是一次做到 P2 再给我看？
