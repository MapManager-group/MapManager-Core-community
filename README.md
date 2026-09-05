# MapManager-Core

[中文文档]( README_zh-CN.md)

MapManager-Core is a Minecraft multi-world management plugin for creative and building servers. It uses Multiverse-Core
to manage worlds and LuckPerms to manage map owners, builders, and visitors.

## Installation

Before installing MapManager-Core, install compatible versions of:

- LuckPerms
- Multiverse-Core
- VoidGen when using void worlds

Then place `MapManager-Core-{version}.jar` in the server's `plugins` directory and restart the server. On first start,
MapManager-Core creates `config.yml`, `worlds.json`, and `groups.json` in `plugins/MapManager-Core/`, plus the
`worldbase`, `apply`, and `public` LuckPerms groups if they do not already exist.

The Core module targets Java 21 and declares Minecraft API version 1.20.

## Map Roles

Each map has three roles:

| Role    | What it can do                                                                   |
|---------|----------------------------------------------------------------------------------|
| Admin   | Manage members and settings for maps in its map group. Admins are also builders. |
| Builder | Enter the map and receive permissions configured in `worldbase` for that world.  |
| Visitor | Enter one specific map, without receiving builder permissions.                   |

Map groups are set with `g:` when creating or importing a map. Maps that use the same group share admins and builders;
visitors remain per-map.

Normal players need `mapmanager.world` to use `/world` and `/worldtp`.

## Configure LuckPerms Groups

### `worldbase`

Every map group inherits `worldbase` with a world context. Add permissions that builders should have only while they are
inside a map, such as WorldEdit or permissions from your protection/build plugin.

For example, this gives builders WorldEdit selection permission:

```text
/lp group worldbase permission set worldedit.selection.pos true
```

Use the permission nodes required by your own protection plugin. MapManager-Core manages membership and world access; it
does not itself implement block-place or block-break protection.

### `apply`

Every newly created map group inherits `apply` without a world context. Put shared permissions here when all map
builders should receive them anywhere on the server, for example access to a shared materials world.

```text
/lp group apply permission set multiverse.access.materials true
```

This group is optional. Do not put map-only build permissions in `apply`, because they will apply outside the map as
well.

## Configuration

`plugins/MapManager-Core/config.yml` stores global physics and explosion settings:

```yaml
!MapManagerConfig
global:
  exploded: null
  physical: null
```

Use `true` to enable, `false` to disable, or `null` to leave the global value unset. Keep the `!MapManagerConfig`
header. Per-map settings are controlled with `/world physics` and `/world explosion`; save changes with
`/mapadmin save`.

## Commands

`<...>` is required and `[...]` is optional.

### Create and import maps

```text
/create n:<world> [e:<type>] [a:<alias>] [g:<group>] [o:<owner>]
/import n:<world> [e:<type>] [a:<alias>] [g:<group>] [o:<owner>]
```

Both commands create the map's LuckPerms group and make `o:` its first admin. Required permissions are
`mapmanager.command.create` and `mapmanager.command.import`.

| Parameter | Meaning                                                                                      |
|-----------|----------------------------------------------------------------------------------------------|
| `n:`      | World name. Required. Use lower-case names. For import, the world folder must already exist. |
| `e:`      | `flat`, `normal`, `void_gen`, `nether`, or `the_end`. Defaults to `flat`.                    |
| `a:`      | Display name. Defaults to the world name.                                                    |
| `g:`      | Shared map group. Defaults to the world name.                                                |
| `o:`      | First map admin. Defaults to the command sender; provide it explicitly from console.         |

Examples:

```text
/create n:studio e:flat a:Studio g:builders o:Alice
/create n:sky_gallery e:void_gen a:SkyGallery g:builders o:Alice
/import n:old_city e:normal a:OldCity g:city_team o:Alice
```

Imports require a valid world folder with `level.dat` in the server world container. Back up an imported world before
managing it.

