package work.alsace.mapmanager.common.function

import net.luckperms.api.model.user.User
import org.bukkit.*
import org.bukkit.entity.SpawnCategory
import org.bukkit.scheduler.BukkitRunnable
import org.mvplugins.multiverse.core.config.handle.PropertyModifyAction
import org.mvplugins.multiverse.core.world.MultiverseWorld
import org.mvplugins.multiverse.core.world.WorldManager
import org.mvplugins.multiverse.core.world.options.*
import work.alsace.mapmanager.MapManagerImpl
import work.alsace.mapmanager.enums.MMWorldType
import work.alsace.mapmanager.service.DynamicWorld
import java.io.File
import java.util.*


/**
 * 动态世界管理器，提供世界的加载、卸载和管理功能。
 */
class DynamicWorldImpl(private val plugin: MapManagerImpl) : DynamicWorld {
    private val mv = plugin.coreApi.worldManager
    private val tasks = HashMap<String, BukkitRunnable>()
    private val loaded = HashSet<String>()

    /**
     * 获取Multiverse-Core的世界管理器。
     * @return MVWorldManager实例，如果Multiverse-Core插件不存在则为null。
     */
    override fun getMVWorldManager(): WorldManager? {
        return mv
    }

    /**
     * 检查指定名称的世界是否已加载。
     * @param name 世界的名称。
     * @return 如果世界已加载，返回true；否则返回false。
     */
    override fun hasLoaded(name: String): Boolean {
        return mv.isLoadedWorld(name)
    }

    /**
     * 检查指定名称的世界是否存在。
     * @param name 世界的名称。
     * @return 如果世界存在（无论是否已加载），返回true；否则返回false。
     */
    override fun isExist(name: String): Boolean {
        return mv.isWorld(name)
    }

    /**
     * 将指定名称的世界标记为已加载。
     * @param world 世界的名称。
     */
    override fun loadAlready(world: String) {
        loaded.add(world)
    }

    /**
     * 尝试加载指定名称的世界。
     * @param name 世界的名称。
     * @return 如果成功加载，返回true；否则返回false。
     */
    override fun loadWorld(name: String): Boolean {
        return mv.getWorld(name)
            .map { mv.loadWorld(LoadWorldOptions.world(it)) }
            .map { result ->
                result.onFailure { failure -> plugin.logger.warning("$name 加载失败: ${failure.failureMessage}") }
                result.isSuccess
            }
            .getOrElse(false)
            .also { if (it) loaded.add(name) }
    }

    /**
     * 在一定时间后卸载指定名称的世界。
     * @param name 世界的名称。
     */
    override fun unloadWorldLater(name: String) {
        if (!loaded.contains(name) || tasks.containsKey(name)) return
        plugin.logger.info("$name 准备卸载")

        val runnable = object : BukkitRunnable() {
            override fun run() {
                val world = Bukkit.getWorld(name)
                if (world?.players?.isEmpty() == true) {
                    mv.getLoadedWorld(name).peek { mvWorld ->
                        mv.unloadWorld(UnloadWorldOptions.world(mvWorld))
                            .onFailure { failure -> plugin.logger.warning("$name 卸载失败: ${failure.failureMessage}") }
                            .onSuccess { _ ->
                                loaded.remove(name)
                                plugin.logger.info("$name 已卸载")
                            }
                    }
                }
                tasks.remove(name)
            }
        }

        runnable.runTaskLater(plugin, 12000)
        tasks[name] = runnable
    }

    /**
     * 检查指定名称的世界是否为额外加载的世界。
     * @param name 世界的名称。
     * @return 如果世界是额外加载的，返回true；否则返回false。
     */
    override fun isExtraLoad(name: String): Boolean {
        return loaded.contains(name)
    }

    /**
     * 获取已加载的世界的MultiverseWorld实例。
     * @param name 世界的名称。
     * @return 对应的MultiverseWorld实例，如果未找到则返回null。
     */
    override fun getLoadedWorld(name: String): MultiverseWorld? {
        return mv.getLoadedWorld(name).getOrNull()
    }

