# 开服与整合包

服务端和客户端必须使用同一份 `ammo_boxes.json`，否则会缺物品/注册表不匹配。

## 登录校验

玩家登录后，服务端会把自己的弹种列表发给客户端：

- 客户端立即比对本地列表
- 不一致时聊天栏提示缺少/多出数量
- 客户端回传本地列表
- 服务端确认不一致后断开连接，并显示明确原因

如果 Forge 在登录阶段就因为注册表差异拒绝连接，这个校验来不及运行；仍然需要提前同步配置。

## 实际路径

| 位置 | 路径 |
|---|---|
| 服务端 | `<服务端根目录>/config/taczammobox/ammo_boxes.json` |
| 客户端 | `<客户端实例目录>/config/taczammobox/ammo_boxes.json` |
| 整合包项目（仅打包用） | `<项目根目录>/overrides/config/taczammobox/ammo_boxes.json` |

- CurseForge/Prism 客户端就是实例目录下的 `config`，**不是 `overrides`**。
- 单人游戏是 `.minecraft/config/taczammobox/`。
- `overrides` 只存在于整合包项目或导出包中；安装后会被铺到客户端 `config`。

## 服主步骤

1. 服务端和客户端安装相同版本的模组、TACZ 和枪包。
2. 服务端启动一次。
3. 有第三方枪包时执行 `/taczammobox scan`。
4. 重启服务端。
5. 把服务端的 `ammo_boxes.json` 和 `settings.json` 放到客户端实例的 `config/taczammobox/`。
6. 客户端重启。

## 整合包

```
项目根目录/
├─ manifest.json
└─ overrides/
   └─ config/
      └─ taczammobox/
         ├─ ammo_boxes.json
         └─ settings.json
```

`overrides` 是给 CurseForge/Modrinth 打包用的，安装后落到玩家的客户端 `config`。

## 注意

- 服务端执行过 `scan` 后，要重新分发配置。
- 不要在客户端单独 `scan` 后不同步。
- 修改机器速率或枪包后需要重启。
