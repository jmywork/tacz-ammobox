# Server & Modpack Setup

The server and every client must use the same `ammo_boxes.json`, or items will be missing / the registry will mismatch.

## Login check

After login the server sends its ammo list to the client:

- The client compares it with its local list
- A mismatch shows missing/extra counts in chat
- The client replies with its local list
- The server disconnects with a clear reason if the lists differ

If Forge rejects the connection during login because of a registry difference, this check never runs; sync the config in advance.

## Actual paths

| Side | Path |
|---|---|
| Server | `<server root>/config/taczammobox/ammo_boxes.json` |
| Client | `<client instance>/config/taczammobox/ammo_boxes.json` |
| Modpack project (packaging only) | `<project>/overrides/config/taczammobox/ammo_boxes.json` |

- CurseForge/Prism clients use the instance's `config`, **not `overrides`**.
- Singleplayer: `.minecraft/config/taczammobox/`.
- `overrides` only exists in a modpack project/export; on install it is copied into the client `config`.

## Server owner steps

1. Install the same mod, TACZ and gun pack versions on the server and clients.
2. Start the server once.
3. With third-party packs, run `/taczammobox scan`.
4. Restart the server.
5. Put the server's `ammo_boxes.json` and `settings.json` into each client instance's `config/taczammobox/`.
6. Restart clients.

## Modpack

```
project/
├─ manifest.json
└─ overrides/
   └─ config/
      └─ taczammobox/
         ├─ ammo_boxes.json
         └─ settings.json
```

`overrides` is for CurseForge/Modrinth packaging; on install it goes into the player's client `config`.

## Notes

- After running `scan` on the server, redistribute the config.
- Do not run `scan` only on a client and leave it unsynced.
- Restart after changing machine speed or gun packs.
