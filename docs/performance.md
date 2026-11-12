# 性能优化记录（0.5.0 计划一）

> 时间：2026-11-12
> 范围：只改「每个玩家每 tick 都跑」和「每个可见方块每帧都跑」的路径，不改功能与数值。
> 下面标「估算」的数字是按调用次数算出来的量级，不是实测；验收以游戏内 TPS/FPS 与 spark 采样为准。

## 0. 这一轮之前已经做过的

0.4.0 时代已经优化过一批：浮游炮的枪型签名与枪列表按版本缓存、弹链满弹时不再遍历背包、装配机空转时不再每 tick 查邻居、机器空闲有 `idleCooldown`、装箱结果有缓存。本轮不重复这些。

## 1. 浮游炮机架：没有卫星的玩家不再做实体查询

`SatelliteRackManager.onPlayerTick` 对每个玩家每 tick 都执行：

```text
findRack()   → 主手 + 36 格背包扫描（+ Curios，已有 10 tick 重试）
rack 为空    → discard() → getEntitiesOfClass(10 格膨胀 AABB) + 排序
```

`discard()` 里的实体查询是最贵的一步，而绝大多数玩家根本没用机架。

现在：

- 用 `ACTIVE` 集合记住「当前有卫星实体」的玩家；
- 不在 `ACTIVE` 里：背包扫描降到 **5 tick 一次**，并且**完全不查实体**；
- 在 `ACTIVE` 里：保持原来的每 tick 精确同步，卸下机架后如果一次查询发现已经没有卫星，就退出 `ACTIVE`（死亡、跨维度、区块卸载后自愈）；
- `discard()` 不再排序、不再走 `satellites()` 的列表分配；
- `easterEggTab` 开关的检查提到背包扫描之前。

代价：从背包第一次装上机架，枪扇最多晚 5 tick（250ms）出现。估算：20 人服务器、无人使用机架时，每秒少 400 次 AABB 实体查询和 400 次背包扫描。

### 1.1 目标扫描：没有目标时也按节拍重扫

`gunTarget` 原来只在「有缓存目标」时才看 `targetCheckTick`，目标为空就直接 `searchTarget`。结果是**和平区域比战斗中更贵**：10 把枪每 tick 各做一次半径 24 格的实体查询，而战斗中每把枪是 4–7 tick 一次。

现在空结果也写 `targetCheckTick` 并尊重它，节拍按槽位错开（每把枪 4–7 tick 一次，10 把合起来约 2 次/tick）。代价：目标死亡或走出范围后重新锁定最多慢 0.35 秒。

## 2. 供弹弹链：记住格子 + Curios 查询重试

`AmmoBeltHelper.feed` 在「枪没满弹」时每 tick 都跑，而交火中枪基本都不满：

```text
findBelt() → 36 格背包扫描；装 Curios 且背包没有弹链时，每 tick 再来一次 Curios capability 查询
```

现在：

- 命中后记住背包格号（`BELT_SLOT`），下个 tick 只验证这一格，失效才回退全量扫描；
- Curios 查空后 5 tick 内不再查询（`CURIO_RETRY`）；
- 玩家退出时清理这两个表。

代价：弹链放进 Curios 后最多晚 5 tick 生效。估算：交火中每个持枪玩家每 tick 从 36 次 `getItem` + 可能的 Curios 查询降到 1 次 `getItem` + 1 次 `instanceof`。

## 3. 动态箱渲染：纹理 id 按弹种缓存

`DynamicCrateRenderer.render` 每个可见动态箱每帧都做：两次 `AmmoTypes.byId`、两次字符串拼接构造 `ResourceLocation`（构造时会算哈希）、两次纹理存在性查询。

现在：按弹种缓存 `{top, label}` 两个 id（`TEXTURE_IDS`），资源重载监听器里一并清空；`renderFrontLabel` 直接用缓存好的 id，不再自己查表。估算：一墙 200 个箱、60 FPS 时，每秒少约 2.4 万次字符串拼接与对象分配。

## 4. 看过但没动的

- **机器 tick**：已有 idle cooldown、结果缓存、空输出不查邻居；`getCapability` 本身走的是 BlockEntity 自带的 LazyOptional，没有每 tick 重建。
- **装配机配方匹配**：每 tick 一次，配方量只有几十条，缓存反而要处理物品/配方的失效，收益不抵风险。
- **击发与换弹 mixin**：只在开枪/换弹事件里执行，不属于每 tick 路径。
- **`TimelessAPI.getCommonGunIndex` 每 tick 查询**：TACZ 内部就是 map 查找，包一层我们自己的缓存要在数据包重载时失效，暂不做。

## 5. 怎么验证

1. 单人：`/forge tps` 看 20 tick 均值，和 0.4.0-hotfix 对比；只看模组自身时用 spark 的 `--only-ticks-over` 之类过滤。
2. 多人：开服后让玩家空手站着（不用机架、不带弹链），spark 采样里 `SatelliteRackManager.onPlayerTick` 与 `AmmoBeltHelper.onPlayerTick` 的占比应明显下降。
3. 客户端：站到一墙动态箱前看 FPS 与 GC 日志（`-Xlog:gc`），对比 0.4.0-hotfix。
4. 功能回归：机架出枪/收枪、弹链补给、动态箱贴图都要照旧（见 `beta-test-checklist.md` 的 0.3.0 / 0.5.0 段）。
