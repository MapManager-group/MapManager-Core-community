# MapManager-Core

[English Documentation](README.md)

MapManager-Core 是面向 Minecraft 创造和建筑服务器的多世界管理插件。它使用 Multiverse-Core 管理世界，使用 LuckPerms
管理地图管理员、建筑人员和访客。

## 安装

安装 MapManager-Core 前，请先安装兼容版本的：

- LuckPerms
- Multiverse-Core
- 使用虚空世界时需要 VoidGen

将 `MapManager-Core-{version}.jar` 放入服务端 `plugins` 目录后重启服务端。首次启动会在 `plugins/MapManager-Core/` 生成
`config.yml`、`worlds.json` 和 `groups.json`；缺少时还会创建 LuckPerms 的 `worldbase`、`apply` 与 `public` 权限组。

Core 模块使用 Java 21 构建，并声明 Minecraft API 版本为 1.20，推荐使用 Java 25运行。

## 三种地图权限

每张地图有三种身份：

| 身份               | 权限与作用                                               |
|--------------------|----------------------------------------------------------|
| 管理员 `admin`     | 管理其地图组内的成员和地图设置；管理员同时也是建筑人员。 |
| 建筑师 `builder`   | 可进入地图，并获得该世界内 `worldbase` 中配置的权限。    |
| 参观人员 `visitor` | 只能进入指定地图，不会自动获得建筑权限。                 |

创建或导入地图时，通过 `g:` 设置地图组。使用同一地图组的地图共享管理员和建筑人员；访客仍按单张地图管理。

普通玩家使用 `/world`、`/worldtp` 需要 `mapmanager.world`。

## 配置 LuckPerms 权限组

### `worldbase`

每个地图组都会以世界上下文继承 `worldbase`。在此处添加建筑人员仅应在某张地图内拥有的权限，例如 WorldEdit 或保护插件的建造权限。

例如，为世界内的建筑人员授予 WorldEdit 选点权限：

```text
/lp group worldbase permission set worldedit.selection.pos true
```

具体权限节点由服务器使用的保护插件决定。MapManager-Core 负责成员与世界访问权限，本身不提供完整的放置和破坏方块保护。

### `apply`

每个新建地图组都会无上下文继承 `apply`。将所有地图建筑人员在服务器任意位置都需要的共享权限放在这里，例如共享材料世界的访问权限：

```text
/lp group apply permission set multiverse.access.materials true
```

`apply` 为可选配置。不要把仅应在地图内生效的建筑权限放入其中，否则权限会在地图外也生效。

## 配置文件

`plugins/MapManager-Core/config.yml` 保存全局物理和爆炸设置：

```yaml
!MapManagerConfig
global:
  exploded: null
  physical: null
```

`true` 表示开启，`false` 表示关闭，`null` 表示不设置全局值。请保留 `!MapManagerConfig` 标记。单张地图使用 `/world physics` 和
`/world explosion` 设置，修改后使用 `/mapadmin save` 保存。

## 命令用法

`<...>` 为必填参数，`[...]` 为可选参数。

### 创建和导入地图

```text
/create n:<世界名> [e:<类型>] [a:<显示名>] [g:<地图组>] [o:<管理员>]
/import n:<世界名> [e:<类型>] [a:<显示名>] [g:<地图组>] [o:<管理员>]
```

两条命令都会创建地图对应的 LuckPerms 地图组，并将 `o:` 指定的玩家设为首位管理员。权限分别为 `mapmanager.command.create` 和
`mapmanager.command.import`。

| 参数 | 说明                                                             |
|------|------------------------------------------------------------------|
| `n:` | 世界名称，必填。建议统一使用小写；导入时该世界目录必须已存在。   |
| `e:` | `flat`、`normal`、`void_gen`、`nether`、`the_end`，默认 `flat`。 |
| `a:` | 世界显示名称，默认世界名。                                       |
| `g:` | 共用的地图组，默认世界名。                                       |
| `o:` | 首位地图管理员，默认命令发送者；控制台执行时请明确填写。         |

示例：

```text
/create n:studio e:flat a:建筑工作室 g:builders o:Alice
/create n:sky_gallery e:void_gen a:天空展馆 g:builders o:Alice
/import n:old_city e:normal a:旧城区 g:city_team o:Alice
```

