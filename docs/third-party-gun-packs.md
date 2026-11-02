# 第三方枪包 / Third-party Gun Packs

## 中文

1. 服务端和客户端都安装**同一个枪包**。
2. 启动服务端，执行 `/taczammobox scan`。
3. 重启服务端。
4. 把服务端的 `ammo_boxes.json` 放到客户端实例的 `config/taczammobox/`：
   - 客户端实例：`<实例目录>/config/taczammobox/`（不是 `overrides`）
   - 做整合包时才放进 `overrides/config/taczammobox/`，安装后会铺到客户端 `config`
   - 服务器包：放进客户端配置
5. 客户端重启。
6. 再执行一次 `scan`，确认没有缺失弹种。

**服务端和客户端必须使用同一份 `ammo_boxes.json`，否则会缺物品或注册表不匹配。**

`scan` 只追加，不删除已有条目。每个第三方条目应保持 `itemName` 不变；它是盒子/箱子的固定物品 ID 后缀。

`scan` only appends entries. Keep each third-party entry's `itemName` unchanged; it is the fixed item-id suffix for boxes/crates.

第三方弹种会自动生成盒/箱模型、标签和顶面图标；箱子可以放置，破坏后掉回对应弹种。

## English

1. Install the **same gun pack** on the server and every client.
2. Start the server and run `/taczammobox scan`.
3. Restart the server.
4. Put the server's `ammo_boxes.json` into each client instance at `config/taczammobox/`:
   - Client instance: `<instance>/config/taczammobox/` (not `overrides`)
   - Only use `overrides/config/taczammobox/` when building a modpack; it is copied into the client `config` on install
   - Server pack: include it in the client config
5. Restart clients.
6. Run `scan` again and confirm nothing is missing.

**The client and server must use the same `ammo_boxes.json`, or items will be missing / the registry will mismatch.**

Third-party types get generated box/crate models, labels and top icons. Their crates are placeable and drop the correct item when broken.

## 移除枪包 / Removing a pack

`/taczammobox scan` **不会**删除已移除枪包的条目。  
默认保留条目，旧盒子/箱子仍然是物品，但会因为 TACZ 找不到弹种而成为孤儿。

推荐顺序：

1. 先不要执行 `clean`
2. `/taczammobox cleanup` 清理玩家背包、Curios、已加载容器和掉落物里的孤儿盒/箱
3. `/taczammobox clean` 删除失效条目
4. 重启

`/taczammobox cleanup` 默认半径 64，也可以写 `/taczammobox cleanup 128`。

`scan` never removes entries. Removed gun packs leave orphan entries whose boxes/crates still exist but cannot be filled.

Recommended order:

1. Do not run `clean` yet
2. `/taczammobox cleanup`
3. `/taczammobox clean`
4. Restart

`/taczammobox cleanup` defaults to radius 64; use `/taczammobox cleanup 128` for a larger area.

运行中不能热删除已注册物品。
Registered items cannot be removed while the game is running.
