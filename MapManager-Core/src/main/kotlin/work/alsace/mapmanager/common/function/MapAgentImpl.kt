package work.alsace.mapmanager.common.function

import com.google.gson.reflect.TypeToken
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.luckperms.api.LuckPerms
import net.luckperms.api.context.DefaultContextKeys
import net.luckperms.api.model.group.Group
import net.luckperms.api.model.user.User
import net.luckperms.api.node.Node
import net.luckperms.api.node.matcher.NodeMatcher
import net.luckperms.api.node.types.InheritanceNode
import net.luckperms.api.node.types.PermissionNode
import net.luckperms.api.node.types.WeightNode
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.World
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.mvplugins.multiverse.core.world.options.UnloadWorldOptions
import work.alsace.mapmanager.MapManagerImpl
import work.alsace.mapmanager.enums.MapGroup
import work.alsace.mapmanager.pojo.MainConfig
import work.alsace.mapmanager.pojo.WorldGroup
import work.alsace.mapmanager.pojo.WorldNode
import work.alsace.mapmanager.service.DynamicWorld
import work.alsace.mapmanager.service.MainYaml
import work.alsace.mapmanager.service.MapAgent
import java.io.File
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.ExecutionException
import java.util.stream.Collectors

/**
 * 地图管理代理，负责处理与LuckPerms权限插件的交互、管理世界及其权限组等功能。
 */
class MapAgentImpl(private val plugin: MapManagerImpl) : MapAgent {
    private val nodeIO: FileIO<WorldNode?>?
    private val groupIO: FileIO<WorldGroup?>?
    private val yaml: MainYaml
    private var luckPerms: LuckPerms = plugin.getLuckPerms()
    private val dynamicWorld: DynamicWorld = plugin.getDynamicWorld()
    private var nodeMap //world name -> world node
            : ConcurrentMap<String, WorldNode?>
    private var groupMap //group name -> group node
            : ConcurrentMap<String, WorldGroup?>
    private val config: MainConfig

    init {
        nodeIO = FileIO(plugin, "worlds", object : TypeToken<ConcurrentMap<String?, WorldNode?>?>() {})
        nodeMap = nodeIO.load()
        groupIO = FileIO(plugin, "groups", object : TypeToken<ConcurrentMap<String?, WorldGroup?>?>() {})
        groupMap = groupIO.load()
        yaml = plugin.getMainYaml()
        config = yaml.load()
        val global = config.global
        if (global != null) {
            physical = global.physical
        }
        if (global != null) {
            exploded = global.exploded
        }
    }

    override fun reload() {
        nodeMap.clear()
        nodeMap = nodeIO!!.load()
        groupMap.clear()
        groupMap = groupIO!!.load()
    }


    override fun save(): Boolean {
        dynamicWorld.getMVWorldManager()?.saveWorldsConfig()
        return nodeIO!!.save(nodeMap) && groupIO!!.save(groupMap)
    }

    override fun setLuckPerms(luckPerms: LuckPerms) {
        this.luckPerms = luckPerms
    }

    private fun getWorldNode(world: String): WorldNode? {
        return nodeMap[world]
    }

    private fun getWorldGroup(world: String): WorldGroup? {
        return getWorldGroupName(world)?.let(groupMap::get)
    }

    private fun getWorldGroupByName(group: String): WorldGroup? {
        return groupMap[group]
    }

    /**
     * 获取玩家的 UUID
     * @param player 玩家名称
     * @return 返回玩家的 UUID（正版模式下若玩家不存在或网络请求失败则返回 null）
     */
    override fun getUniqueID(player: String): UUID? {
        // 获取在线玩家uuid
        val onlinePlayer = plugin.server.getPlayerExact(player)
        if (onlinePlayer != null) {
            return onlinePlayer.uniqueId
        }
        // 若玩家不在线，从paper获取缓存
        val profile = plugin.server.createProfile(player)
        if (profile.completeFromCache() && profile.id != null) {
            return profile.id
        }
        return if (isEffectiveOnlineMode()) {
            try {
                // complete(false) 会向 Mojang API 发起 HTTP 请求补全 UUID
                // 参数 false 表示不请求皮肤材质数据 (Textures)，以提高响应速度
                if (profile.complete(false) && profile.id != null) {
                    profile.id
                } else {
                    // Mojang 数据库中不存在该正版玩家
                    null
                }
            } catch (e: Exception) {
                plugin.logger.warning(
                    "[MapManager] 向 Mojang 查询玩家 '$player' 的正版 UUID 时发生网络异常: ${e.message}"
                )
                null
            }
        } else {
            generateOfflineUUID(player)
        }
    }