    /**
     * 获取与给定名称匹配的正确的世界名称。
     * 如果给定名称的世界已加载，返回其准确名称；否则尝试匹配未加载的世界名称。
     * @param name 世界名称。
     * @return 匹配的世界名称，如果未找到则返回null。
     */
    override fun getCorrectName(name: String): String? {
        return mv.getWorld(name).getOrNull()?.name
    }

    /**
     * 获取未加载的世界中与给定名称匹配的正确名称。
     * @param name 世界名称。
     * @return 匹配的未加载世界名称，如果未找到则返回null。
     */
    override fun getCorrectUnloadedName(name: String): String? {
        return mv.getUnloadedWorld(name).getOrNull()?.name
    }

    /**
     * 取消指定世界的延迟卸载任务。
     * 如果指定世界有一个待执行的卸载任务，该任务将被取消。
     * @param name 世界的名称。
     */
    override fun cancelUnloadTask(name: String) {
        if (tasks.containsKey(name)) {
            tasks[name]?.cancel()
            if (tasks.remove(name) != null) plugin.logger.warning(name + "已取消卸载")
        }
    }

    /**
     * 根据前缀获取已加载和未加载的所有世界的名称列表。
     * @param prefix 世界名称的前缀。
     * @return 匹配前缀的所有世界名称列表。
     */
    override fun getWorlds(prefix: String): MutableList<String> {
        val lowerPrefix = prefix.lowercase(Locale.ROOT)
        return mv.worlds.asSequence()
            .map { it.name }
            .filter { it.lowercase(Locale.ROOT).startsWith(lowerPrefix) }
            .toMutableList()
    }

    /**
     * 获取该玩家管理的所有世界的名称列表。
     * @param player 玩家名称。
     * @return 玩家管理的所有世界名称列表。
     */
    override fun getOwnerWorlds(player: String): List<String> {
        val luckPerms = plugin.getLuckPerms()
        val playerUuid = plugin.getMapAgent().getUniqueID(player) ?: return emptyList()

        plugin.logger.info(playerUuid.toString())

        val user = luckPerms.userManager.loadUser(playerUuid).join() ?: return emptyList()

        plugin.logger.info(user.username ?: "Unknown user")

        return getWorlds("")
            .mapNotNull { world ->
                plugin.getMapAgent().getWorldGroupName(world)
            }
            .filter { group ->
                hasPermission(user, "mapmanager.admin.$group")
            }
            .flatMap { group ->
                plugin.getMapAgent().getWorldListByGroup(group)?.asSequence() ?: emptySequence()
            }
            .distinct()
            .toList()
    }

    /**
     * 获取玩家可以进入的所有世界
     * @param name 玩家名
     * @return 玩家可进入的所有世界列表
     */
    override fun getAccessWorlds(name: String): List<String> {
        val luckPerms = plugin.getLuckPerms()
        val playerUuid = plugin.getMapAgent().getUniqueID(name) ?: return emptyList()

        val user = luckPerms.userManager.loadUser(playerUuid).join() ?: return emptyList()

        return getWorlds("")
            .filter { world ->
                hasPermission(user, "multiverse.access.$world")
            }
    }

    /**
     * 获取与给定名称精确匹配的MultiverseWorld实例。
     * @param name 世界的名称。
     * @return 对应的MultiverseWorld实例，如果未找到则返回null。
     */
    override fun getCorrectWorld(name: String): MultiverseWorld? {
        return mv.getWorld(name)?.get()
    }

    /**
     * 从Multiverse-Core中彻底移除指定名称的世界。
     * @param world 要移除的世界名称。
     * @return 如果成功移除，返回true；否则返回false。
     */
    override fun removeWorld(world: String): Boolean {
        return mv.getWorld(world)
            .map { mv.deleteWorld(DeleteWorldOptions.world(it)) }
            .map { result ->
                result.onFailure { failure -> plugin.logger.warning("地图 $world 删除失败: ${failure.failureMessage}") }
                result.onSuccess { deletedWorld -> plugin.logger.info("地图 $deletedWorld 已移除") }
                result.isSuccess
            }
            .getOrElse(false)
    }

