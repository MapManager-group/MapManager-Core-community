package work.alsace.mapmanager.service

import net.luckperms.api.LuckPerms
import org.bukkit.ChatColor
import org.bukkit.World
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import work.alsace.mapmanager.enums.MapGroup
import work.alsace.mapmanager.pojo.WorldGroup
import work.alsace.mapmanager.pojo.WorldNode
import java.io.File
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * 地图管理代理，负责处理与LuckPerms权限插件的交互、管理世界及其权限组等功能。
 * @author CHuNan, Hanamizu, gaye
 */
interface MapAgent {
    /**
     * 创建一个新的世界，并将其注册到权限管理中。
     *
     * @param world 世界名称。
     * @param owner 世界拥有者的玩家名。
     * @param group 权限组名称。
     */
    fun newWorld(world: String, owner: String, group: String): CompletableFuture<Void>

    /**
     * 删除一个世界及其相关的权限信息。
     *
     * @param world 要删除的世界名称。
     * @return 如果成功删除，返回true；否则返回false。
     */
    fun deleteWorld(world: String): Boolean

    /**
     * 将玩家添加至地图权限组
     *
     * @param world  要添加权限的地图名
     * @param group  要添加权限的地图权限组，只能是ADMIN,BUILDER,VISITOR
     * @param player 玩家名
     * @return 添加结果，添加成功返回true，失败返回false
     */
    fun addPlayer(world: String, group: MapGroup, player: String): Boolean

    /**
     * 移除玩家在某地图的权限
     *
     * @param world  要移除权限的地图名
     * @param group  要移除权限的地图权限组，只能是ADMIN,BUILDER,VISITOR
     * @param player 玩家名称
     * @return 移除结果，移除成功返回true，失败返回false
     */
    fun removePlayer(world: String, group: MapGroup, player: String): Boolean

    /**
     * 将指定世界设置为公开状态。
     *
     * @param world 世界名称。
     * @return 操作成功返回true，否则返回false。
     */
    fun publicizeWorld(world: String): Boolean

    /**
     * 将指定世界设置为私有状态。
     *
     * @param world 世界名称。
     * @return 操作成功返回true，否则返回false。
     */
    fun privatizeWorld(world: String): Boolean

    /**
     * 判断指定的世界是否为公共世界。
     *
     * @param world 要检查的世界名称。
     * @return 如果世界为公共世界，返回true，否则返回false。
     */
    fun isPublic(world: String): Boolean

    /**
     * 检测玩家是否为地图管理员
     * @param player 玩家名称
     * @param world 世界名称
     * @return 是否为管理员，如果是返回true，否则返回false
     */
    fun isAdmin(player: String, world: String): Boolean

    /**
     * 获取指定名称的玩家集合，基于指定的权限组筛选。
     *
     * @param worldName   世界名称。
     * @param group  权限组枚举（ADMIN, BUILDER, VISITOR）。
     * @return 包含玩家名称的CompletableFuture实例。
     */
    fun getPlayers(worldName: String, group: MapGroup): CompletableFuture<MutableSet<String>>

    /**
     * 获取地图别名
     *
     * @param worldName 世界名
     * @return String 别名
     */
    fun getWorldAlias(worldName: String): String

    /**
     * 设置地图别名
     * @param worldName 地图名
     * @param alias 别名
     */
    fun setWorldAlias(worldName: String, alias: String)

    /**
     * 设置全局物理/方块更新规则状态。
     *
     * @param physical 新的物理/方块更新规则状态。
     */
    fun setPhysical(physical: Boolean?)

    /**
     * 设置指定世界的物理/方块更新规则状态。
     *
     * @param world 世界名称。
     * @param physical 新的物理/方块更新规则状态。
     */
    fun setPhysical(world: String, physical: Boolean)

    /**
     * 获取指定世界的物理/方块更新规则状态。
     *
     * @param world 世界名称。
     * @return 指定世界的物理/方块更新规则状态。
     */
    fun isPhysical(world: String): Boolean

