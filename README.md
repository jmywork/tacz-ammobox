# TACZ Ammo Box

TACZ（Timeless and Classics Zero）的弹药包装附属。把散装弹药装成盒、箱，并自动化装进 TACZ 弹药盒。

- Minecraft 1.20.1 / Forge 47.3.19
- 需要 TACZ 1.1.8-hotfix2
- Create 6.0.x 可选
- 协议 GPL-3.0-only
- 作者 jmywork
- 版本 `0.3.0`

English: [README.en.md](README.en.md)

容量 API 和数据驱动配置见 [docs/api.md](docs/api.md)。

## 从源码构建

见 [BUILDING.md](BUILDING.md)。

## 安装

1. 安装 Forge 1.20.1。
2. 安装 TACZ 1.1.8-hotfix2。
3. 把 `taczammobox-1.20.1-0.3.0.jar` 放进 `mods/`。
4. 想用 Create 部署器兼容再装 Create 6.0.x，不装也能正常玩。

## 快速开始

- **盒**：右键拆一组散弹；按 `V` 把它装进背包里的同类弹药盒。手持散装弹药按 `V` 也可以直接装入弹药盒，装不下的会留在手里。
- 散装弹药会从当前格继续找背包/精妙背包/Curios 里的同弹种，**能装多少装多少，不浪费**；盒/箱也会聚合同类包装，**浪费开启**时最后一包可部分浪费，**浪费关闭**时遇到放不下的整包就停止。
- 打开背包/容器时，把鼠标移到散装弹药、盒或箱上按 `V`，可以直接用这一格装填；主手同类物品用完后会自动从背包、精妙背包或 Curios 补到手上。自动补货可用 `settings.json` 的 `autoRefill` 关闭。
- **箱**：右键放置成方块；9 盒 ↔ 1 箱。
- **盒堆**：潜行右键方块放置；普通右键盒堆取一盒（空手进手、同种盒未满叠手、否则进背包）；潜行右键同种盒堆叠加；每堆最多 12 盒，破坏整堆按数量掉落。
- **一键打包**：按 `H`（可改键）或 `/taczammobox pack`，把玩家背包、Curios、当前打开容器（含精妙背包/箱子）里的满组散弹打包成新盒；不满一组会留在原位。
- **自动供弹弹链**：放在背包/Curios 中，主手枪低于满弹匣时自动从压缩弹药盒/箱补弹，不触发换弹；弹链耗尽或武器过热后恢复正常逻辑。当前为 0.3.0 原型，尚无生存配方。
- **装填优先级**：`settings.json` 的 `boxPriority` 可调整 `main_hand / inventory / curios / open_container` 的搜索顺序。
- **自检/重载配方**：`/taczammobox doctor` 检查配置和生成资源；`/taczammobox reloadrecipes` 重载数据包和第三方配方。
- **打包机**：散弹 → 盒、9 盒 → 箱；也可以反向拆包。GUI 有过滤槽，放入散弹/盒/箱可只处理该弹种。
- **过滤槽是标记槽**：左键放物品设置过滤，右键或空手点击清除；不消耗物品、不能取出、破坏机器也不掉落。
- **装填机**：把盒/箱/散弹装进 TACZ 弹药盒。GUI 有过滤槽，放入散弹/盒/箱可只处理该弹种。
- 盒/箱装不满时是否浪费，用 `/taczammobox waste true|false` 控制。
- `V` 可以在「控制 → TACZ 弹药盒装 → 装填弹药盒」里改键。

## 机器

**打包机**：铁锭、铁块、红石块 ×2、工作台、漏斗。

- 两槽：输入散弹/盒/箱，输出成品。
- GUI 切换「打包 / 拆包」。
- 六个面可分别设为输入、输出或关闭，默认底面输出、其余输入。
- 支持红石控制与自动上下料。
- 比较器输出：0 空闲、1–14 处理进度、15 输出槽有成品。

**装填机**：铁锭、铁砧、红石块、箱子 ×2、打包机。

- 三槽：输入弹药盒、料（盒/箱/散弹）、输出弹药盒。
- 弹药盒装满后才会进入输出槽。
- 同样支持六面配置、红石和自动上下料。
- 比较器输出：0 空闲、1–14 装填进度、15 输出槽有成品。

## 可选显示

安装 **Jade** 或 **WTHIT** 后，准星对准机器或第三方箱子可以看到：

