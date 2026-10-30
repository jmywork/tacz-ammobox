# Building / 构建

## Requirements / 需要

- JDK 17
- Gradle 8.1.1
- Minecraft Forge 47.3.19 (Gradle will download it)
- TACZ 1.1.8-hotfix2 jar
- Optional compile-only jars:
  - Jade 11.x
  - WTHIT 8.x
  - Curios 5.x

## Steps / 步骤

1. Put the TACZ jar and optional jars into `libs/`.
   - `libs/tacz-1.20.1-1.1.8-hotfix2.jar`
   - `libs/Jade-1.20.1-Forge-11.13.3.jar`
   - `libs/wthit-1.20.1-forge-8.21.1.jar`
   - `libs/curios-forge-5.14.1+1.20.1.jar`
2. Run:

```bash
gradle build
```

3. The jar is written to `build/libs/`.

The `libs/*.jar` files are intentionally ignored by Git; they are local build dependencies, not redistributed.

`libs/*.jar` 是本地编译依赖，已被 `.gitignore` 忽略，不随仓库分发。
