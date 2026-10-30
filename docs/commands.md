# Commands / 指令

所有指令以 `/taczammobox` 开头。

| 指令 | 权限 | 作用 |
|---|---|---|
| `/taczammobox help` | 任意 | 显示常用指令和配置文件位置 |
| `/taczammobox dump` | 2 | 导出当前加载的弹种到 `config/taczammobox/detected_ammo.json` |
| `/taczammobox scan` | 2 | 补充缺失弹种、清理已移除枪包的条目；**重启后生效** |
| `/taczammobox reload` | 2 | 重读 `settings.json` 并同步给在线客户端 |
| `/taczammobox autorefill` | 任意 | 查看主手自动补货开关 |
| `/taczammobox autorefill <true\|false>` | 2 | 设置主手自动补货开关 |
| `/taczammobox capacity held <容量>` | 2 | 设置主手弹药盒当前等级 + 当前弹种的容量 |
| `/taczammobox capacity heldlevel <容量>` | 2 | 设置主手弹药盒当前等级的容量（所有弹种） |
| `/taczammobox capacity heldmult <倍率>` | 2 | 设置主手弹药盒当前等级的容量倍率 |
| `/taczammobox capacity rounding <round\|floor\|ceil>` | 2 | 设置小数倍率的取整方式 |
| `/taczammobox capacity mult <物品> <等级> <倍率>` | 2 | 设置指定物品/等级的容量倍率 |
| `/taczammobox capacity multall <物品> <倍率>` | 2 | 设置该物品所有等级的容量倍率 |
| `/taczammobox capacity setammomult <物品> <等级> <弹种> <倍率>` | 2 | 设置精确到弹种的容量倍率 |
| `/taczammobox capacity set <物品> <等级> <容量>` | 2 | 设置指定物品/等级的容量覆盖（所有弹种） |
| `/taczammobox capacity setammo <物品> <等级> <弹种> <容量>` | 2 | 设置精确到弹种的容量覆盖 |
| `/taczammobox capacity setall <物品> <容量>` | 2 | 设置该物品所有等级的容量覆盖 |
| `/taczammobox capacity clear <物品> <等级>` | 2 | 移除指定物品/等级的覆盖 |
| `/taczammobox capacity clearammo <物品> <等级> <弹种>` | 2 | 移除精确到弹种的覆盖 |
| `/taczammobox capacity clearall <物品>` | 2 | 移除该物品所有等级的覆盖 |
| `/taczammobox capacity list` | 2 | 列出当前容量覆盖 |
| `/taczammobox waste` | 任意 | 查看浪费开关 |
| `/taczammobox waste <true\|false>` | 2 | 设置盒/箱装不满时是否浪费 |

## 说明

- `scan` 会修改 `ammo_boxes.json`，新物品需要重启后才会出现。
- `reload` 只重读 `settings.json`（浪费开关、机器速率），不改变物品注册。
- `capacity held` 会按主手盒子的物品 id + 当前等级 + 当前弹种写入；`capacity heldlevel` 只按物品 id + 等级写。
- 第三方作者通常改的是等级容量倍率，优先用 `heldmult` / `mult`，这样所有弹种会自动适配；自动探测会写物品级倍率。
- 1.25 这类小数倍率可用；如果作者用 floor/ceil 而不是四舍五入，用 `capacity rounding` 设置取整方式。
- `capacity clear` / `clearall` 会同时移除绝对容量和倍率覆盖。
- `capacity setall` 会把同一容量套到所有等级/弹种，只适合容量固定的自定义盒子。
- 服务器上只需在服务端执行 `scan`，然后把配置发给客户端。
- `dump` 只导出参考文件，不修改配置。

---

All commands start with `/taczammobox`.

| Command | Permission | Description |
|---|---|---|
| `/taczammobox help` | any | Show common commands and the config path |
| `/taczammobox dump` | 2 | Export loaded ammo types to `config/taczammobox/detected_ammo.json` |
| `/taczammobox scan` | 2 | Add missing ammo and prune removed packs; **restart required** |
| `/taczammobox reload` | 2 | Reload `settings.json` and sync it to online clients |
| `/taczammobox autorefill` | any | Show the main-hand auto-refill toggle |
| `/taczammobox autorefill <true\|false>` | 2 | Set the main-hand auto-refill toggle |
| `/taczammobox capacity held <capacity>` | 2 | Override the held box's current level + ammo capacity |
| `/taczammobox capacity heldlevel <capacity>` | 2 | Override the held box's current level for all ammo |
| `/taczammobox capacity heldmult <multiplier>` | 2 | Set the held box's current-level capacity multiplier |
| `/taczammobox capacity rounding <round\|floor\|ceil>` | 2 | Set fractional-capacity rounding |
| `/taczammobox capacity mult <item> <level> <multiplier>` | 2 | Set one item/level capacity multiplier |
| `/taczammobox capacity multall <item> <multiplier>` | 2 | Set all levels of one item to a multiplier |
| `/taczammobox capacity setammomult <item> <level> <ammo> <multiplier>` | 2 | Set one item/level/ammo multiplier |
| `/taczammobox capacity set <item> <level> <capacity>` | 2 | Override one item/level capacity (all ammo) |
| `/taczammobox capacity setammo <item> <level> <ammo> <capacity>` | 2 | Override one item/level/ammo capacity |
| `/taczammobox capacity setall <item> <capacity>` | 2 | Override all levels of one item |
| `/taczammobox capacity clear <item> <level>` | 2 | Remove one item/level override |
| `/taczammobox capacity clearammo <item> <level> <ammo>` | 2 | Remove one item/level/ammo override |
| `/taczammobox capacity clearall <item>` | 2 | Remove all overrides for one item |
| `/taczammobox capacity list` | 2 | List current capacity overrides |
| `/taczammobox waste` | any | Show the waste setting |
| `/taczammobox waste <true\|false>` | 2 | Change the waste setting |

## Notes

- `scan` edits `ammo_boxes.json`; new items only appear after a restart.
- `reload` only re-reads `settings.json` (waste and machine speed); it does not change item registration.
- `capacity held` writes the held box's item id + current level + current ammo; `capacity heldlevel` writes only item id + level.
- Third-party authors usually change a level's capacity multiplier; prefer `heldmult` / `mult` so every ammo type adapts. Automatic detection writes an item-wide multiplier.
- Decimal multipliers such as 1.25 work; if the author uses floor/ceil instead of rounding, set it with `capacity rounding`.
- `capacity clear` / `clearall` remove both absolute and multiplier overrides.
- `capacity setall` applies one capacity to all levels/ammo types; only use it for fixed-capacity custom boxes.
- On a server, run `scan` on the server and distribute the config to clients.
- `dump` only exports a reference file; it does not change the config.