### Manage the current map

All `/world` commands are player-only and require `mapmanager.world`. Commands that change membership or settings also
require `mapmanager.admin.<group>` for the current map.

| Command                                               | Description                                                          |
|-------------------------------------------------------|----------------------------------------------------------------------|
| `/world admins`, `/world builders`, `/world visitors` | List the current map's members.                                      |
| `/world admin <add \| remove> <player>`               | Add or remove an admin.                                              |
| `/world builder <add \| remove> <player>`             | Add or remove a builder.                                             |
| `/world visitor <add \| remove> <player>`             | Add or remove a visitor for the current map.                         |
| `/world public [true \| false \|info]`                | Make the current map public, private, or show its status.            |
| `/world physics [true \| false \|info]`               | Control block physics for the current map.                           |
| `/world explosion [true \| false \|info]`             | Control explosion block damage for the current map.                  |
| `/world pvp [true \| false \|info]`                   | Control PVP for the current map.                                     |
| `/world setname <name>`                               | Set the map display name.                                            |
| `/world setspawn`                                     | Set the current location as the map spawn.                           |
| `/world kick <player>`                                | Send an online player in the current map to the default-world spawn. |
| `/world tp <world>`                                   | Load the target map if needed and teleport to its spawn.             |

`on`, `enable`, and `yes` also mean `true`; `off`, `disable`, and `no` mean `false`. Use `/world public true` to publish
a map. Adding `*` as a visitor does not publish it.

`/worldtp <world>` and `/wtp <world>` are shortcuts for `/world tp <world>`.

### Administration

| Command                                        | Permission                       | Description                                                                      |
|------------------------------------------------|----------------------------------|----------------------------------------------------------------------------------|
| `/init <world>`                                | `mapmanager.command.init`        | Apply MapManager's default world settings to a loaded Multiverse world.          |
| `/delete [world]`                              | `mapmanager.command.delete`      | Start deletion; confirm within 10 seconds with `/delete confirm`. Back up first. |
| `/write`                                       | `mapmanager.command.write`       | Print the currently loaded map and group records.                                |
| `/mapadmin save`                               | `mapmanager.command.mapadmin.md` | Save MapManager data and Multiverse world settings.                              |
| `/mapadmin reload`                             | `mapmanager.command.mapadmin.md` | Reload `worlds.json` and `groups.json`.                                          |
| `/mapadmin sync`                               | `mapmanager.command.mapadmin.md` | Rebuild cached member lists from LuckPerms.                                      |
| `/mapadmin physics <true \| false \| clear>`   | `mapmanager.command.mapadmin.md` | Set or clear the global physics value.                                           |
| `/mapadmin explosion <true \| false \| clear>` | `mapmanager.command.mapadmin.md` | Set or clear the global explosion value.                                         |

`/delete` can remove world data through Multiverse-Core. Always make a backup before confirming.

## Development

Use the published API as a compile-only dependency. Do not shade MapManager-Core into your own plugin.

Maven:

```xml

<dependency>
    <groupId>work.alsace.mapmanager</groupId>
    <artifactId>MapManager-Core</artifactId>
    <version>${version}</version>
    <scope>provided</scope>
</dependency>
```

Gradle Kotlin DSL:

```kotlin
dependencies {
    compileOnly("work.alsace.mapmanager:MapManager-Core:${version}")
}
```

Add MapManager-Core as a dependency in your plugin descriptor:

```yaml
depend:
  - MapManager-Core
```

Retrieve the service after your plugin is enabled:

```java
MapManager mapManagerCore = ((MapManager) Objects
        .requireNonNull(Bukkit.getServer()
                .getPluginManager()
                .getPlugin("MapManager-Core"));
```

`MapAgent` manages map membership, public status, aliases, and stored data. `DynamicWorld` manages Multiverse worlds,
including creation, import, loading, initialization, and deletion. Calls that change Bukkit or world state must run on
the server main thread.
