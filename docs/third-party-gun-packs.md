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

移除枪包后执行 `/taczammobox scan` 清理配置，重启后再分发一次配置。
After removing a pack, run `/taczammobox scan`, restart, then redistribute the config.

运行中不能热删除已注册物品。
Registered items cannot be removed while the game is running.
