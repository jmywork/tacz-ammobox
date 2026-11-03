# Changelog

## 0.3.0 (development)

### 新增

- 自动供弹弹链 `taczammobox:ammo_belt` 原型
- 主手 TACZ 枪低于满弹匣时从背包/Curios 的压缩弹药盒/箱自动补弹
- 不触发换弹动作、不按 R；弹匣不归零
- 内部储备缓存：整盒/整箱拆出的余弹保存在弹链 NBT，不会因为弹匣容量小浪费
- 弹链耗尽后恢复正常换弹
- 武器过热锁定后停止自动供弹
- 目前仅创造模式/指令获得，配方材料待定

## 0.2.0

0.2.0 正式版。

### 新增

- 一键打包：`H` / `/taczammobox pack` 把满组散弹打包成新盒
- 一键打包开关：`oneKeyPack` + `/taczammobox packtoggle`，默认开启
- 打包机/装填机弹种过滤槽
- 过滤槽改为标记槽：不消耗物品、不可取出、破坏不掉落
- JEI 打包机/装填机加工关系分类
- 容量 Provider API（`AmmoBoxCapacityApi`）
- `ammo_boxes.json` 数据驱动容量：`capacity` / `capacityMultiplier`
- 自动探测按等级写入 `物品#等级` 倍率
- 固定第三方 `itemName`，`scan` 只追加
- `/taczammobox clean` 直接清理失效条目
- `/taczammobox cleanup [半径]` 直接清理孤儿盒/箱
- `/taczammobox doctor` 自检
- `/taczammobox reloadrecipes` 配方重载
- 盒/箱最大堆叠默认 64，可配置 1–64
- 旧打包机 2 槽 / 装填机 3 槽 NBT 自动迁移

### 修复

- 自动输入误填过滤槽并消耗 1 个物品
- 一键打包每格生成一盒、不与已有盒子堆叠
- 旧机器 NBT 槽位数量导致 `Slot 2 not in valid range`
- 装填机/打包机界面枚举 switch 合成类导致的 `NoClassDefFoundError`
- 过滤槽标记不能取出/掉落

### 升级说明

1. 关闭游戏/服务器
2. 替换为 `taczammobox-1.20.1-0.2.0.jar`
3. 不要删除 `config/taczammobox/`
4. 服务端和所有客户端必须一起更新
5. 正常更新不要执行 `clean` / `cleanup`

## 0.1.0-beta

首个内测版本。

### 新增

- TACZ 默认 24 种弹药的盒/箱包装
- `config/taczammobox/ammo_boxes.json` 支持第三方枪包弹种
- `/taczammobox scan` 自动补充新弹种、清理已移除弹种
- `/taczammobox dump` 导出当前弹种
- 9 盒 ↔ 1 箱、1 盒 → 一组散弹配方
- 配方书解锁：74 个配方 advancement
- 按 `stack_size` 计算的盒容量
- `V` 键装填弹药盒：支持盒/箱和散装弹药（可改键）
- 统一浪费开关 `/taczammobox waste <true|false>`
- 打包机：打包/拆包、GUI、六面配置、红石控制、自动上下料
- 装填机：三槽、GUI、六面配置、红石控制、自动上下料
- 打包机/装填机比较器输出（0 空闲、1–14 进度、15 有成品）
- 打包机产物缓存，避免每 tick 重建/分配
- 自动上下料改为批量搬运，减少 `IItemHandler` 调用
- 动态箱 BER 纹理缓存，避免每帧查资源
- 生成资源包增量判断，未变化时跳过重建
- 空闲机器降频到每 10 tick，库存/红石变化唤醒
- 登录时校验双端弹种配置，不一致明确断开
- `V` 键手动填充增加 2 tick 服务端冷却，防连点刷包
- 背包界面悬停弹药按 `V` 直接装填；主手耗尽自动补货
- 散装弹药装填会聚合背包/精妙背包/Curios 同类弹药，不浪费
- 兼容第三方修改容量的弹药盒：自动探测 + `ammoBoxCapacity` 覆盖
- 修复主背包/主手弹药盒在背包界面装填不生效（改为直接写回玩家背包）
- 修复精妙背包装备在饰品栏/背部时，内部弹药盒无法装填
- 新增 A5：按 `H` 或 `/taczammobox pack` 一键把背包、Curios、当前打开容器里的满组散弹打包成新盒
- 新增 A6：装填目标自动跳过满盒、弹种不匹配盒和创造模式盒
- 新增 C1：公开容量 Provider API（`AmmoBoxCapacityApi`）
- 新增 C2：`ammo_boxes.json` 支持 `capacity` / `capacityMultiplier` 按等级数据配置
- 新增 C3：自动探测按 `物品#等级` 写入倍率
- 新增 C7：`/taczammobox doctor` 自检
- 新增 C8：`/taczammobox reloadrecipes` 热重载配方
- 固定第三方 `itemName`，`scan` 只追加；`clean` 直接删除失效条目；`doctor` 报告孤儿条目
- 新增 `oneKeyPack` 开关和 `/taczammobox packtoggle`，默认开启一键打包
- 盒/箱最大堆叠默认改为 64，可通过 `boxStackSize` / `crateStackSize` 配置为 1–64
- 自动探测：干净倍数写物品级倍率，非整数倍写绝对容量，避免取整误差
- 支持 1.25 等小数倍率，新增 `capacityRounding` 和 `capacity rounding` 指令
- 配置文件内嵌中英示例，新增 `/taczammobox help`
- `autoRefill` 开关，可关闭主手自动补货以兼容其他补货模组
- 新增 `/taczammobox autorefill [true|false]` 指令
- 盒/箱装填也会聚合背包/精妙背包/Curios 同类包装，按浪费开关决定最后一包
- 新增容量倍率配置与 `heldmult` / `mult` / `multall` / `setammomult` 指令
- 新增 `/taczammobox capacity` 指令，可直接设置/清理/查看容量覆盖
- 容量覆盖支持 `物品#等级#弹种`，兼容 TACZ 铁/金/钻石三档和不同弹种容量
- 新增 `capacity heldlevel`，只按当前等级写容量
- 第三方配方改为数据包生成，运行时注入作为回退
- 机器速率可配置：`packerTicks` / `loaderTicks`（单位 tick，20 tick = 1 秒）
- `/taczammobox reload` 热重载 `settings.json` 并同步客户端
- 可选 Jade / WTHIT 显示机器模式、红石、进度和动态箱弹种
- `V` 键搜索精妙背包和 Curios 饰品栏里的弹药盒
- 中英双文说明文档：配置、指令、第三方枪包、服务端同步
- Create 部署器兼容（盒/箱灌弹药盒）
- 第三方弹种自动模型、正面标签、顶面图标
- 第三方箱动态可放置方块，弹种存方块实体，破坏掉回对应箱
- 中英文语言、创造模式标签页、GPL-3.0-only

### 已知限制

- 未在专用服务器上完成完整回归
- 第三方配方通过运行时注入，依赖资源重载顺序
- 第三方弹种图标依赖枪包 `textures/ammo/slot/<id>.png`
- 第三方箱子的方块模型是通用木箱 + BER 标签/图标
- 配置需要在服务端和客户端手动同步