    override fun getWorldParentFolder(): File {
        val level = plugin.server.worlds[0].name
        val rootDir = plugin.server.worldContainer
        val dimensionsDir = File(rootDir, "$level/dimensions/minecraft")
        if (dimensionsDir.exists() && dimensionsDir.isDirectory) {
            return dimensionsDir
        }
        return rootDir
    }

    override fun getWorldFolder(world: String): File {
        return File(getWorldParentFolder(), world)
    }


    private fun generateOfflineUUID(playerName: String): UUID {
        return UUID.nameUUIDFromBytes("OfflinePlayer:$playerName".toByteArray(Charsets.UTF_8))
    }

    /**
     * 判断当前服务器是否处于online模式
     */
    fun isEffectiveOnlineMode(): Boolean {
        // 1. 直连模式下开启了正版验证
        if (plugin.server.onlineMode) {
            return true
        }

        // 2. 检查 Paper 全局配置文件
        val paperGlobalFile = File(plugin.server.worldContainer, "config/paper-global.yml")
        if (paperGlobalFile.exists()) {
            try {
                val config = YamlConfiguration.loadConfiguration(paperGlobalFile)

                // 检查 Velocity 代理转发 + 正版验证
                val velocityEnabled = config.getBoolean("proxies.velocity.enabled", false)
                val velocityOnline = config.getBoolean("proxies.velocity.online-mode", false)
                if (velocityEnabled && velocityOnline) {
                    return true
                }

                // 检查 BungeeCord 代理转发 + 正版验证
                val bungeeSpigotEnabled = plugin.server.spigot().config.getBoolean("settings.bungeecord", false)
                val bungeePaperOnline = config.getBoolean("proxies.bungee-cord.online-mode", false)
                if (bungeeSpigotEnabled && bungeePaperOnline) {
                    return true
                }
            } catch (e: Exception) {
                plugin.logger.warning(
                    "读取 config/paper-global.yml 配置文件时出错: ${e.message}"
                )
            }
        }

        return false
    }

    /**
     * 通过玩家名获取服务器在线玩家实体
     * @param player 玩家名称
     * @return Player? 玩家实体（若玩家不在线或不存在则返回 null）
     */
    override fun getPlayer(player: String): Player? {
        val onlinePlayer = plugin.server.getPlayerExact(player)
        if (onlinePlayer != null) {
            return onlinePlayer
        }
        val uuid = getUniqueID(player) ?: return null
        return plugin.server.getPlayer(uuid)
    }

    override fun isPlayerRegister(player: String): Boolean {
        val online = plugin.server.getPlayer(player)
        if (online != null) return true // 玩家在线
        for (off in plugin.server.offlinePlayers) {
            val uuid = off.uniqueId
            if (uuid == getUniqueID(player)) return true
        }
        // 没有找到玩家
        return false
    }

    /**
     * 异步获取 LuckPerms 的 User 对象（彻底防止阻塞主线程）
     * @param owner 玩家名称
     * @return CompletableFuture<User?>
     */
    private fun getProcess(owner: String?): CompletableFuture<User?> {
        if (owner == null) {
            return CompletableFuture.completedFuture(null)
        }

        return CompletableFuture.supplyAsync {
            getUniqueID(owner)
        }.thenCompose { uuid ->
            if (uuid == null) {
                CompletableFuture.completedFuture(null)
            } else {
                luckPerms.userManager.loadUser(uuid)
            }
        }
    }

