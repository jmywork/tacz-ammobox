# Capacity API / 容量接口

## Java API

Other addons can register a capacity provider during mod construction:

```java
import com.taczammobox.api.AmmoBoxCapacityApi;

AmmoBoxCapacityApi.register("examplemod", (box, stack, type, level, formula) -> {
    // Return null to fall through to the next resolver.
    // Return the capacity in rounds for this box.
    return switch (level) {
        case 0 -> 360;
        case 1 -> 720;
        case 2 -> 1200;
        default -> null;
    };
});
```

The provider receives:

- `IAmmoBox box` — the box implementation
- `ItemStack stack` — the actual box stack
- `AmmoType type` — TACZ ammo type being filled
- `int level` — 0 = iron, 1 = gold, 2 = diamond
- `int formula` — TACZ's normal formula result

Resolution order:

```
settings.json override
→ ammo_boxes.json data capacity
→ registered provider
→ DCSharp
→ automatic probe
→ TACZ formula
```

Provider exceptions are caught and logged; they never crash the fill path.

## Data-driven capacity / 数据驱动容量

Third-party ammo entries in `config/taczammobox/ammo_boxes.json` can define capacity for the standard TACZ ammo box:

```json
{
  "ammo": "modid:custom_ammo",
  "id": "custom_ammo",
  "name": "Custom Ammo",
  "capacity": [360, 720, 1200],
  "capacityMultiplier": [2.0, 2.0, 2.222]
}
```

- A number applies to all three levels.
- An array uses iron / gold / diamond order.
- `capacity` is absolute rounds.
- `capacityMultiplier` is applied to TACZ's formula.
- Missing or non-positive entries fall through.
- Changes require restart because item registration happens during mod construction.

This data applies to `tacz:ammo_box`; custom box **items** should use the Java provider API or `settings.json`.
