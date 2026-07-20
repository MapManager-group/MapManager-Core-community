package work.alsace.mapmanager.service

import net.luckperms.api.model.user.User
import org.bukkit.Location
import org.mvplugins.multiverse.core.world.MultiverseWorld
import org.mvplugins.multiverse.core.world.WorldManager
import org.mvplugins.multiverse.external.vavr.control.Option
import work.alsace.mapmanager.enums.MMWorldType

/**
 * 动态世界管理器，提供世界的加载、卸载和管理功能。
 * @author CHuNan，Hanamizu
 */
interface DynamicWorld {
    /**
     * 获取Multiverse-Core的世界管理器。
     * @return MVWorldManager实例，如果Multiverse-Core插件不存在则为null。
     */
    fun getMVWorldManager(): WorldManager?

    /**
     * 检查指定名称的世界是否已加载。
     * @param name 世界的名称。
     * @return 如果世界已加载，返回true；否则返回false。
     */
    fun hasLoaded(name: String): Boolean

    /**
     * 检查指定名称的世界是否存在。
     * @param name 世界的名称。
     * @return 如果世界存在（无论是否已加载），返回true；否则返回false。
     */
    fun isExist(name: String): Boolean

    /**
     * 将指定名称的世界标记为已加载。
     * @param world 世界的名称。
     */
    fun loadAlready(world: String)

    /**
     * 尝试加载指定名称的世界。
     * @param name 世界的名称。
     * @return 如果成功加载，返回true；否则返回false。
     */
    fun loadWorld(name: String): Boolean

    /**
     * 在一定时间后卸载指定名称的世界。
     * @param name 世界的名称。
     */
    fun unloadWorldLater(name: String)

    /**
     * 检查指定名称的世界是否为额外加载的世界。
     * @param name 世界的名称。
     * @return 如果世界是额外加载的，返回true；否则返回false。
     */
    fun isExtraLoad(name: String): Boolean

    /**
     * 获取已加载的世界的MultiverseWorld实例。
     * @param name 世界的名称。
     * @return 对应的MultiverseWorld实例，如果未找到则返回null。
     */
    fun getLoadedWorld(name: String): MultiverseWorld?

    /**
     * 获取与给定名称匹配的正确的世界名称。
     * 如果给定名称的世界已加载，返回其准确名称；否则尝试匹配未加载的世界名称。
     * @param name 世界名称。
     * @return 匹配的世界名称，如果未找到则返回null。
     */
    fun getCorrectName(name: String): String?

    /**
     * 获取未加载的世界中与给定名称匹配的正确名称。
     * @param name 世界名称。
     * @return 匹配的未加载世界名称，如果未找到则返回null。
     */
    fun getCorrectUnloadedName(name: String): String?

    /**
     * 取消指定世界的延迟卸载任务。
     * 如果指定世界有一个待执行的卸载任务，该任务将被取消。
     * @param name 世界的名称。
     */
    fun cancelUnloadTask(name: String)

    /**
     * 根据前缀获取已加载和未加载的所有世界的名称列表。
     * @param prefix 世界名称的前缀。
     * @return 匹配前缀的所有世界名称列表。
     */
    fun getWorlds(prefix: String): MutableList<String>

    /**
     * 获取该玩家管理的所有世界的名称列表。
     * @param player 玩家名称。
     * @return 玩家管理的所有世界名称列表。
     */
    fun getOwnerWorlds(player: String): List<String>

    /**
     * 获取玩家可以进入的所有世界。
     * @param name 玩家名。
     * @return 玩家可进入的所有世界。
     */
    fun getAccessWorlds(name: String): List<String>

    /**
     * 获取与给定名称精确匹配的MultiverseWorld实例。
     * @param name 世界的名称。
     * @return 对应的MultiverseWorld实例，如果未找到则返回null。
     */
    fun getCorrectWorld(name: String): MultiverseWorld?

    /**
     * 从Multiverse-Core中彻底移除指定名称的世界。
     * @param world 要移除的世界名称。
     * @return 如果成功移除，返回true；否则返回false。
     */
    fun removeWorld(world: String): Boolean

    /**
     * 获取可能存在的世界名称集合。
     * @return 包含所有潜在世界名称的集合。
     */
    fun getPotentialWorlds(): MutableCollection<String?>?

    /**
     * 获取指定名称的MultiverseWorld实例。
     * @param world 世界的名称。
     * @return 对应的MultiverseWorld实例，如果未找到则返回null。
     */
    fun getMVWorld(world: String): MultiverseWorld

    /**
     * 获取服务器默认世界的出生点位置。
     * @return 服务器默认世界的出生点Location实例。
     */
    fun getSpawnLocation(): Location?

    /**
     * 导入指定名称的世界。
     * @param name 世界的名称。
     * @param alias 世界的别名。
     * @param color 世界名称的颜色。
     * @return 如果成功导入，返回true；否则返回false。
     */
    fun importWorld(name: String, alias: String, color: String): Boolean

    /**
     * 导入指定名称指定类型的世界。
     * @param name 世界的名称。
     * @param alias 世界的别名。
     * @param color 世界名称的颜色。
     * @param generate 世界的生成器类型。
     * @return 如果成功导入，返回true；否则返回false。
     */
    fun importWorld(name: String, alias: String, color: String, generate: MMWorldType): Boolean

    /**
     * 创建一个指定类型的新世界。
     * @param name 世界的名称。
     * @param alias 世界的别名。
     * @param color 世界名称的颜色。
     * @param generate 世界的生成器类型。
     * @return 如果成功创建，返回true；否则返回false。
     */
    fun createWorld(name: String, alias: String, color: String, generate: MMWorldType): Boolean

    /**
     * 判断玩家是否有权限
     * @param user 玩家Luckperms实体
     * @param permission 权限节点
     * @return 结果
     */
    fun hasPermission(user: User, permission: String): Boolean

    /**
     * 初始化地图配置
     * @param world MultiverseWorld mv世界
     * @param alias String 别名
     */
    fun initWorld(world: MultiverseWorld, alias: String)

    /**
     * 初始化地图配置
     * @param world MultiverseWorld mv世界
     */
    fun initWorld(world: MultiverseWorld)
}
