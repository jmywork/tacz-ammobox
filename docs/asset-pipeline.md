# 模型与贴图管线（Blockbench 交接）

这份文档说明盒装弹药 / 弹药箱的模型、贴图放在哪、怎么替换，以及“其他枪包没有模型”时怎么自动兜底。

## 1. 文件结构

```
assets/taczammobox/
  models/item/
    box_base.json           ← 3D 几何 + UV + display 变换（你替换这个）
    crate_base.json         ← 同上，木箱
    box_generic.json        ← 隐藏锚点，配置新增弹种兜底用（不要动）
    crate_generic.json      ← 同上
    box_<id>.json           ← 子模型：覆盖 #label 与 #top（脚本生成，不要手改）
    crate_<id>.json         ← 同上
  models/block/
    crate_base.json         ← 方块几何（满方块）
    crate_<id>.json         ← 方块模型：覆盖 #label/#top/#side/#bottom
  blockstates/
    crate_<id>.json         ← 方块状态（目前只有一个朝向）
  textures/item/
    box_side.png            ← 共享：纸盒侧面
    box_top.png             ← 共享兜底顶面
    box_bottom.png          ← 共享：纸盒底面
    crate_side.png          ← 共享：木箱侧面
    crate_top.png           ← 共享兜底顶面
    crate_bottom.png        ← 共享：木箱底面
    label/
      box_<id>.png          ← 每个口径一张：正面（壳体 + 标签）
      crate_<id>.png        ← 每个口径一张：正面（木箱 + 铭牌）
    top/
      box_<id>.png          ← 每个口径一张：顶面（弹形印刷图）
      crate_<id>.png        ← 每个口径一张：顶面（金属牌 + 弹形图）
```

`<id>` 是弹种路径，例如 `556x45`、`762x39`、`rpg_rocket`。

## 2. 模型约定

- 坐标系：16×16×16（Java Item Model）。
- 必须使用 5 个贴图变量：
  - `#side`：左右侧面
  - `#top`：顶面（盒/箱都合成 TACZ 的 `textures/ammo/slot/<id>.png` 原图标；缺图标时回退脚本绘制的子弹图）
  - `#bottom`：底面
  - `#label`：正面（每口径不同）
  - `#particle`：粒子
- 正面朝 **north**；`south` 用 `#side`，避免文字镜像。
- `display` 至少要给 `gui`、`ground`、`fixed`、`firstperson_righthand`、`thirdperson_righthand`。
- 贴图尺寸默认 32×32；改尺寸时同步改模型的 `texture_size`。
- 盒子/箱子顶面优先使用 TACZ 自带弹药图标（`assets/tacz/.../textures/ammo/slot/<id>.png`），由 `tools/gen_assets.py` 读取 TACZ jar 后合成；图标缺失时回退到脚本绘制的子弹图。
- 纸盒几何：`[0.5, 5, 4]` 到 `[15.5, 11, 12]`（15×6×8，扁平）。
- 木箱几何：`[0, 0, 0]` 到 `[16, 16, 16]`。

方块形态（`crate_<id>`）：

- 物品模型仍是 `models/item/crate_<id>.json`（3D 方块，带 display 变换）。
- 放置后的方块模型是 `models/block/crate_<id>.json`，父模型 `block/crate_base`，`#label` 在 north。
- 掉落表在 `data/taczammobox/loot_tables/blocks/crate_<id>.json`，破坏掉回对应的箱子物品。
- 方块有 `facing` 水平朝向属性，blockstate 用 `y` 旋转模型，放下时正面朝向玩家。
- 第三方弹种（配置新增）没有方块，只有纯物品箱子；物品模型走 `crate_generic` 兜底。

## 3. 用 Blockbench 改模型

1. Blockbench → **File → Import → Java Item Model**，选 `models/item/box_base.json`。
2. 导入贴图（`box_side.png` / `box_top.png` / `box_bottom.png`），按 UV 调整。
3. 改完 **File → Export → Java Item Model**，覆盖 `models/item/box_base.json`。
4. `crate_base.json` 同理。

> `tools/gen_assets.py` 只在 `box_base.json` / `crate_base.json` **不存在**时才生成，重跑不会覆盖你的模型。需要重新生成骨架时，先删掉这两个文件。

如果更想从零开始：新建 **Java Item** 项目，按上面的尺寸和贴图变量做，导出到同名路径即可。

## 4. 新增一个口径

**A. 想给它专属正面 + 顶面贴图（推荐给常用枪包）**

1. 在 `tools/gen_assets.py` 的 `TYPES` 里加一行 `(id, "5.56x45", "#9CA86B")`。
2. 在 `AmmoTypes.BUILTINS` 里加同样的 id 和颜色。
3. 重跑 `python tools/gen_assets.py`：生成 `label/box_<id>.png`、`top/box_<id>.png`、`top/crate_<id>.png` 和子模型。
4. 重新构建。

**B. 只想让它能用，不画专属贴图**

直接在 `config/taczammobox/ammo_boxes.json` 的 `entries` 里加一条：

```json
{ "ammo": "mygunpack:68x51" }
```

重启后物品会注册，客户端通过 `ModelFallback` 自动套用 `box_generic` / `crate_generic`（通用 “AMMO” 外观）。物品名和 tooltip 仍然显示正确口径。

想给它换专属外观时，在资源包里提供 `assets/taczammobox/models/item/box_<id>.json` 覆盖即可，`putIfAbsent` 不会覆盖已有模型。

## 5. 其他枪包的兜底机制

`client/ModelFallback` 监听 `ModelEvent.ModifyBakingResult`：

- 对每个已注册弹种，计算物品模型 key `taczammobox:box_<id>#inventory`；
- 如果烘焙结果里没有这个 key，就把 `box_generic` 的烘焙模型塞进去；
- 有静态模型的（内置 24 种、或资源包提供的）保持不变。

所以“枪包有自定义弹种、但没做模型”不会出现紫黑方块，最差也是通用盒子。

## 6. 贴图规范

- 尺寸 32×32，PNG，带 alpha。
- 纸盒：深灰聚合物壳体、分模线、色条；正面白色标签写大号口径代码；顶面印对应子弹。
- 木箱：木板、金属包角、铆钉、金属铭牌；顶面金属牌印子弹。
- 颜色建议按弹种家族区分：手枪暖色、步枪橄榄、狙击蓝灰、霰弹红、榴弹绿。
- 32px 下完整口径名会糊，正面只放短代码（`556`、`762`、`12G`…），全名交给物品名和 tooltip。

当前 `tools/gen_assets.py` 生成的贴图是**程序化占位**，可以直接用，也可以整套替换。