    /**
     * 获取可能存在的世界名称集合。
     * @return 包含所有潜在世界名称的集合。
     */
    override fun getPotentialWorlds(): MutableCollection<String?> {
        val worldDir = File(plugin.server.worldContainer, "")
        if (!worldDir.exists()) return mutableListOf()
        val managedWorldNames = mv.worlds.map { it.name }.toSet()
        val systemFolders = setOf(
            "plugins", "logs", "config", "cache", "libraries", "versions",
            "crash-reports"
        )
        val fileNames = worldDir.listFiles()
            ?.asSequence()
            ?.filter { it.isDirectory }
            ?.filter { !managedWorldNames.contains(it.name) }
            ?.filter { !systemFolders.contains(it.name) }
            ?.filter { File(it, "level.dat").exists() }
            ?.map { it.name }
            ?.toMutableList()
        return fileNames ?: mutableListOf()
    }

    /**
     * 获取指定名称的MultiverseWorld实例。
     * @param world 世界的名称。
     * @return 对应的MultiverseWorld实例，如果未找到则返回null。
     */
    override fun getMVWorld(world: String): MultiverseWorld {
        return mv.getWorld(world).getOrNull()
            ?: error("Multiverse-Core 中不存在世界: $world")
    }

    /**
     * 获取世界的出生点位置。
     * @return 世界的出生点Location实例。
     */
    override fun getSpawnLocation(world: String): Location? {
        return mv.getWorld(world).getOrNull()?.spawnLocation
            ?: mv.defaultWorld.getOrNull()?.spawnLocation
    }

    /**
     * 获取服务器默认世界的出生点位置。
     * @return 服务器默认世界的出生点Location实例。
     */
    override fun getDefaultSpawnLocation(): Location? {
        return mv.defaultWorld.getOrNull()?.spawnLocation
    }

    /**
     * 导入指定名称的世界。
     * @param name 世界的名称。
     * @param alias 世界的别名。
     * @param color 世界名称的颜色。
     * @return 如果成功导入，返回true；否则返回false。
     */
    override fun importWorld(name: String, alias: String, color: String): Boolean {
        return importWorld(name, alias, color, MMWorldType.NORMAL)
    }

    /**
     * 导入指定名称指定类型的世界。
     * @param name 世界的名称。
     * @param alias 世界的别名。
     * @param color 世界名称的颜色。
     * @param generate 世界的生成器类型。
     * @return 如果成功导入，返回true；否则返回false。
     */
    override fun importWorld(name: String, alias: String, color: String, generate: MMWorldType): Boolean {
        //确认根目录下有要导入的文件
        val file = File(plugin.server.worldContainer, name)
        if (!file.exists()) {
            plugin.logger.warning("未找到世界文件$name")
            return false
        }
        //确认dimension目录下没有同名地图
        val defaultWorld = plugin.server.worlds[0].name
        val dimensionsFile = File(plugin.server.worldContainer, "$defaultWorld/dimensions/minecraft/$name")
        if (dimensionsFile.exists()) {
            plugin.logger.warning("世界" + name + "已经存在")
            return false
        }
        val versionCheck = plugin.getVersionCheck()
        if (!versionCheck.isMapVersionCorrect(name)) {
            return false
        }
        var gene = World.Environment.NORMAL
        when (generate) {
            MMWorldType.FLAT -> gene = World.Environment.NORMAL
            MMWorldType.VOID -> gene = World.Environment.NORMAL
            MMWorldType.NETHER -> gene = World.Environment.NETHER
            MMWorldType.END -> gene = World.Environment.THE_END
            MMWorldType.NORMAL -> World.Environment.NORMAL
        }
        val options = ImportWorldOptions.worldName(name)
            .environment(gene)
            .doFolderCheck(true)
            .generator("VoidGen:{}")

        return mv.importWorld(options).fold(
            { failure ->
                plugin.logger.warning("导入地图 $name 失败: ${failure.failureMessage}")
                false
            },
            { world ->
                initWorld(world, alias)
                true
            }
        )
    }