    /**
     * 创建一个新的世界，并将其注册到权限管理中。
     *
     * @param world 世界名称。
     * @param owner 世界拥有者的玩家名。
     * @param group 权限组名称。
     */
    override fun newWorld(world: String, owner: String, group: String): CompletableFuture<Void> {
        val worldLowerCase = world.lowercase(Locale.ROOT)
        val groupLowerCase = group.lowercase(Locale.ROOT)
        val gm = luckPerms.groupManager
        val um = luckPerms.userManager

        // 创建/加载 Group 和 获取/加载 User
        val groupFuture: CompletableFuture<Group> = gm.createAndLoadGroup(groupLowerCase).thenApply { lp ->
            lp.data().apply {
                add(PermissionNode.builder("multiverse.access.$worldLowerCase").build())
                add(InheritanceNode.builder("default").build())
                add(
                    InheritanceNode.builder("worldbase").withContext(DefaultContextKeys.WORLD_KEY, worldLowerCase)
                        .build()
                )
                add(InheritanceNode.builder("apply").build())
                add(WeightNode.builder(1).build())
            }
            plugin.logger.info("permission.group initialized: group=${lp.name}")
            lp
        }

        val userFuture: CompletableFuture<User?> = getProcess(owner)

        return groupFuture.thenCombine(userFuture) { lp, user ->
            user?.data()?.add(PermissionNode.builder("mapmanager.admin.$groupLowerCase").build())
            user?.data()?.add(InheritanceNode.builder(lp).build())
            plugin.logger.info("permission.group member-added: group=$groupLowerCase, player=${user?.username ?: owner}")

            Pair(lp, user)
        }.thenCompose { (lp, user) ->
            // 异步平滑组合：等待 LuckPerms 的保存真正落盘完成
            CompletableFuture.allOf(
                gm.saveGroup(lp),
                user?.let { um.saveUser(it) }
            )
        }.thenRun {
            // 落盘完成后更新本地缓存与保存配置
            nodeMap[world] = WorldNode(groupLowerCase)

            val worldGroup = groupMap[groupLowerCase]
            if (worldGroup != null) {
                worldGroup.addWorld(world)
            } else {
                groupMap[groupLowerCase] = WorldGroup(world, owner)
            }

            save()
        }
    }

    private fun checkGroup(group: Group): Boolean {
        val nodes = group.distinctNodes
        if (nodes.size > 3) {
            return false
        }
        for (node in nodes) {
            val key = node.key
            if (!key.startsWith("weight.") && key != "group.apply" && key != "group.default") return false
        }
        return true
    }

