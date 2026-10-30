# Configuration / 配置

`config/taczammobox/settings.json`：

生成的配置里有 `_comment_cn` / `_comment_en` 说明，以及 `_examples_cn` / `_examples_en` 示例数组；它们只是给人看的，模组不会读取。

```json
{
  "waste": true,
  "autoRefill": true,
  "packerTicks": 5,
  "loaderTicks": 5,
  "capacityRounding": "round",
  "ammoBoxCapacity": {
    "tacz:ammo_box#0#tacz:12g": 180,
    "tacz:ammo_box#1#tacz:12g": 360,
    "tacz:ammo_box#2#tacz:12g": 540,
    "modid:custom_ammo_box": 1080
  }
}
```

| 配置 | 说明 |
|---|---|
| `waste` | `true`：盒/箱装不满也消耗，剩余浪费；`false`：必须整组/整包装得下 |
| `autoRefill` | `true`（默认）：主手用完后自动从背包补同类物品；与其他自动补货模组冲突时可关 |
| `packerTicks` | 打包机每次操作耗时，单位 **tick**，默认 `5` |
| `loaderTicks` | 装填机每次操作耗时，单位 **tick**，默认 `5` |
| `capacityRounding` | 小数倍率取整方式：`round`（默认）/ `floor` / `ceil` |
| `ammoBoxCapacity` | 可选，弹药盒**绝对容量**覆盖，键为 `物品id`、`物品id#等级` 或 `物品id#等级#弹种` |
| `ammoBoxCapacityMultiplier` | 可选，弹药盒**容量倍率**覆盖，键规则同上；推荐用于第三方改等级容量的盒子 |

- `20 tick = 1 秒`，数值越小越快。
- 范围 `1–200`，改完在服务器执行 `/taczammobox reload` 即可生效并同步客户端；单人也可直接重启。
- 服务端和客户端保持一致，进度条速度才会一致。

### 容量倍率（推荐）/ Capacity multiplier

TACZ 默认容量：

```
弹种 stack_size × AMMO_BOX_STACK_SIZE × (level + 1)
```

- `AMMO_BOX_STACK_SIZE` 是 TACZ 同步配置，默认 **3**
- 12g 默认：铁 180、金 360、钻石 540
- 9mm 默认：铁 90、金 180、钻石 270
- 支持小数倍率，例如 `1.25`；`90 × 1.25 = 112.5` 时按 `capacityRounding` 取整：`round`=113、`floor`=112、`ceil`=113
- 自动探测会优先尝试两位小数倍率（如 `1.25`），否则三位小数；都还原不了才写绝对容量

如果第三方作者只是把某个等级整体放大，写倍率即可，所有弹种会自动按比例适配：

```json
"ammoBoxCapacityMultiplier": {
  "tacz:ammo_box": 2.0
}
```

- 12g 铁盒：180 × 2 = 360
- 9mm 铁盒：90 × 2 = 180
- 自动探测成功时写**物品级倍率**，例如 `"tacz:ammo_box": 2.0`
- 只有不同等级确实需要不同倍率时，才写 `tacz:ammo_box#0` / `#1` / `#2`
- 指令：`/taczammobox capacity heldmult <倍率>`、`mult <物品> <等级> <倍率>`、`multall <物品> <倍率>`

### 绝对容量 / Absolute capacity

如果第三方盒子是**固定容量**、不随弹种 `stack_size` 变化，才用绝对覆盖：

```json
"ammoBoxCapacity": {
  "tacz:ammo_box#0#tacz:12g": 180,
  "tacz:ammo_box#1#tacz:12g": 360,
  "tacz:ammo_box#2#tacz:12g": 540,
  "modid:custom_ammo_box": 1080
}
```

- `IAmmoBox` 没有最大容量方法，模组会尝试自动探测
- 自动探测不到时，才需要手写 `ammoBoxCapacity` 或 `ammoBoxCapacityMultiplier`
- 手动装填和装填机共用这个容量
- 改完执行 `/taczammobox reload`
- 物品 id 和弹种 id 可在 F3+H 高级提示里查看

优先级：**绝对容量覆盖 > 倍率覆盖 > 自动探测 > TACZ 公式**；同一等级不要同时写绝对值和倍率。

也可以不手改：
- `/taczammobox capacity held <容量>`：按 `物品#等级#弹种` 写绝对容量
- `/taczammobox capacity heldlevel <容量>`：按 `物品#等级` 写绝对容量
- `/taczammobox capacity heldmult <倍率>`：按 `物品#等级` 写倍率

服务端自动探测到不同容量时，会写入**物品级倍率**，例如 `"tacz:ammo_box": 2.0`；如果倍率不能整除成干净值，才写绝对容量到 `ammoBoxCapacity`，避免取整误差。

---

`config/taczammobox/settings.json`:

