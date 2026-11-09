# 故障排查

先看日志，再对症状。所有与本模组相关的日志都带 `[taczammobox]` 前缀。

## 1. 启动阶段

| 现象 | 可能原因 | 处理 |
|---|---|---|
| 启动日志 `auto-fire disabled, missing TACZ API: ...` | TACZ 版本不匹配，关键方法/字段被改名 | 按日志缺失项判断；先设 `safeMode: true` 保证可玩，再等适配 |
| `features: ...` 显示全 false | `features.json` 被写成全关，或 `safeMode: true` | 改回 true，执行 `/taczammobox reload` |
| 枪包目录缺少 `tacz/taczammobox_autofire` | `autoFire` 为 false，或游戏目录不可写 | 确认开关；检查日志里的 `Wrote TACZ gun pack file` |
| 游戏崩溃且堆栈指向 `mods.toml` | 文件编码损坏（BOM） | 重新解压模组 jar，不要用文本编辑器改 mods.toml |

## 2. 自动击发弹匣

| 现象 | 检查 |
|---|---|
| 改枪界面找不到三档弹匣 | 枪是否为拉栓枪（`manual_action`）；日志是否有枪包扫描记录；是否刚启动就打开界面（枪包未加载完） |
| 装上了但射速没变 | `features.json` 的 `rpmOverride` 是否被关；`autofire.json` 是否被改成原版数值 |
| 装上了但弹容不对 | `capacityOverride` 是否被关；`autofire.json` 的 `capacity` 是否生效（改完需 `/taczammobox reload`） |
| 打一发就停 | 弹匣是否打空；膛内状态是否被其它模组改动；看日志有无 `Feature '...' disabled` |
| 无拉栓动画但有声音 | `autoFire` 开着但客户端音效 mixin 未生效，检查客户端日志 |

## 3. 自动供弹弹链

| 现象 | 检查 |
|---|---|
| 弹链不补弹 | 是否放背包或 Curios；主手是否为 TACZ 枪；`features.json` 的 `belt` 是否为 true |
| 只补到原版弹容就不动了 | 0.3.0 之前的老版本行为；0.4.0 起按弹匣档位弹容补（标准 10/20/30、彩蛋 300） |
| 弹链储备不增长 | `settings.json` 的 `beltMaxStore` 是否被设成了较小值（0 = 不限） |
| 弹链吃掉了整盒但只补几发 | 正常：余弹存在弹链 NBT，下次继续用 |

## 4. 哨戒动力臂

| 现象 | 检查 |
|---|---|
| 只打一发就停 | 0.3.0 修复过；确认版本 ≥ 0.3.0，且没有开 `safeMode` |
| 能打但很慢 | 正常：哨戒臂按自身拉栓节奏开火，不是全自动 |
| 不消耗弹匣 | 确认枪上装的是我们的弹匣；哨戒臂自身的弹药盒逻辑与弹匣是两套 |

## 5. 机器与配方

| 现象 | 检查 |
|---|---|
| JEI 里看不到加工关系 | 是否安装 JEI；分类由 `TaczAmmoBoxJeiPlugin` 注册 |
| 过滤槽物品被消耗 | 标记槽不应消耗；若发生，检查是否用了自动输入把物品塞进了过滤槽 |
| 机器不进料 | 六面配置是否把该面设为输入；红石信号是否符合设置 |

## 6. 常用指令

```text
/taczammobox doctor          配置、资源、特性与守卫状态
/taczammobox reload          重载 settings.json / features.json / autofire.json 并重生成枪包
/taczammobox reloadrecipes   重载数据包与第三方配方
/taczammobox scan            扫描新增弹种
/taczammobox clean           清理失效弹种条目
```

## 7. 反馈时请附上

- `logs/latest.log`（含 `[taczammobox]` 的行）；
- 游戏版本、Forge 版本、TACZ 版本、Create/Curios 版本；
- 复现步骤与是否使用 `safeMode`；
- 若涉及弹匣，附 `config/taczammobox/features.json` 与 `autofire.json`。
