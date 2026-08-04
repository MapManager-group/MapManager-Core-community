package work.alsace.mapmanager.common.command

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player
import work.alsace.mapmanager.MapManagerImpl
import work.alsace.mapmanager.enums.MapGroup
import work.alsace.mapmanager.service.DynamicWorld
import work.alsace.mapmanager.service.MapAgent
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.stream.Collectors
import java.util.stream.Stream

class WorldCommand(private val plugin: MapManagerImpl) : TabExecutor {
    private var args: Array<String>? = null
    private var sender: CommandSender? = null
    private val dynamicWorld: DynamicWorld = plugin.getDynamicWorld()
    private val mapAgent: MapAgent = plugin.getMapAgent()
    private val subCommand1: MutableList<String> = mutableListOf(
        "admin",
        "admins",
        "builder",
        "builders",
        "visitor",
        "visitors",
        "explosion",
        "physics",
        "pvp",
        "public",
        "tp",
        "kick",
        "setspawn",
        "setname",
    )
    private val subCommandToggle: MutableList<String> =
        mutableListOf("on", "enable", "true", "yes", "off", "disable", "false", "no", "info", "status")
    private val emptyList: MutableList<String?> = ArrayList(0)

    private val cmdGuide: Component = Component.text("MapManager 命令帮助：", NamedTextColor.DARK_AQUA)
        .append(
            Component.text(
                "点我跳转到命令指南",
                NamedTextColor.AQUA,
                TextDecoration.UNDERLINED
            )
                .clickEvent(ClickEvent.openUrl("https://www.alsace.team/uj?url=command-guide"))
        )

    enum class Operation {
        ENABLE,
        STATUS,
        DISABLE
    }

    private fun getOperation(name: String): Operation {
        return when (name.lowercase(Locale.getDefault())) {
            "on", "enable", "true", "yes" -> {
                Operation.ENABLE
            }

            "off", "disable", "false", "no" -> {
                Operation.DISABLE
            }

            else -> {
                Operation.STATUS
            }
        }
    }

    private fun getList(prefix: String, collection: MutableCollection<String>): MutableList<String?>? {
        return collection.stream().filter { s: String ->
            s.lowercase(
                Locale.getDefault()
            ).startsWith(prefix)
        }.collect(Collectors.toList())
    }

    private fun getList(prefix: String, vararg args: String): MutableList<String?>? {
        return Stream.of(*args).filter { s: String ->
            s.lowercase(
                Locale.getDefault()
            ).startsWith(prefix)
        }.collect(Collectors.toList())
    }

    override fun onTabComplete(
        sender: CommandSender,
        cmd: Command,
        label: String,
        args: Array<String>
    ): MutableList<String?>? {
        return if (sender !is Player) emptyList else when (args.size) {
            1 -> {
                val prefix = args[0].lowercase(Locale.getDefault())
                if (hasPermission(sender)) getList(prefix, subCommand1) else getList(
                    prefix,
                    "builders",
                    "admins",
                    "visitors"
                )
            }

            2 -> {
                val prefix = args[1].lowercase(Locale.getDefault())
                if (hasPermission(sender)) {
                    when (args[0].lowercase(Locale.getDefault())) {
                        "admin", "builder", "visitor" -> {
                            getList(prefix, "add", "remove")
                        }

                        "blockupdate", "physics", "explosion", "pvp", "public" -> {
                            if (args[1].isEmpty()) mutableListOf("true", "false", "info") else getList(
                                prefix,
                                subCommandToggle
                            )
                        }

                        "kick" -> {
                            null
                        }

                        "tp" -> {
                            dynamicWorld.getWorlds("").stream()
                                .filter { world: String -> sender.hasPermission("multiverse.access.$world") }
                                .collect(Collectors.toList())
                        }

                        else -> {
                            emptyList
                        }
                    }
                } else emptyList
            }

            3 -> {
                //final WorldNode node = mapAgent.getWorldNode(player.getWorld().getName());
                if (sender.hasPermission("mapmanager.admin." + mapAgent.getWorldGroupName(sender.world.name))) {
                    if (args[1].equals("add", ignoreCase = true)) {
                        when (args[0].lowercase(Locale.getDefault())) {
                            "admin", "builder", "visitor" -> null
                            else -> emptyList
                        }
                    } else if (args[1].equals("remove", ignoreCase = true)) {
                        val prefix = args[2].lowercase(Locale.getDefault())
                        when (args[0].lowercase(Locale.getDefault())) {
                            "admin" -> getList(
                                prefix,
                                mapAgent.getAdminSet(sender.world)!!
                            )!!

                            "builder" -> getList(prefix, mapAgent.getBuilderSet(sender.world)!!)!!
                            "visitor" -> getList(prefix, mapAgent.getVisitorSet(sender.world)!!)!!
                            else -> emptyList
                        }
                    } else emptyList
                } else emptyList
            }

            else -> {
                emptyList
            }
        }
    }