    /**
     * 删除一个世界及其相关的权限信息。
     *
     * @param world 要删除的世界名称。
     * @return 如果成功删除，返回true；否则返回false。
     */
    override fun deleteWorld(world: String): Boolean {
        val lowerWorld = world.lowercase(Locale.ROOT)
        val worldNode = getWorldNode(world)
        val groupNode = getWorldGroup(world)
        val gm = luckPerms.groupManager
        val um = luckPerms.userManager

        // 踢出玩家
        Bukkit.getWorld(world)?.let { bukkitWorld ->
            val spawnLoc = dynamicWorld.getDefaultSpawnLocation()
            val notice = Component.text("世界 $world 正在被删除，您已被传送至出生点", NamedTextColor.GRAY)

            for (player in bukkitWorld.players) {
                spawnLoc?.let { player.teleport(it) }
                player.sendMessage(notice)
            }
        }

        // 地图卸载逻辑
        dynamicWorld.getMVWorldManager()?.getLoadedWorld(world)?.getOrNull()?.let {
            dynamicWorld.getMVWorldManager()?.unloadWorld(UnloadWorldOptions.world(it))
                ?.onFailure { failure -> plugin.logger.warning("world.unload failed: world=$world, reason=${failure.failureMessage}") }
        }

        // 构造 access 权限节点
        val enterNode = PermissionNode.builder("multiverse.access.$lowerWorld").build()
        // 构造 admin 权限节点
        val adminNode = PermissionNode.builder("mapmanager.admin.${worldNode?.group ?: lowerWorld}").build()

        // 清理 default 权限组中的 multiverse.access.$lowerWorld 权限
        val defaultGroupSaveFuture = gm.getGroup("default")?.let { defaultGroup ->
            defaultGroup.data().remove(enterNode)
            gm.saveGroup(defaultGroup)
        } ?: CompletableFuture.completedFuture(null)

        // 异步清除散落在用户身上的 access 和 admin 节点
        val userAccessCleanup = um.searchAll(NodeMatcher.key(enterNode)).thenCompose { result ->
            val futures = result.keys.filterNotNull().map { uuid ->
                um.modifyUser(uuid) { user ->
//                    user.data().remove(enterNode)
                    user.data().remove(adminNode)
                }
            }
            CompletableFuture.allOf(*futures.toTypedArray())
        }

        // 清理权限
        val combinedCleanup = CompletableFuture.allOf(defaultGroupSaveFuture, userAccessCleanup)

        // 本地缓存同步清除
        nodeMap.remove(world)
        groupNode?.removeWorld(world)

        val groupName = worldNode?.group
        val group = groupName?.let { gm.getGroup(it) }

        if (group == null || group.name == "__nil") {
            if (worldNode != null) {
                plugin.logger.warning("permission.group missing: group=$groupName, world=$world")
            }
            saveConfigAndCancelTask(world)
            return dynamicWorld.removeWorld(world)
        }

        // 清理地图group中的节点
        group.data().remove(enterNode)
        val worldBaseNode = InheritanceNode.builder("worldbase")
            .withContext(DefaultContextKeys.WORLD_KEY, lowerWorld)
            .build()
        group.data().remove(worldBaseNode)
        gm.saveGroup(group)

        // 判断并清理继承该group的所有用户节点，随后完全销毁
        if (checkGroup(group)) {
            val groupInheritanceNode = InheritanceNode.builder(group).build()

            combinedCleanup.thenCompose {
                // 搜索所有继承了此 Group 的玩家
                um.searchAll(NodeMatcher.key(groupInheritanceNode))
            }.thenCompose { result ->
                // 批量移除继承节点并保存
                val futures = result.keys.filterNotNull().map { uuid ->
                    um.modifyUser(uuid) { user ->
                        user.data().remove(groupInheritanceNode)
                    }
                }
                CompletableFuture.allOf(*futures.toTypedArray())
            }.thenAccept {
                // 确保所有 User 及 Default 组修改落盘完成后，再删除 Group
                gm.deleteGroup(group)
                groupMap.remove(groupName)
                saveConfigAndCancelTask(world)
            }
        } else {
            saveConfigAndCancelTask(world)
        }

        return dynamicWorld.removeWorld(world)
    }

    private fun saveConfigAndCancelTask(world: String) {
        nodeIO?.save(nodeMap)
        groupIO?.save(groupMap)
        dynamicWorld.cancelUnloadTask(world)
    }

    /**
     * 为指定世界的指定权限组添加一个玩家。
     *
     * @param world 世界名称。
     * @param group 权限组（管理员、建筑师、访客之一）。
     * @param player 玩家名称。
     * @return 操作成功返回true，否则返回false。
     */
    override fun addPlayer(world: String, group: MapGroup, player: String): Boolean {
        val worldGroup = getWorldGroupName(world)
        if (worldGroup == null) {
            plugin.logger.warning("permission.group missing: world=$world")
            return false
        }
        val uuid = getUniqueID(player) ?: return false

        val user: User? = try {
            luckPerms.userManager.loadUser(uuid).get()
        } catch (e: ExecutionException) {
            plugin.logger.warning("permission.user load-failed: player=$player, reason=${e.cause?.message ?: e.message}")
            return false
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            plugin.logger.warning("permission.user load-interrupted: player=$player")
            return false
        }
        when (group) {
            MapGroup.ADMIN -> {
                run {
                    user?.data()?.add(PermissionNode.builder("mapmanager.admin.$worldGroup").build())
                    addAdmin(world, player)
                }
                run {
                    user?.data()?.add(InheritanceNode.builder(worldGroup).build())
                    addBuilder(world, player)
                }
            }

            MapGroup.BUILDER -> {
                user?.data()?.add(InheritanceNode.builder(worldGroup).build())
                addBuilder(world, player)
            }

            MapGroup.VISITOR -> {
                user?.data()?.add(
                    PermissionNode.builder("multiverse.access." + world.lowercase(Locale.ROOT)).build()
                )
                addVisitor(world, player)
            }
        }
        user?.let { luckPerms.userManager.saveUser(it) }
        groupIO?.save(groupMap)
        return true
    }