- 打包机：模式、红石状态、当前进度
- 装填机：红石状态、当前进度
- 第三方箱子：弹种名称

两个都是可选的，不装不影响游戏。

## 兼容

- **精妙背包**：按 `V` 会搜索背包里的弹药盒
- **DCSharp**：自动读取它对铁/金/钻石弹药盒的 6/12/20 组容量修改
- **JEI**：提供打包机/装填机加工关系分类（可选）
- **Curios**：饰品栏里如果有弹药盒，也会被搜索
- 没有安装这些模组时，只搜索玩家普通背包

## 配置

`config/taczammobox/settings.json`：

```json
{
  "waste": true,
  "packerTicks": 5,
  "loaderTicks": 5
}
```

- `waste`：`true` 装不满也消耗并浪费剩余；`false` 必须整组/整包装得下。
- `packerTicks` / `loaderTicks`：每次操作耗时，**单位 tick，20 tick = 1 秒**，默认 `5`。
- 改完在服务器执行 `/taczammobox reload` 即可生效并同步客户端，或直接重启。
- 第三方盒子容量异常时，手持该盒子执行 `/taczammobox capacity held <容量>` 即可写入覆盖，不用手改 JSON。

详细说明见 [docs/config.md](docs/config.md)。

## 第三方枪包

1. 服务端和客户端都安装同一个枪包。
2. 服务端启动后执行 `/taczammobox scan`。
3. 重启服务端。
4. 把服务端的 `ammo_boxes.json` 放到客户端实例的 `config/taczammobox/`；做整合包时才放进 `overrides/config/taczammobox/`（整合包安装后会铺到客户端 `config`）。
5. 客户端重启。

服务端和客户端必须使用同一份配置，否则会出现缺物品/注册表不匹配。完整步骤见 [docs/third-party-gun-packs.md](docs/third-party-gun-packs.md)。

## 指令

| 指令 | 权限 | 作用 |
|---|---|---|
| `/taczammobox help` | 任意 | 查看常用指令和配置位置 |
| `/taczammobox dump` | 2 | 导出当前弹种到 `detected_ammo.json` |
| `/taczammobox pack` | 任意 | 满组散弹打包成新盒 |
| `/taczammobox packtoggle [true\|false]` | 2 设置 | 开关一键打包 |
| `/taczammobox scan` | 2 | 只追加缺失弹种，不删除已有条目，重启生效 |
| `/taczammobox clean` | 2 | 直接清理失效弹种条目 |
| `/taczammobox cleanup [半径]` | 2 | 直接清理玩家/容器/掉落物里的孤儿盒，默认 64 |
| `/taczammobox reload` | 2 | 重读 `settings.json` 并同步客户端 |
| `/taczammobox autorefill` | 任意 | 查看自动补货开关 |
| `/taczammobox autorefill <true\|false>` | 2 | 设置自动补货开关 |
| `/taczammobox capacity held <容量>` | 2 | 设置主手盒子容量（带弹种） |
| `/taczammobox capacity heldlevel <容量>` | 2 | 设置主手盒子当前等级容量（所有弹种） |
| `/taczammobox capacity heldmult <倍率>` | 2 | 设置主手盒子当前等级容量倍率 |
| `/taczammobox waste` | 任意 | 查看浪费开关 |
| `/taczammobox waste <true\|false>` | 2 | 设置盒/箱装不满时是否浪费 |

详细说明见 [docs/commands.md](docs/commands.md)。

## 开服

登录时服务端会比对双方弹种列表，不一致会断开并提示缺少/多出数量。

双端实际路径：

- 服务端：`<服务端根目录>/config/taczammobox/ammo_boxes.json`
- 客户端：`<客户端实例目录>/config/taczammobox/ammo_boxes.json`
  - CurseForge/Prism 实例里就是实例目录下的 `config`，**不是 `overrides`**
  - 单人游戏是 `.minecraft/config/taczammobox/`
- `overrides/config/taczammobox/` 只是**做整合包时的项目目录**，安装后会被铺到上面的客户端 `config`

把验证过的 `ammo_boxes.json` 和 `settings.json` 按上面的路径放到两端，让所有玩家使用同一份配置。

见 [docs/server-setup.md](docs/server-setup.md)。

## 反馈

遇到问题请附 `logs/latest.log`、TACZ/Create/枪包版本和复现步骤。
