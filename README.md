# MiningTop — 服务端挖掘统计 Mod

> 统计每一名玩家挖掘的每一个方块。

MiningTop 是一个运行在**服务端**的 Forge mod（客户端无需安装）：玩家每破坏一个方块就计数 +1，数据随世界存档自动保存，并把计数实时同步到原版计分板，可以在头顶、侧边栏或 Tab 列表中展示。适合挖矿比赛、生存服务器排行榜等场景。

- **支持版本**：Minecraft **1.7.10** 与 **1.12.2**（两个独立项目，功能一致）
- **安装方式**：服务端 `mods/` 放入对应版本的 jar 即可；单人/LAN 模式放进 `.minecraft/mods/` 也能用

## 特点

- **什么都数**：生存、创造、任意维度、原版和 mod 方块一律计数（爆炸、燃烧等非玩家破坏不计入）
- **纯服务端**：`acceptableRemoteVersions="*"`，客户端什么都不用装
- **数据可靠**：计数存在主世界 `data/miningtop_data.dat`，按 UUID 记录——重启服务器不丢、玩家改名不受影响
- **分类统计**：除了总数，还记录每种方块各挖了多少（如石头 ×1234）
- **接入原版计分板**：数据同步到名为 `mined` 的 dummy 目标，可用原版 `/scoreboard` 指令直接操作
- **近零开销**：单次挖掘的处理量在微秒级，不写日志、不额外刷盘，对 TPS 无可感知影响
- **中英双语**：命令输出跟随玩家客户端语言

## 显示效果

计数默认显示在玩家**头顶名牌下方**（belowName 模式），OP 可随时用 `/mset` 切换：

| 位置 | 效果 |
|---|---|
| `belowName` | 玩家名字下方常驻数字（默认） |
| `sidebar` | 屏幕右侧排行榜，自动按数值排序，`/mnick` 的昵称在这里生效 |
| `list` | Tab 玩家列表中名字后面显示数字 |
| `off` | 不显示（数据照常统计） |

## 命令

| 命令 | 权限 | 说明 |
|---|---|---|
| `/mcount [玩家]` | 所有人 | 查询挖掘统计（总数 + 最常挖的 5 种方块），支持查离线玩家 |
| `/mtop [页码]` | 所有人 | 全服挖掘排行榜，每页 10 人 |
| `/mname [标题\|reset]` | OP | 修改计分栏标题（侧边栏顶部 / belowName 的名字），`reset` 恢复默认"挖掘数" |
| `/mnick <玩家> [名字\|clear]` | OP | 设置指定玩家（含离线）的昵称，用于侧边栏和排行榜；不填名字为查询 |
| `/mset <belowName\|sidebar\|list\|off>` | OP | 切换计数显示位置，默认 `belowName` |
| `/mreset <玩家\|all>` | OP | 重置某玩家或全部人的统计 |

示例：

```
/mname 挖矿大赛          ← 把计分栏标题改成"挖矿大赛"
/mset sidebar            ← 切到右侧边栏显示
/mnick Steve 挖矿王      ← 给 Steve 设昵称
/mtop                    ← 看排行榜
```

## 产物与构建

两个版本各自的构建产物：

| 目录 | 产物 |
|---|---|
| `./1.7.10/` | `build/libs/miningtop-1.7.10-1.3.0.jar` |
| `./1.12.2/` | `build/libs/miningtop-1.12.2-1.3.0.jar` |

**1.7.10**：Forge `1.7.10-10.13.4.1614`，mappings `stable_12`，Java 6 语法
**1.12.2**：Forge `1.12.2-14.23.5.2847`，mappings `stable_39`，Java 8 语法

构建需要 **JDK 8**（两个版本通用）。仓库采用**多版本单选构建**：根目录不含 mod 代码，直接 `gradlew build` 会失败，必须用 `-PmcVersion` 指定要构建的版本：

```bash
set JAVA_HOME=C:\Program Files\Java\jdk-1.8

gradlew build -PmcVersion=1.7.10    # 构建 1.7.10 版本
gradlew build -PmcVersion=1.12.2    # 构建 1.12.2 版本
```

同一套 `build` / `clean` / `jar` 任务都会转发给选中的版本，未选中的版本完全不参与构建。

两个项目都使用 anatawa12 维护的 ForgeGradle 分支（1.7.10 用 FG 1.2 分支，1.12.2 用 FG 2.3 分支），原版插件已因旧仓库下线无法使用。首次构建会下载依赖（几百 MB），需要联网。

## 使用场景

- **挖矿比赛**：`/mset sidebar` + `/mname` 定制标题，实时看排名
- **生存服务器**：belowName 常驻显示，`/mreset all` 每周清零开新赛季
- **单机存档**：单人世界自带集成服务器，功能完全相同；开 LAN 后统计所有加入的玩家

## 工作原理

- `BlockBreakHandler`：监听服务端 `BlockEvent.BreakEvent`（仅玩家亲手破坏触发），写入计数
- `MiningData`：继承 `WorldSavedData`，随世界自动保存/加载，按 UUID 记录
- `ScoreboardSync`：把计数镜像到原版计分板目标 `mined`，并同步到所有已加载世界的计分板
- 数据按方块 id（如 `minecraft:stone`）分类，原版与 mod 方块通用

## 已知限制

- 昵称（`/mnick`）只在 sidebar 生效：belowName 和 Tab 列表是客户端按玩家真实 ID 匹配的，无法替换（客户端限制）
- 只有玩家亲手挖的方块计数；TNT 炸掉的、活塞推掉的、烧毁的不计（因此也无法用 TNT 刷榜）
- 1.7.10 中每个维度有独立的计分板实例，mod 已做全维度同步；1.12.2 共享一个服务端计分板，天然无此问题

## 目录结构

```
miningtop/
├── build.gradle / settings.gradle / gradle.properties   # 根项目：版本选择与任务转发
├── 1.7.10/
│   ├── build.gradle / gradle.properties                  # 1.7.10 构建
│   └── src/main/
│       ├── java/com/miningtop/                           # 1.7.10 源码
│       └── resources/ (mcmod.info, assets/.../lang/)
└── 1.12.2/
    ├── build.gradle / gradle.properties                  # 1.12.2 构建
    └── src/main/
        ├── java/com/miningtop/                           # 1.12.2 源码
        └── resources/ (mcmod.info, assets/.../lang/)
```

两个版本源码结构完全相同：`MiningTop`（主类）、`BlockBreakHandler`（事件监听）、`MiningData`（数据持久化）、`PlayerMiningStats`（单玩家统计）、`ScoreboardSync`（计分板同步）、`BlockUtil`（方块工具）、`Command*.java`（六个命令）。