    /**
     * 从指定世界的指定权限组中移除一个玩家。
     *
     * @param world 世界名称。
     * @param group 权限组索引（0为管理员，1为建筑师，2为访客）。
     * @param player 玩家名称。
     * @return 操作成功返回true，否则返回false。
     */
    override fun removePlayer(world: String, group: MapGroup, player: String): Boolean {
        val worldGroup = getWorldGroupName(world)
        if (worldGroup == null) {
            plugin.logger.warning("permission.group missing: world=$world")
            return false
        }
        val uuid = getUniqueID(player) ?: return false
        val user = luckPerms.userManager.loadUser(uuid).join()
        when (group) {
            MapGroup.ADMIN -> {
                run {
                    user?.data()?.remove(PermissionNode.builder("mapmanager.admin.$worldGroup").build())
                    removeAdmin(world, player)
                }
                run {
                    user?.data()?.remove(InheritanceNode.builder(worldGroup).build())
                    removeBuilder(world, player)
                    groupIO?.save(groupMap)
                }
            }

            MapGroup.BUILDER -> {
                user?.data()?.remove(InheritanceNode.builder(worldGroup).build())
                removeBuilder(world, player)
                groupIO?.save(groupMap)
            }

            MapGroup.VISITOR -> {
                user?.data()
                    ?.remove(
                        PermissionNode.builder("multiverse.access." + world.lowercase(Locale.ROOT)).build()
                    )
                removeVisitor(world, player)
                nodeIO?.save(nodeMap)
            }
        }
        user?.let { luckPerms.userManager.saveUser(it) }
        return true
    }

    /**
     * 将指定世界设置为公开状态。
     *
     * @param world 世界名称。
     * @return 操作成功返回true，否则返回false。
     */
    override fun publicizeWorld(world: String): Boolean {
        val lp = luckPerms.groupManager.getGroup("default")
        if (lp == null) {
            plugin.logger.warning("permission.group missing: group=default")
            return false
        }
        lp.data().add(PermissionNode.builder("multiverse.access." + world.lowercase(Locale.ROOT)).build())
        luckPerms.groupManager.saveGroup(lp)
        var alias = dynamicWorld.getMVWorld(world).alias
        if (alias == null) {
            alias = world
        }
        val str = ignoreColor(alias, "")
        dynamicWorld.getMVWorld(world).alias = "&2${str}"
        dynamicWorld.getMVWorldManager()?.saveWorldsConfig()
        return true
    }

    /**
     * 将指定世界设置为私有状态。
     *
     * @param world 世界名称。
     * @return 操作成功返回true，否则返回false。
     */
    override fun privatizeWorld(world: String): Boolean {
        val lp = luckPerms.groupManager.getGroup("default")
        if (lp == null) {
            plugin.logger.warning("permission.group missing: group=default")
            return false
        }
        lp.data()
            .remove(PermissionNode.builder("multiverse.access." + world.lowercase(Locale.ROOT)).build())
        luckPerms.groupManager.saveGroup(lp)
        var alias = dynamicWorld.getMVWorld(world).alias
        if (alias == null) {
            alias = world
        }
        val str = ignoreColor(alias, "")
        dynamicWorld.getMVWorld(world).alias = "&3${str}"
        dynamicWorld.getMVWorldManager()?.saveWorldsConfig()
        return true
    }