The generated file contains `_comment_cn` / `_comment_en` and `_examples_cn` / `_examples_en`; they are human-readable only and ignored by the mod.

```json
{
  "waste": true,
  "autoRefill": true,
  "packerTicks": 5,
  "loaderTicks": 5,
  "capacityRounding": "round",
  "ammoBoxCapacity": {
    "tacz:ammo_box#0#tacz:12g": 180,
    "tacz:ammo_box#1#tacz:12g": 360,
    "tacz:ammo_box#2#tacz:12g": 540,
    "modid:custom_ammo_box": 1080
  }
}
```

| Key | Description |
|---|---|
| `waste` | `true`: consume a partially fitting package and discard the rest; `false`: require the whole package to fit |
| `autoRefill` | `true` (default): pull a matching item back to the main hand after a fill; disable if another refill mod conflicts |
| `packerTicks` | Ticks per ammo packer operation, default `5` |
| `loaderTicks` | Ticks per ammo loader operation, default `5` |
| `capacityRounding` | Rounding for fractional multipliers: `round` (default), `floor` or `ceil` |
| `ammoBoxCapacity` | Optional **absolute** capacity overrides, keyed by `item_id`, `item_id#level` or `item_id#level#ammo` |
| `ammoBoxCapacityMultiplier` | Optional **capacity multipliers**, same key rules; recommended for level-capacity boxes |

- `20 ticks = 1 second`; lower is faster.
- Range `1–200`; after editing, run `/taczammobox reload` on the server (it also syncs clients), or restart.
- Keep client and server values identical so the progress bar matches.

### Capacity multiplier (recommended)

TACZ's default capacity:

```
ammo stack_size × AMMO_BOX_STACK_SIZE × (level + 1)
```

- `AMMO_BOX_STACK_SIZE` is TACZ's synced config, default **3**
- 12g defaults: iron 180, gold 360, diamond 540
- 9mm defaults: iron 90, gold 180, diamond 270
- Decimal multipliers such as `1.25` are supported; `90 × 1.25 = 112.5` follows `capacityRounding`: `round`=113, `floor`=112, `ceil`=113
- Automatic detection prefers two-decimal multipliers (e.g. `1.25`), then three-decimal; if neither reproduces the value it stores an absolute capacity

If a third-party author only scales a level's capacity, use a multiplier so every ammo type adapts automatically:

```json
"ammoBoxCapacityMultiplier": {
  "tacz:ammo_box": 2.0
}
```

- 12g iron: 180 × 2 = 360
- 9mm iron: 90 × 2 = 180
- Automatic detection writes an **item-wide multiplier**, e.g. `"tacz:ammo_box": 2.0`
- Only use `tacz:ammo_box#0` / `#1` / `#2` if the levels really need different multipliers
- Commands: `/taczammobox capacity heldmult <multiplier>`, `mult <item> <level> <multiplier>`, `multall <item> <multiplier>`

### Absolute capacity

Use absolute overrides only when the custom box has a **fixed capacity independent of ammo stack_size**:

```json
"ammoBoxCapacity": {
  "tacz:ammo_box#0#tacz:12g": 180,
  "tacz:ammo_box#1#tacz:12g": 360,
  "tacz:ammo_box#2#tacz:12g": 540,
  "modid:custom_ammo_box": 1080
}
```

- `IAmmoBox` has no max-capacity method; the mod first tries automatic detection
- Only write `ammoBoxCapacity` or `ammoBoxCapacityMultiplier` when detection fails
- Manual filling and the ammo loader share this capacity
- Run `/taczammobox reload` after editing
- Item and ammo ids are visible with F3+H advanced tooltips

Priority: **absolute capacity > multiplier > automatic detection > TACZ formula**; do not set both absolute and multiplier for the same level.

You can also avoid editing JSON:
- `/taczammobox capacity held <capacity>` writes an absolute `item#level#ammo` override
- `/taczammobox capacity heldlevel <capacity>` writes an absolute `item#level` override
- `/taczammobox capacity heldmult <multiplier>` writes an `item#level` multiplier

When the server detects a different capacity, it writes an **item-wide multiplier** such as `"tacz:ammo_box": 2.0`; if the value is not a clean multiple, it writes an absolute capacity into `ammoBoxCapacity` to avoid rounding drift.

## 第三方弹种 / Third-party ammo

第三方枪包的弹种写在 `config/taczammobox/ammo_boxes.json`，由 `/taczammobox scan` 自动补充。这个文件会影响物品注册，**客户端和服务端必须一致**。

Third-party ammo is stored in `config/taczammobox/ammo_boxes.json` and can be filled by `/taczammobox scan`. This file affects item registration, so **client and server must use the same file**.

完整流程见 [third-party-gun-packs.md](third-party-gun-packs.md)。
See [third-party-gun-packs.md](third-party-gun-packs.md) for the full workflow.