    override fun onCommand(sender: CommandSender, cmd: Command, label: String, args: Array<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§c该指令仅限玩家执行")
            return true
        }
        if (!sender.hasPermission("mapmanager.world")) {
            sender.sendMessage("§c你没有权限使用此命令")
            return true
        }
        this.sender = sender
        this.args = args
        if (notEnough(1)) return true
        when (args[0].lowercase(Locale.getDefault())) {
            "admin" -> {
                if (noPermission(sender)) return false
                if (notEnough(3)) {
                    sender.sendMessage("§c给某玩家添加/移除管理员权限： /world admin add/remove <玩家id>")
                    return false
                }
                if (!mapAgent.isPlayerRegister(args[2])) {
                    sender.sendMessage("§a玩家" + args[2] + "不存在")
                }
                if (args[1].equals("add", ignoreCase = true)) {
                    if (mapAgent.addPlayer(
                            sender.world.name,
                            MapGroup.ADMIN,
                            args[2]
                        )
                    ) sender.sendMessage("§a已将玩家" + args[2] + "设置为该地图的管理员") else sender.sendMessage(
                        "§c权限修改时出现错误，请联系管理员以修复该错误"
                    )
                } else if (args[1].equals("remove", ignoreCase = true)) {
                    if (mapAgent.removePlayer(
                            sender.world.name,
                            MapGroup.ADMIN,
                            args[2]
                        )
                    ) {
                        sender.sendMessage("§a已取消玩家" + args[2] + "在该地图内的管理员资格")
                    } else {
                        sender.sendMessage(
                            "§c权限修改时出现错误，请联系管理员以修复该错误"
                        )
                    }
                } else {
                    sender.sendMessage("§c给某玩家添加/移除管理员权限： /world admin add/remove <玩家id>")
                }
            }

            "admins" -> {
                val admins: MutableSet<String> = try {
                    mapAgent.getPlayers(mapAgent.getWorldGroupName(sender.world.name)!!, MapGroup.ADMIN).get()
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                    sender.sendMessage("§c管理员列表查询失败，请联系管理员以解决该问题")
                    return false
                } catch (e: ExecutionException) {
                    e.printStackTrace()
                    sender.sendMessage("§c管理员列表查询失败，请联系管理员以解决该问题")
                    return false
                }
                if (admins.isEmpty()) {
                    sender.sendMessage("§7该地图暂未设置管理员")
                    return false
                }
                sender.sendMessage("§b有下列玩家为该地图的管理员（共" + admins.size + "人）：")
                if (hasPermission(sender)) listMembers(
                    admins,
                    NamedTextColor.DARK_AQUA,
                    NamedTextColor.AQUA,
                    "admin",
                    "管理员",
                    sender
                ) else for (name in admins) sender.sendMessage(
                    "§3> §b$name"
                )
            }

            "builder" -> {
                if (noPermission(sender)) return false
                if (notEnough(3)) {
                    sender.sendMessage("§c给某玩家添加/移除建筑权限： /world builder add/remove <玩家id>")
                    return false
                }
                if (!mapAgent.isPlayerRegister(args[2])) {
                    sender.sendMessage("§a玩家" + args[2] + "不存在")
                }
                if (args[1].equals("add", ignoreCase = true)) {
                    if (mapAgent.addPlayer(
                            sender.world.name,
                            MapGroup.BUILDER,
                            args[2]
                        )
                    ) {
                        sender.sendMessage("§a已将玩家" + args[2] + "设置为该地图的建筑人员")
                    } else {
                        sender.sendMessage(
                            "§c权限修改时出现错误，请联系管理员以修复该错误"
                        )
                    }
                } else if (args[1].equals("remove", ignoreCase = true)) {
                    if (mapAgent.removePlayer(
                            sender.world.name,
                            MapGroup.BUILDER,
                            args[2]
                        )
                    ) {
                        sender.sendMessage("§a已取消玩家" + args[2] + "在该地图内的建筑资格")
                    } else {
                        sender.sendMessage(
                            "§c权限修改时出现错误，请联系管理员以修复该错误"
                        )
                    }
                } else {
                    sender.sendMessage("§c给某玩家添加/移除管理员权限： /world builder add/remove <玩家id>")
                }
            }

            "builders" -> {
                val builders: MutableSet<String> = try {
                    mapAgent.getPlayers(mapAgent.getWorldGroupName(sender.world.name)!!, MapGroup.BUILDER).get()
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                    sender.sendMessage("§c管理员列表查询失败，请联系管理员以解决该问题")
                    return false
                } catch (e: ExecutionException) {
                    e.printStackTrace()
                    sender.sendMessage("§c管理员列表查询失败，请联系管理员以解决该问题")
                    return false
                }
                if (builders.isEmpty()) {
                    sender.sendMessage("§7该地图暂未设置建筑人员")
                    return false
                }
                sender.sendMessage("§e有下列玩家为该地图的建筑人员（共" + builders.size + "人）：")
                if (hasPermission(sender)) listMembers(
                    builders,
                    NamedTextColor.GOLD,
                    NamedTextColor.YELLOW,
                    "builder",
                    "建筑人员",
                    sender
                ) else for (name in builders) sender.sendMessage(
                    "§6> §e$name"
                )
            }

            "visitor" -> {
                if (noPermission(sender)) return false
                if (notEnough(3)) {
                    sender.sendMessage("§c给某玩家添加/移除参观权限： /world visitor add/remove <玩家id>")
                    return false
                }
                if (!mapAgent.isPlayerRegister(args[2])) {
                    sender.sendMessage("§a玩家" + args[2] + "不存在")
                }
                if (args[1].equals("add", ignoreCase = true)) {
                    if (mapAgent.addPlayer(
                            sender.world.name,
                            MapGroup.VISITOR,
                            args[2]
                        )
                    ) sender.sendMessage("§a玩家" + args[2] + "现在可以来参观你的地图了") else {
                        sender.sendMessage(
                            "§c权限修改时出现错误，请联系管理员以修复该错误"
                        )
                    }
                } else if (args[1].equals("remove", ignoreCase = true)) {
                    if (mapAgent.removePlayer(
                            sender.world.name,
                            MapGroup.VISITOR,
                            args[2]
                        )
                    ) sender.sendMessage("§a玩家" + args[2] + "将不能参观你的地图了") else sender.sendMessage("§c权限修改时出现错误，请联系管理员以修复该错误")
                } else {
                    sender.sendMessage("§c给某玩家添加/移除参观权限： /world visitor add/remove <玩家id>")
                }
            }

            "visitors" -> {
                if (mapAgent.isPublic(sender.world.name)) {
                    sender.sendMessage("§a该地图允许任何玩家进来参观")
                    return false
                }
                val visitors: MutableSet<String> = try {
                    mapAgent.getPlayers(sender.world.name, MapGroup.VISITOR).get()
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                    sender.sendMessage("§c参观人员列表查询失败，请联系管理员以解决该问题")
                    return false
                } catch (e: ExecutionException) {
                    e.printStackTrace()
                    sender.sendMessage("§c参观人员列表查询失败，请联系管理员以解决该问题")
                    return false
                }
                if (visitors.isEmpty()) {
                    sender.sendMessage("§7该地图暂未设置可进来参观的玩家")
                    return false
                }
                sender.sendMessage("§a有下列玩家可进来参观该地图（共" + visitors.size + "人）：")
                if (hasPermission(sender)) listMembers(
                    visitors,
                    NamedTextColor.DARK_GREEN,
                    NamedTextColor.GREEN,
                    "visitor",
                    "参观",
                    sender
                ) else for (name in visitors) sender.sendMessage(
                    "§2> §a$name"
                )
            }

            "kick" -> {
                if (noPermission(sender)) return false
                if (notEnough(2)) {
                    sender.sendMessage("§c将某玩家请出当前世界： /world kick <玩家id>")
                    return false
                }
                //kick the player out of the world
                val kicked = args[1].let { Bukkit.getServer().getPlayer(it) }
                if (kicked == null) {
                    sender.sendMessage("§c玩家+" + args[1] + "不在线！")
                    return false
                }
                if (kicked.world.uid != sender.world.uid) {
                    sender.sendMessage("§c玩家" + args[1] + "未在你的世界中")
                    return false
                }
                dynamicWorld.getDefaultSpawnLocation()?.let { kicked.teleport(it) }
                kicked.sendMessage("§c你被" + sender.name + "从他的世界中请出")
                sender.sendMessage("§a已将玩家" + args[1] + "从你的世界中请出")
            }

            "setspawn" -> {
                if (noPermission(sender)) return false
                //set world spawn point
                val loc: Location = sender.location
                dynamicWorld.getMVWorldManager()?.getWorld(sender.world)?.get()?.let { it.spawnLocation = loc }
                sender.world.spawnLocation = loc
                sender.sendMessage("§a已将世界出生点设置为： (" + loc.blockX + ", " + loc.blockY + ", " + loc.blockZ + ')')
            }

            "setname", "rename" -> {
                if (noPermission(sender)) return false
                if (notEnough(2)) {
                    sender.sendMessage("§c重命名当前世界（不支持颜色代码）： /world setname <名称>")
                    return false
                }
                //rename
                if (!checkLength(args[1])) {
                    sender.sendMessage("§c名称过长，最多允许16个字符")
                    return false
                }
                val world = dynamicWorld.getMVWorld(sender.world.name)
                if (world != null) {
                    mapAgent.setWorldAlias(world.name, args[1])
                }
                sender.sendMessage("§a已将世界名称修改为： ${args.contentToString()}[1]")
            }

            "blockupdate", "physics", "physical" -> {
                if (noPermission(sender)) return false
                if (args.size < 2) {
                    //show the status;
                    sender.sendMessage("§b当前地图已 " + (if (mapAgent.isPhysical(sender.world.name)) "开启" else "关闭") + " 方块更新")
                    return false
                }
                when (getOperation(args[1])) {
                    Operation.ENABLE -> {
                        //set to true
                        mapAgent.setPhysical(sender.world.name, true)
                        sender.sendMessage("§a已开启方块更新")
                    }

                    Operation.DISABLE -> {
                        //set to false
                        mapAgent.setPhysical(sender.world.name, false)
                        sender.sendMessage("§a已关闭方块更新")
                    }

                    Operation.STATUS -> {
                        //show the status
                        sender.sendMessage("§b当前地图已 " + (if (mapAgent.isPhysical(sender.world.name)) "开启" else "关闭") + " 方块更新")
                    }
                }
            }

            "explosion", "exploded" -> {
                if (noPermission(sender)) return false
                if (args.size < 2) {
                    //show the status;
                    sender.sendMessage("§b当前地图已 " + (if (mapAgent.isExploded(sender.world.name)) "开启" else "关闭") + " 爆炸破坏")
                    return false
                }
                when (getOperation(args[1])) {
                    Operation.ENABLE -> {
                        //set to true
                        mapAgent.setExploded(sender.world.name, true)
                        sender.sendMessage("§a已开启爆炸破坏")
                    }

                    Operation.DISABLE -> {
                        //set to false
                        mapAgent.setExploded(sender.world.name, false)
                        sender.sendMessage("§a已关闭爆炸破坏")
                    }

                    Operation.STATUS -> {
                        //show the status
                        sender.sendMessage("§b当前地图已 " + (if (mapAgent.isExploded(sender.world.name)) "开启" else "关闭") + " 爆炸破坏")
                    }
                }
            }

            "pvp" -> {
                if (noPermission(sender)) return false
                if (args.size < 2) {
                    //show the status;
                    sender.sendMessage(
                        "§b当前地图已 "
                                + (if (dynamicWorld.getMVWorld(sender.world.name)
                                ?.pvp == true
                        ) "开启" else "关闭")
                                + " PVP"
                    )
                    return false
                }
                when (getOperation(args[1])) {
                    Operation.ENABLE -> {
                        //set to true
                        dynamicWorld.getMVWorld(sender.world.name)?.pvp = true
                        sender.sendMessage("§a已开启PVP")
                    }

                    Operation.DISABLE -> {
                        //set to false
                        dynamicWorld.getMVWorld(sender.world.name)?.pvp = false
                        sender.sendMessage("§a已关闭PVP")
                    }

                    Operation.STATUS -> {
                        //show the status
                        sender.sendMessage(
                            "§b当前地图已 "
                                    + (if (dynamicWorld.getMVWorld(sender.world.name)
                                    ?.pvp == true
                            ) "开启" else "关闭")
                                    + " PVP"
                        )
                    }
                }
            }

            "public" -> {
                if (noPermission(sender)) return false
                if (args.size < 2) {
                    //show the status;
                    sender.sendMessage("§b当前地图 " + if (mapAgent.isPublic(sender.world.name)) "已公开" else "未公开")
                    return false
                }
                when (getOperation(args[1])) {
                    Operation.ENABLE -> {
                        //set to true
                        if (mapAgent.publicizeWorld(sender.world.name)) sender.sendMessage("§a已公开地图") else sender.sendMessage(
                            "§c公开失败，请联系管理员以解决该问题"
                        )
                    }

                    Operation.DISABLE -> {
                        //set to false
                        if (mapAgent.privatizeWorld(sender.world.name)) sender.sendMessage("§a已取消公开地图") else sender.sendMessage(
                            "§c取消公开失败，请联系管理员以解决该问题"
                        )
                    }

                    Operation.STATUS -> {
                        //show the status
                        sender.sendMessage("§b当前地图已 " + if (mapAgent.isPublic(sender.world.name)) "公开" else "未公开")
                    }
                }
            }

            "tp" -> {
                if (notEnough(2)) {
                    sender.sendMessage("§c传送到指定世界 /world tp <世界id>")
                    return false
                }
                val name = args[1].lowercase(Locale.getDefault())
                if (!sender.hasPermission("multiverse.access.$name")) {
                    sender.sendMessage("§c你没有权限进入此地图")
                    return false
                }
                val worldManager = dynamicWorld.getMVWorldManager()
//              获取已加载的世界实例
                val loadedWorldOption = worldManager?.getLoadedWorld(name)
                loadedWorldOption?.let {
                    // 未加载，获取世界实例
                    if (it.isEmpty) {
                        if(!dynamicWorld.isExist(name)) {
                            sender.sendMessage("§c世界 $name 不存在")
                            return false
                        }
                        sender.sendMessage("§e加载世界中，请稍后...")
                        if (!dynamicWorld.loadWorld(name)) {
                            sender.sendMessage("§c世界 $name 加载失败，请联系管理员以解决该问题")
                            return false
                        }
                        val loadedWorld = worldManager.getWorld(name).get()
                        sender.sendMessage("§a世界加载完毕")
                        sender.sendMessage("§e正在传送...")
                        sender.teleport(loadedWorld.spawnLocation)
                    } else {
                        sender.sendMessage("§e正在传送...")
                        val loadedWorld = worldManager.getWorld(name).get()
                        sender.teleport(loadedWorld.spawnLocation)
                    }
                }

//                var mvworld = dynamicWorld.getLoadedWorld(name)
//                if (mvworld == null) {
//                    val correct = dynamicWorld.getCorrectUnloadedName(name)
//                    if (correct == null) {
//                        sender.sendMessage("§c未找到世界" + args[1])
//                        return false
//                    }
//                    sender.sendMessage("§e加载世界中，请稍后...")
//                    if (!dynamicWorld.loadWorld(correct)) {
//                        sender.sendMessage("§c世界" + correct + "加载失败，请联系管理员以解决该问题")
//                        return false
//                    }
//                    sender.sendMessage("§a世界加载完毕")
//                    mvworld = dynamicWorld.getMVWorld(correct)
//                }
//                sender.sendMessage("§e正在传送...")
//                if (mvworld != null) {
//                    dynamicWorld.getSpawnLocation()?.let { sender.teleport(it) }
//                }
            }

            else -> {
                sender.sendMessage("§c未知操作，请点击下方链接查阅命令指南以获取帮助")
                sender.sendMessage(cmdGuide)
            }
        }
        return true
    }

    private fun noPermission(player: Player?): Boolean {
        if (!hasPermission(player)) {
            player?.sendMessage("§c你没有权限使用此命令")
            return true
        }
        return false
    }

    private fun hasPermission(player: Player?): Boolean {
        if (player != null) {
            return player.hasPermission("mapmanager.admin." + mapAgent.getWorldGroupName(player.world.name))
        }
        return false
    }

    private fun notEnough(n: Int): Boolean {
        if (args!!.size < n) {
            sender?.sendMessage("§c参数不足，请补全参数")
            return true
        }
        return false
    }

    private fun checkLength(str: String?): Boolean {
        if (str != null) {
            if (str.length > 16) return false
        }
        var length = 0
        if (str != null) {
            for (i in str.indices) {
                length += if (str[i].code > 255) 2 else 1
                if (length > 16) return false
            }
        }
        return true
    }

    private fun listMembers(
        set: MutableSet<String>,
        a: TextColor,
        b: TextColor,
        group: String,
        perm: String,
        sender: CommandSender
    ) {
        for (name in set) {
            if (name.isEmpty()) continue
            val prefix: Component = Component.text("> ", a)
            val body: Component = Component.text(name, b)
                .hoverEvent(HoverEvent.showText(Component.text("点击此处以取消" + name + "的" + perm + "资格")))
                .clickEvent(ClickEvent.suggestCommand("/world $group remove $name"))
            sender.sendMessage(prefix.append(body))
        }
    }

}