    /**
     * 判断指定的世界是否为公共世界。
     *
     * @param world 要检查的世界名称。
     * @return 如果世界为公共世界，返回true，否则返回false。
     */
    override fun isPublic(world: String): Boolean {
        val lp = luckPerms.groupManager.getGroup("default")
        if (lp == null) {
            plugin.logger.warning("permission.group missing: group=default")
            return false
        }
        val group = lp.nodes
        return group.contains(
            PermissionNode.builder("multiverse.access." + world.lowercase(Locale.ROOT)).build()
        )
    }

    /**
     * 检测玩家是否为地图管理员
     * @param player 玩家名称
     * @param world 世界名称
     * @return 是否为管理员，如果是返回true，否则返回false
     */
    override fun isAdmin(player: String, world: String): Boolean {
        val luckPerms = plugin.getLuckPerms()
        val playerUuid = plugin.getMapAgent().getUniqueID(player) ?: return false
        val userFuture = luckPerms.userManager.loadUser(playerUuid)
        val user = userFuture.join() ?: return false
        return dynamicWorld.hasPermission(user, "mapmanager.admin." + getWorldGroupName(world))
    }

//name -> group name | world name
    /**
     * 获取指定名称的玩家集合，基于指定的权限组筛选。
     *
     * @param worldName 世界名称或权限组名称。
     * @param group 权限组枚举（管理员、建筑师、访客）。
     * @return 包含玩家名称的CompletableFuture实例。
     */
    override fun getPlayers(worldName: String, group: MapGroup): CompletableFuture<MutableSet<String>> {
        val matcher = when (group) {
            MapGroup.ADMIN -> NodeMatcher.key<Node>(
                PermissionNode.builder(
                    "mapmanager.admin." + worldName.lowercase(
                        Locale.getDefault()
                    )
                ).build()
            )

            MapGroup.BUILDER -> NodeMatcher.key<Node>(
                InheritanceNode.builder(worldName.lowercase(Locale.getDefault())).build()
            )

            MapGroup.VISITOR -> NodeMatcher.key<Node>(
                PermissionNode.builder(
                    "multiverse.access." + worldName.lowercase(
                        Locale.getDefault()
                    )
                ).build()
            )
        }
        return matcher.let {
            luckPerms.userManager.searchAll(it)
                .thenApplyAsync { results: MutableMap<UUID, MutableCollection<Node>> ->
                    results.keys.stream()
                        .map { id: UUID ->
                            Bukkit.getOfflinePlayer(
                                id
                            )
                        }
                        .map { obj: OfflinePlayer -> obj.name }
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet())
                }
        }


    }

    private fun putWorldGroup(group: String, world: String) {
        if (groupMap.containsKey(group)) getWorldGroupByName(group)?.addWorld(world) else groupMap[group] =
            WorldGroup(world)
    }

    /**
     * 与LuckPerms插件同步数据，更新本地存储的玩家权限信息。
     *
     * @param sender 命令发送者，用于回显操作结果。
     */
    override fun syncWithLuckPerms(sender: CommandSender) {
        val nodeMapBackup: ConcurrentMap<String, WorldNode?> = ConcurrentHashMap(nodeMap)
        val groupMapBackup: ConcurrentMap<String, WorldGroup?> = ConcurrentHashMap(groupMap)

        plugin.logger.info("正在与 LuckPerms 同步数据...")
        sender.sendMessage(Component.text("正在与 LuckPerms 同步数据...", NamedTextColor.YELLOW))

        // 重建groupMap的映射关系
        groupMap.clear()
        for ((key, value) in nodeMap) {
            value?.let { putWorldGroup(it.group, key) }
        }

        // 并行构建 Visitor 数据的查询 Futures
        plugin.logger.info("开始同步参观人员数据...")
        val visitorFutures = nodeMap.mapNotNull { (key, value) ->
            value?.let { node ->
                getPlayers(key, MapGroup.VISITOR).thenAccept { players ->
                    node.visitors = players
                }
            }
        }.toTypedArray()

        val visitorTask = CompletableFuture.allOf(*visitorFutures).thenRun {
            plugin.logger.info("参观人员数据同步完成")
        }

        // 并行构建 Admin 和 Builder 数据的查询 Futures
        plugin.logger.info("开始同步管理员与建筑人员数据...")
        val groupFutures = groupMap.flatMap { (key, value) ->
            val futures = mutableListOf<CompletableFuture<Void>>()
            value?.let { group ->
                futures.add(getPlayers(key, MapGroup.ADMIN).thenAccept { players ->
                    group.admins = players
                })
                futures.add(getPlayers(key, MapGroup.BUILDER).thenAccept { players ->
                    group.builders = players
                })
            }
            futures
        }.toTypedArray()

        val groupTask = CompletableFuture.allOf(*groupFutures).thenRun {
            plugin.logger.info("管理员与建筑人员数据同步完成")
        }

        // 等待所有任务并发完成后汇总落盘
        CompletableFuture.allOf(visitorTask, groupTask).whenComplete { _, throwable ->
            if (throwable != null) {
                // 出现异常进行回滚
                nodeMap = nodeMapBackup
                groupMap = groupMapBackup
                plugin.logger.severe("数据同步时出现错误，同步中止")
                sender.sendMessage(Component.text("数据同步时出现错误，同步中止", NamedTextColor.RED))
                throwable.printStackTrace()
                return@whenComplete
            }

            // 同步成功，执行保存
            plugin.logger.info("所有数据均已同步完成，数据保存中...")
            nodeMapBackup.clear()
            groupMapBackup.clear()
            save()
            plugin.logger.info("数据保存完成")
            sender.sendMessage(Component.text("数据同步完成！", NamedTextColor.GREEN))
        }
    }


