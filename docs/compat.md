# 兼容性说明

## 1. 运行环境

| 项 | 要求 |
|---|---|
| Minecraft | 1.20.1 |
| Forge | 47.3.19（47.4.x 未完整验证） |
| TACZ | **1.2.0 / 1.2.0-hotfix**（硬依赖） |
| Create | 6.0.x（可选，仅部署器兼容与哨戒臂） |
| Curios | 5.14.x（可选，弹链与饰品搜索） |

> **1.2.0-hotfix 与 1.2.0 的差**（逐 class 做 SHA-256 比对得出）：
> - 改了 **15 个 class**，全部在客户端：`InteractKey`、`ClientPreventGunClick`、`BedrockGunModel`、`BedrockAttachmentModel`、`bedrock/BedrockModel|CubeBox|CubePerFace|Polygon`、`BulletHoleParticle`、`RenderConfig`、`RenderClothConfig`、`mixin/client/ar/BedrockPartMixin`；
> - 内置 `simplebedrockmodel` 从 2.6.2 升到 2.6.3。
>
> 本模组的 10 个 mixin 目标（`AbstractGunItem`、`AnimationStateMachine`、`AttachmentDataUtils`、`AttachmentsTagManager`、`GunData`、`LocalPlayerShoot`、`ModernKineticGunItem`、`SoundPlayManager` 与两个 Create 类）**都不在这 15 个里**，启动自检反射的类与方法也都没变，所以 0.5.0 不需要改代码。
>
> ⚠ **1.2.0-hotfix 与哨戒动力臂（sentrymechanicalarm 0.3.6）冲突**：`InteractKey.interactBlock` 从 3 个参数变成 4 个（多了 `boolean`），而 0.3.6 的 `InteractKeyMixin` 还按 3 个参数注入，启动时 mixin apply 失败、整个游戏加载崩溃。需要停用哨戒臂或等它的更新版；和本模组无关。
>
> `mods.toml` 的 `[1.2.0,)` 同时接受两个版本（Maven 版本序里 `1.2.0-hotfix > 1.2.0`，用 maven-artifact 实测过）。

## 2. 对 TACZ / Create 的注入点

本模组通过 mixin 修改少量第三方行为，全部受 `config/taczammobox/features.json` 控制。

| Mixin | 目标 | 作用 | 开关 |
|---|---|---|---|
| `AttachmentsTagMixin` | TACZ `AttachmentsTagManager` | 让所有拉栓枪接受全部自动击发弹匣（标准 + 彩蛋） | `autoFire` |
| `AttachmentDataUtilsMixin` | TACZ 容量计算 | 按档位返回弹容（标准 10/20/30、彩蛋 300） | `capacityOverride` |
| `GunDataShootIntervalMixin` | TACZ 射击间隔 | 按档位返回 60000/RPM | `rpmOverride` |
| `ModernKineticGunBoltMixin` | TACZ `startBolt` | 玩家跳过拉栓脚本（FakePlayer 不跳） | `autoFire` |
| `AnimationStateMachineMixin`（客户端） | TACZ 动画状态机 | 屏蔽 `blot` 拉栓动画 | `autoFire` |
| `SoundPlayManagerMixin`（客户端） | TACZ 音效 | 屏蔽拉栓音 | `autoFire` |
| `DeployerHandlerMixin` | Create 部署器 | 盒/箱倒入弹药盒 | `createCompat` |
| `DeployerBlockEntityMixin` | Create 部署器方块实体 | 同上 | `createCompat` |

> 除以上之外，不修改 TACZ 的换弹、伤害、弹道与枪包数据。

## 3. 开关与降级

`config/taczammobox/features.json`：

```json
{
  "autoFire": true,
  "capacityOverride": true,
  "rpmOverride": true,
  "belt": true,
  "easterEggTab": true,
  "createCompat": true,
  "safeMode": false
}
```

- `safeMode: true` 会关掉所有对其它模组的行为改动，物品与机器照常可用；
- 改动后执行 `/taczammobox reload`，或重启游戏；
- 启动时 `CompatSelfCheck` 会反射检查 TACZ 关键方法/字段，缺失时自动关闭 auto-fire 并打印警告；
- 运行中某个特性抛出意外异常时，`FeatureGuard` 只停用它自己并记一条日志，不会让游戏崩溃。

## 4. TACZ 更新后的处理顺序

1. 看日志 `TACZ compatibility check passed` 还是 `auto-fire disabled, missing TACZ API: ...`；
2. 若缺失，先开 `safeMode` 保证游戏可玩；
3. 把日志里的缺失项反馈给模组作者，等待适配版本。

## 5. 已知限制

- 自动击发只对 `manual_action`（拉栓）枪生效；
- 哨戒臂的射速由它自身状态机决定，接入我们的弹匣不会变成全自动；
- 自动击发弹匣与弹链属于「彩蛋」内容，**不提供合成表**；
- 弹匣与弹链没有专属模型，暂时借用 TACZ 的 `sniper_extended_mag_N` 与通用物品贴图。