导入前，需将包含 `level.dat` 的有效地图目录放入服务端世界容器目录。管理导入地图前请先备份。

### 管理当前地图

所有 `/world` 命令只能由玩家使用，并需要 `mapmanager.world`。修改成员或地图设置还需要当前地图的
`mapmanager.admin.<地图组>`。

| 命令                                                  | 说明                                       |
|-------------------------------------------------------|--------------------------------------------|
| `/world admins`、`/world builders`、`/world visitors` | 查看当前地图成员。                         |
| `/world admin <add \| remove> <玩家>`                 | 添加或移除管理员。                         |
| `/world builder <add \| remove> <玩家>`               | 添加或移除建筑人员。                       |
| `/world visitor <add \| remove> <玩家>`               | 添加或移除当前地图的访客。                 |
| `/world public [true \| false \|info]`                | 公开、私有或查看当前地图的公开状态。       |
| `/world physics [true \| false \|info]`               | 设置或查看当前地图的方块物理。             |
| `/world explosion [true \| false \|info]`             | 设置或查看当前地图的爆炸破坏。             |
| `/world pvp [true \| false \|info]`                   | 设置或查看当前地图 PVP。                   |
| `/world setname <名称>`                               | 设置地图显示名称。                         |
| `/world setspawn`                                     | 将当前位置设为地图出生点。                 |
| `/world kick <玩家>`                                  | 将当前地图内的在线玩家送回默认世界出生点。 |
| `/world tp <世界名>`                                  | 按需加载目标地图并传送到其出生点。         |

`on`、`enable`、`yes` 与 `true` 等效；`off`、`disable`、`no` 与 `false` 等效。公开地图请使用 `/world public true`，为访客添加
`*` 不会公开地图。

`/worldtp <世界名>` 和 `/wtp <世界名>` 是 `/world tp <世界名>` 的快捷命令。

### 管理员命令

| 命令                                            | 所需权限                         | 说明                                                           |
|-------------------------------------------------|----------------------------------|----------------------------------------------------------------|
| `/init <世界名>`                                | `mapmanager.command.init`        | 对已加载的 Multiverse 世界应用 MapManager 默认世界设置。       |
| `/delete [世界名]`                              | `mapmanager.command.delete`      | 发起删除；10 秒内使用 `/delete confirm` 确认。删除前务必备份。 |
| `/write`                                        | `mapmanager.command.write`       | 输出当前内存中的地图和地图组记录。                             |
| `/mapadmin save`                                | `mapmanager.command.mapadmin.md` | 保存 MapManager 数据和 Multiverse 世界设置。                   |
| `/mapadmin reload`                              | `mapmanager.command.mapadmin.md` | 重载 `worlds.json` 和 `groups.json`。                          |
| `/mapadmin sync`                                | `mapmanager.command.mapadmin.md` | 从 LuckPerms 同步成员缓存。                                    |
| `/mapadmin physics <true \| false \| clear>`    | `mapmanager.command.mapadmin.md` | 设置或清除全局物理值。                                         |
| `/mapadmin explosion <true \| false  \| clear>` | `mapmanager.command.mapadmin.md` | 设置或清除全局爆炸值。                                         |

`/delete` 会通过 Multiverse-Core 删除世界数据，不只是删除插件记录；确认前请完成备份。

## 开发

将 MapManager-Core 作为仅编译依赖使用，不要将它打包进自己的插件。

Maven：

```xml

<dependency>
    <groupId>work.alsace.mapmanager</groupId>
    <artifactId>MapManager-Core</artifactId>
    <version>${version}</version>
    <scope>provided</scope>
</dependency>
```

Gradle Kotlin DSL：

```kotlin
dependencies {
    compileOnly("work.alsace.mapmanager:MapManager-Core:${version}")
}
```

在自身插件的 `plugin.yml` 中声明依赖：

```yaml
depend:
  - MapManager-Core
```

在自己的插件启用后获取服务：

```java
MapManager mapManagerCore = ((MapManager) Objects
        .requireNonNull(Bukkit.getServer()
                .getPluginManager()
                .getPlugin("MapManager-Core"));
}
```

`MapAgent` 用于管理地图成员、公开状态、别名和存储数据；`DynamicWorld` 用于创建、导入、加载、初始化和删除 Multiverse 世界。修改
Bukkit 或世界状态的调用应在服务端主线程执行。