//Getters and setters
    /**
     * 获取地图别名
     *
     * @param worldName 世界名
     * @return String 别名
     */
    override fun getWorldAlias(worldName: String): String {
        return dynamicWorld.getMVWorld(worldName).alias ?: worldName
    }

    /**
     * 设置地图别名
     * @param worldName 地图名
     * @param alias 别名
     */
    override fun setWorldAlias(worldName: String, alias: String) {
        val world = dynamicWorld.getMVWorld(worldName)
        val str = ignoreColor(alias, "")
        if (isPublic(worldName)) {
            world.alias = "&2${str}"
        } else {
            world.alias = "&3${str}"
        }
        dynamicWorld.getMVWorldManager()?.saveWorldsConfig()
    }

    /**
     * 清理字段中的颜色代码
     */
    override fun ignoreColor(string: String, world: String): String {
        return string.replace(MINECRAFT_COLOR_CODE, "")
    }

    /**
     * 设置全局物理规则状态。
     *
     * @param physical 新的物理规则状态。
     */
    override fun setPhysical(physical: Boolean?) {
        Companion.physical = physical
        config.setPhysical(physical)
        yaml.save(config)
    }

    /**
     * 设置指定世界的物理规则状态。
     *
     * @param world 世界名称。
     * @param physical 新的物理规则状态。
     */
    override fun setPhysical(world: String, physical: Boolean) {
        getWorldNode(world)?.physical = physical
    }

    /**
     * 获取指定世界的物理规则状态。
     *
     * @param world 世界名称。
     * @return 指定世界的物理规则状态。
     *
     */
    override fun isPhysical(world: String): Boolean {
        return getWorldNode(world)?.physical == true
    }

    /**
     * 设置全局爆炸破坏状态。
     *
     * @param exploded 新的爆炸破坏状态。
     */
    override fun setExploded(exploded: Boolean?) {
        Companion.exploded = exploded
        config.setExploded(exploded)
        yaml.save(config)
    }

    /**
     * 设置指定世界的爆炸破坏状态。
     *
     * @param world 世界名称。
     * @param exploded 新的爆炸破坏状态。
     */
    override fun setExploded(world: String, exploded: Boolean) {
        getWorldNode(world)?.exploded = exploded
    }

    /**
     * 获取指定世界的爆炸破坏状态。
     *
     * @param world 世界名称。
     * @return 指定世界的爆炸破坏状态。
     */
    override fun isExploded(world: String): Boolean {
        return getWorldNode(world)?.exploded == true
    }

    /**
     * 获取指定世界的权限组名称。
     *
     * @param world 世界名称。
     * @return 权限组名称，如果世界未指定权限组，则返回null。
     */
    override fun getWorldGroupName(world: String): String? {
        return nodeMap[world]?.group
    }

    /**
     * 获取权限组所包含的世界。
     *
     * @param group 权限组名称。
     * @return 世界列表，若权限组下无世界，则返回null。
     */
    override fun getWorldListByGroup(group: String): List<String>? {
        return groupMap[group]?.worlds?.toList()
    }

    /**
     * 获取指定世界的管理员集合。
     *
     * @param world 指定的世界对象。
     * @return 该世界的管理员用户名集合。
     */
    override fun getAdminSet(world: World): MutableSet<String>? {
        return getWorldGroup(world.name)?.admins
    }

    /**
     * 获取指定世界的建筑师集合。
     *
     * @param world 指定的世界对象。
     * @return 该世界的建筑师用户名集合。
     */
    override fun getBuilderSet(world: World): MutableSet<String>? {
        return getWorldGroup(world.name)?.builders
    }

    /**
     * 获取指定世界的访客集合。
     *
     * @param world 指定的世界对象。
     * @return 该世界的访客用户名集合。
     */
    override fun getVisitorSet(world: World): MutableSet<String>? {
        return getWorldNode(world.name)?.visitors
    }

    private fun addAdmin(world: String, player: String): Boolean {
        return getWorldGroup(world)?.addAdmin(player) == true
    }

    private fun addBuilder(world: String, player: String): Boolean {
        return getWorldGroup(world)?.addBuilder(player) == true
    }

    private fun addVisitor(world: String, player: String): Boolean {
        return getWorldNode(world)?.addVisitor(player) == true
    }

    private fun removeAdmin(world: String, player: String): Boolean {
        return getWorldGroup(world)?.removeAdmin(player) == true
    }

    private fun removeBuilder(world: String, player: String): Boolean {
        return getWorldGroup(world)?.removeBuilder(player) == true
    }

    private fun removeVisitor(world: String, player: String): Boolean {
        return getWorldNode(world)?.removeVisitor(player) == true
    }

    /**
     * 获取当前所有MapManager所管理的世界的节点映射。
     *
     * @return 包含所有MapManager所管理的世界及其对应节点信息的映射表。世界名称作为键，对应的[WorldNode]作为值。
     */
    override fun getNodeMap(): MutableMap<String, WorldNode?> {
        return nodeMap
    }

    /**
     * 获取当前所有权限组的映射。
     *
     * @return 包含所有权限组及其对应信息的映射表。权限组名称作为键，对应的[WorldGroup]作为值。
     */
    override fun getGroupMap(): MutableMap<String, WorldGroup?> {
        return groupMap
    }

    /**
     * 检查指定的世界是否被MapManager所管理。
     *
     * @param world 要检查的世界名称。
     * @return 如果指定的世界被MapManager所管理，返回true；否则返回false。
     */
    override fun containsWorld(world: String): Boolean {
        return nodeMap.containsKey(world)
    }

    /**
     * 获取当前所有MapManager所管理的世界的名称。
     *
     * @return 一个包含所有MapManager所管理的世界名称的集合。
     */
    override fun getWorlds(): MutableSet<String> {
        return nodeMap.keys
    }

    companion object {
        private val MINECRAFT_COLOR_CODE = Regex("(?i)(?:[&§]x(?:[&§][0-9a-f]){6}|[&§]#[0-9a-f]{6}|[&§][0-9a-fk-or])")

        // 全局物理效果设置。当设置为true时，启用全局物理效果；否则禁用。
        private var physical: Boolean? = null

        // 全局爆炸效果设置。当设置为true时，启用全局爆炸效果；否则禁用。
        private var exploded: Boolean? = null
    }
}
