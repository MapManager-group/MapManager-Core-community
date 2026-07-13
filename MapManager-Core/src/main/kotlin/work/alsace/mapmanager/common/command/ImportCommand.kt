package work.alsace.mapmanager.common.command

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player
import work.alsace.mapmanager.MapManagerImpl
import work.alsace.mapmanager.enums.MMWorldType
import java.util.*
import java.util.stream.Collectors

class ImportCommand(private val plugin: MapManagerImpl) : TabExecutor {
    private val emptyList: MutableList<String?> = ArrayList(0)
    override fun onTabComplete(
        sender: CommandSender,
        cmd: Command,
        label: String,
        args: Array<out String>
    ): MutableList<String?>? {
        if (!sender.hasPermission("mapmanager.command.import")) return emptyList
        val index = args.size.minus(1)
        return if (index.let { args[it].length } < 2) emptyList else when (args[index].substring(0, 2)) {
            "n:" -> {
                val prefix = args[index].lowercase(Locale.getDefault())
                plugin.getDynamicWorld().getPotentialWorlds()?.stream()
                    ?.map { s: String? -> "n:$s" }
                    ?.filter { s: String? -> s?.lowercase(Locale.getDefault())?.startsWith(prefix) == true }
                    ?.collect(Collectors.toList())
            }

            "e:" -> {
                val worldTypes = listOf("void_gen", "normal", "nether", "the_end", "flat")
                val prefix = args[index].lowercase(Locale.getDefault())
                worldTypes.stream()
                    .map { type -> "e:$type" }
                    .filter { type -> type.lowercase(Locale.getDefault()).startsWith(prefix) }
                    .collect(Collectors.toList())
            }

            "o:" -> {
                val prefix = args[index].lowercase(Locale.getDefault())
                plugin.server.onlinePlayers.stream()
                    .map { p: Player? -> "o:" + p?.name }
                    ?.filter { s: String? -> s?.lowercase(Locale.getDefault())!!.startsWith(prefix) }
                    ?.collect(Collectors.toList())
            }

            else -> {
                emptyList
            }
        }
    }

    override fun onCommand(sender: CommandSender, cmd: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mapmanager.command.import")) {
            sender.sendMessage("§c你没有权限使用此命令")
            return true
        }
        var name: String? = null
        var alias: String? = null
        var color = "darkaqua"
        var group: String? = null
        var owner: String? = null
        var generate: String? = null
        for (arg in args) {
            when (arg.take(2)) {
                "n:" -> name = arg.substring(2)
                "a:" -> alias = arg.substring(2)
                "c:" -> color = arg.substring(2)
                "g:" -> group = arg.substring(2)
                "o:" -> owner = arg.substring(2)
                "e:" -> generate = arg.substring(2)
                else -> {}
            }
        }
        if (name == null) {
            sender.sendMessage("§c未指定地图名")
            return true
        }
        if (owner == null) owner = sender.name
        if (alias == null) alias = name
        if (group == null) group = name.lowercase(Locale.getDefault())
        val generateType: MMWorldType = when (generate) {
            "void_gen" -> MMWorldType.VOID
            "normal" -> MMWorldType.NORMAL
            "nether" -> MMWorldType.NETHER
            "the_end" -> MMWorldType.END
            else -> MMWorldType.FLAT
        }
        if (!plugin.getDynamicWorld().importWorld(name, alias, color, generateType)) {
            sender.sendMessage("§c地图导入失败，请查看控制台以获取更多信息")
            return true
        }
        sender.sendMessage("§a已导入地图")
        plugin.getMapAgent().newWorld(name, owner, group)
        return true
    }
}