    /**
     * 创建一个新世界。
     * @param name 世界的名称。
     * @param alias 世界的别名。
     * @param color 世界名称的颜色。
     * @param generate 世界的生成器类型。
     * @return 如果成功创建，返回true；否则返回false。
     */
    override fun createWorld(name: String, alias: String, color: String, generate: MMWorldType): Boolean {
        val safeName = name.lowercase(Locale.getDefault())

        if (name != safeName) {
            plugin.logger.info("已自动转换为符合规范的小写名称: '$safeName'")
        }
        val defaultWorld = plugin.server.worlds[0].name
        val dimensionsFile = File(plugin.server.worldContainer, "$defaultWorld/dimensions/minecraft/$safeName")
        val rootFile = File(plugin.server.worldContainer, safeName)
        if (dimensionsFile.exists() || rootFile.exists()) {
            plugin.logger.warning("世界" + safeName + "已经存在")
            return false
        }
        val key = NamespacedKey.minecraft(safeName)
        val options = CreateWorldOptions.worldKey(key)
            .doFolderCheck(true)
            .generateStructures(false)
        when (generate) {
            MMWorldType.VOID -> options.generator("VoidGen:{}").worldType(WorldType.FLAT)
            MMWorldType.NORMAL -> options.worldType(WorldType.NORMAL)
            else -> options.worldType(WorldType.FLAT)
        }
        options.environment(
            when (generate) {
                MMWorldType.NETHER -> World.Environment.NETHER
                MMWorldType.END -> World.Environment.THE_END
                else -> World.Environment.NORMAL
            }
        )

        return mv.createWorld(options).fold(
            { failure ->
                plugin.logger.warning("创建地图 $safeName 失败: ${failure.failureMessage}")
                false
            },
            { world ->
                initWorld(world, alias)
                true
            }
        )
    }

    override fun initWorld(world: MultiverseWorld) {
        initWorld(world, world.name)
    }

    override fun initWorld(world: MultiverseWorld, alias: String) {
        world.alias = "&3${alias}"
        world.difficulty = Difficulty.PEACEFUL
        world.isAutoLoad = true
        world.isKeepSpawnInMemory = false
        world.gameMode = GameMode.CREATIVE
        initSpawn(world)

        val key = NamespacedKey.minecraft(world.name.lowercase(Locale.getDefault()))
        val w = Bukkit.getWorld(key)
        if (w == null) {
            plugin.logger.warning("地图初始化失败，无法找到世界${w}")
            return
        }
        w.setGameRule(GameRule.RANDOM_TICK_SPEED, 0)
        w.setGameRule(GameRule.DO_FIRE_TICK, false)
        w.setGameRule(GameRule.DO_WEATHER_CYCLE, false)
        w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false)
        w.setGameRule(GameRule.MOB_GRIEFING, false)
        w.setGameRule(GameRule.DO_MOB_SPAWNING, false)
        w.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS,false)
        val name = world.name
        name?.let { loadAlready(it) }
        mv.saveWorldsConfig()
    }

    private fun initSpawn(world: MultiverseWorld) {
        listOf(
            SpawnCategory.ANIMAL,
            SpawnCategory.WATER_ANIMAL,
            SpawnCategory.WATER_AMBIENT,
            SpawnCategory.MONSTER,
            SpawnCategory.WATER_UNDERGROUND_CREATURE,
            SpawnCategory.AMBIENT,
            SpawnCategory.AXOLOTL
        ).forEach { category ->
            world.entitySpawnConfig.getSpawnCategoryConfig(category).apply {
                stringPropertyHandle.modifyPropertyString("spawn", "false", PropertyModifyAction.SET)
            }
        }
        world.entitySpawnConfig.getSpawnCategoryConfig(SpawnCategory.MISC).apply {
            stringPropertyHandle.modifyPropertyString("spawn", "true", PropertyModifyAction.SET)
        }

    }

    /**
     * 判断玩家是否有权限
     * @param user 玩家LuckPerms实体
     * @param permission 权限节点
     * @return 结果
     */
    override fun hasPermission(user: User, permission: String): Boolean {
        return user.cachedData.permissionData.checkPermission(permission).asBoolean()
    }
}