    /**
     * 设置全局爆炸破坏状态。
     *
     * @param exploded 新的爆炸破坏状态。
     */
    fun setExploded(exploded: Boolean?)

    /**
     * 设置指定世界的爆炸破坏状态。
     *
     * @param world 世界名称。
     * @param exploded 新的爆炸破坏状态。
     */
    fun setExploded(world: String, exploded: Boolean)

    /**
     * 获取指定世界的爆炸破坏状态。
     *
     * @param world 世界名称。
     * @return 指定世界的爆炸破坏状态。
     */
    fun isExploded(world: String): Boolean


    /**
     * 获取指定世界的权限组名称。
     *
     * @param world 世界名称。
     * @return 权限组名称，如果世界未指定权限组，则返回null。
     */
    fun getWorldGroupName(world: String): String?

    /**
     * 获取权限组所包含的世界。
     *
     * @param group 权限组名称。
     * @return 世界列表，若权限组下无世界，则返回null。
     */
    fun getWorldListByGroup(group: String): List<String>?


    /**
     * 获取指定世界的管理员集合。
     *
     * @param world 指定的世界对象。
     * @return 该世界的管理员用户名集合。
     */
    fun getAdminSet(world: World): MutableSet<String>?

    /**
     * 获取指定世界的建筑师集合。
     *
     * @param world 指定的世界对象。
     * @return 该世界的建筑师用户名集合。
     */
    fun getBuilderSet(world: World): MutableSet<String>?

    /**
     * 获取指定世界的访客集合。
     *
     * @param world 指定的世界对象。
     * @return 该世界的访客用户名集合。
     */
    fun getVisitorSet(world: World): MutableSet<String>?

    /**
     * 获取当前所有MapManager所管理的世界的节点映射。
     *
     * @return 包含所有MapManager所管理的世界及其对应节点信息的映射表。世界名称作为键，对应的[WorldNode]作为值。
     */
    fun getNodeMap(): MutableMap<String, WorldNode?>

    /**
     * 获取当前所有权限组的映射。
     *
     * @return 包含所有权限组及其对应信息的映射表。权限组名称作为键，对应的[WorldGroup]作为值。
     */
    fun getGroupMap(): MutableMap<String, WorldGroup?>

    /**
     * 检查指定的世界是否被MapManager所管理。
     *
     * @param world 要检查的世界名称。
     * @return 如果指定的世界被MapManager所管理，返回true；否则返回false。
     */
    fun containsWorld(world: String): Boolean

    /**
     * 获取当前所有MapManager所管理的世界的名称。
     *
     * @return 一个包含所有MapManager所管理的世界名称的集合。
     */
    fun getWorlds(): MutableSet<String>

    /**
     * 获取玩家的UUID
     * @param player 玩家名称。
     * @return 返回玩家的UUID。
     */
    fun getUniqueID(player: String): UUID?

    /**
     * 获取服务器地图所在父目录
     * @return 返回服务器地图所在父目录
     */
    fun getWorldParentFolder(): File

    /**
     * 获取地图目录
     * @param world 地图名称
     * @return 返回地图目录
     */
    fun getWorldFolder(world: String): File

    /**
     * 通过玩家名获取服务器在线玩家
     * @param player 玩家id
     * @return Player 玩家实体
     */
    fun getPlayer(player: String): Player?

    /**
     * 判断玩家是否在服务器注册(登录过)
     * @param player 玩家id
     * @return Boolean 是否注册，true为注册，false为未注册。
     */
    fun isPlayerRegister(player: String): Boolean

    /**
     * 保存MapManager信息。
     * @return 保存的结果。
     */
    fun save(): Boolean

    /**
     * 重载MapManager
     */
    fun reload()

    /**
     * 设置LuckPerms实例
     */
    fun setLuckPerms(luckPerms: LuckPerms)

    /**
     * 与LuckPerms同步
     */
    fun syncWithLuckPerms(sender: CommandSender)

    /**
     * 忽略字符颜色
     * @param string String 字符串
     * @param world String 地图名
     * @return 返回的字符串
     */
    fun ignoreColor(string: String, world: String): String
}
