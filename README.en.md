# TACZ Ammo Box

Ammo packaging for **Timeless and Classics Zero (TACZ)**: pack loose ammo into boxes and crates, then fill TACZ ammo boxes automatically.

- Minecraft 1.20.1 / Forge 47.3.19
- TACZ 1.1.8-hotfix2
- Create 6.0.x optional
- GPL-3.0-only
- Author jmywork
- Version `0.1.0-beta`

中文: [README.md](README.md)

## Install

1. Install Forge 1.20.1.
2. Install TACZ 1.1.8-hotfix2.
3. Put `taczammobox-1.20.1-0.1.0-beta.jar` into `mods/`.
4. Create is optional; install it only if you want deployer support.

## Quick start

- **Box**: right-click to unpack one stack; press `V` to fill a matching TACZ ammo box. Holding loose ammo and pressing `V` fills the box directly; any remainder stays in your hand.
- Loose ammo keeps pulling matching stacks from your inventory, Sophisticated Backpacks and Curios: it fills as much as possible and **never wastes**. Boxes/crates also aggregate matching packages: with waste **on** the last package may be partially wasted; with waste **off** filling stops before a package that does not fit.
- With an inventory/container open, hover loose ammo, a box or a crate and press `V` to fill from that slot. When the held stack runs out, the same item is pulled from your inventory, Sophisticated Backpacks or Curios. Disable this with `autoRefill` in `settings.json` if another refill mod conflicts.
- **Crate**: right-click to place; 9 boxes ↔ 1 crate.
- **Ammo Packer**: loose ammo → box, 9 boxes → crate, or unpack.
- **Ammo Loader**: fills TACZ ammo boxes from boxes, crates or loose ammo.
- `/taczammobox waste true|false` controls whether partially fitting packages are wasted.
- `V` can be rebound in Controls → TACZ Ammo Box → Fill Ammo Box.

## Machines

**Ammo Packer**: iron ingots, iron block, 2 redstone blocks, crafting table, hopper.

- Two slots: input ammo/boxes/crates, output result.
- GUI switches between Pack and Unpack.
- Six independent faces: input, output or disabled. Default: bottom output, others input.
- Redstone control and auto I/O.
- Comparator: 0 idle, 1–14 progress, 15 output ready.

**Ammo Loader**: iron ingots, anvil, redstone block, 2 chests, ammo packer.

- Three slots: ammo box in, source, ammo box out.
- The box moves to the output only when it is full.
- Same per-face configuration, redstone and auto I/O.
- Comparator: 0 idle, 1–14 progress, 15 output ready.

## Optional overlays

With **Jade** or **WTHIT** installed, look at a machine or a third-party crate to see:

- Ammo Packer: mode, redstone state, progress
- Ammo Loader: redstone state, progress
- Third-party crate: ammo type

Both are optional; the mod works without them.

## Compatibility

- **Sophisticated Backpacks**: `V` also searches ammo boxes inside backpacks
- **DCSharp**: reads its custom iron/gold/diamond box stack counts automatically
- **Curios**: ammo boxes in equipped curio slots are searched too
- Without these mods, only the normal player inventory is searched

## Config

`config/taczammobox/settings.json`:

```json
{
  "waste": true,
  "packerTicks": 5,
  "loaderTicks": 5
}
```

- `waste`: `true` = consume a partially fitting package and discard the remainder; `false` = require the whole package to fit.
- `packerTicks` / `loaderTicks`: ticks per operation, **20 ticks = 1 second**, default `5`.
- Run `/taczammobox reload` on the server after editing (it also syncs clients), or restart.
- If a custom box has the wrong capacity, hold it and run `/taczammobox capacity held <capacity>`; no JSON editing needed.

See [docs/config.md](docs/config.md).

## Third-party gun packs

1. Install the same pack on server and clients.
2. Start the server and run `/taczammobox scan`.
3. Restart the server.
4. Put the server's `ammo_boxes.json` into each client instance at `config/taczammobox/`; only use `overrides/config/taczammobox/` when building a modpack (it is copied into the client `config` on install).
5. Restart clients.

Client and server must use the same config, or items will be missing. See [docs/third-party-gun-packs.md](docs/third-party-gun-packs.md).

## Commands

| Command | Permission | Description |
|---|---|---|
| `/taczammobox help` | any | Show common commands and config path |
| `/taczammobox dump` | 2 | Export loaded ammo types to `detected_ammo.json` |
| `/taczammobox scan` | 2 | Add missing ammo and prune removed packs; restart required |
| `/taczammobox reload` | 2 | Reload `settings.json` and sync it to clients |
| `/taczammobox autorefill` | any | Show the auto-refill toggle |
| `/taczammobox autorefill <true\|false>` | 2 | Set the auto-refill toggle |
| `/taczammobox capacity held <capacity>` | 2 | Override the held box's capacity (with ammo) |
| `/taczammobox capacity heldlevel <capacity>` | 2 | Override the held box's current level (all ammo) |
| `/taczammobox capacity heldmult <multiplier>` | 2 | Set the held box's current-level capacity multiplier |
| `/taczammobox waste` | any | Show the waste setting |
| `/taczammobox waste <true\|false>` | 2 | Change the waste setting |

See [docs/commands.md](docs/commands.md).

## Server setup

On login the server compares both ammo lists; a mismatch disconnects with a missing/extra count message.

Actual paths:

- Server: `<server root>/config/taczammobox/ammo_boxes.json`
- Client: `<client instance>/config/taczammobox/ammo_boxes.json`
  - CurseForge/Prism instances use the instance's `config`, **not `overrides`**
  - Singleplayer: `.minecraft/config/taczammobox/`
- `overrides/config/taczammobox/` is only the **modpack project** staging folder; it is copied into the client `config` on install.

Put the validated `ammo_boxes.json` and `settings.json` at the paths above on both sides.

See [docs/server-setup.en.md](docs/server-setup.en.md).

## Feedback

Include `logs/latest.log`, TACZ/Create/gun pack versions and reproduction steps.
