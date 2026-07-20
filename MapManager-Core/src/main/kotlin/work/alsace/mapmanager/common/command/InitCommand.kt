package work.alsace.mapmanager.common.command

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player
import work.alsace.mapmanager.MapManagerImpl
import java.util.*
import java.util.stream.Collectors

class InitCommand(private val plugin: MapManagerImpl) : TabExecutor {
    private val emptyList: MutableList<String?> = ArrayList(0)
    override fun onTabComplete(
        sender: CommandSender,
        cmd: Command,
        label: String,
        args: Array<out String>
    ): MutableList<String?>? {
        if (!sender.hasPermission("mapmanager.command.init")) return emptyList
        val index = args.size.minus(1)
        return if (index.let { args[it].length } < 2) emptyList else when (args[index].substring(0, 2)) {
            "n:" -> {
                val prefix = args[index].lowercase(Locale.getDefault())
                plugin.getDynamicWorld().getPotentialWorlds()?.stream()
                    ?.map { s: String? -> "n:$s" }
                    ?.filter { s: String? -> s?.lowercase(Locale.getDefault())!!.startsWith(prefix) }
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

    override fun onCommand(sender: CommandSender, cmd: Command, p2: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mapmanager.command.init")) {
            sender.sendMessage("§c你没有权限使用此命令")
            return true
        }
        val name = args[0]

        val dynamicWorld = plugin.getDynamicWorld();
        if (dynamicWorld.isExist(name)) {
            dynamicWorld.initWorld(dynamicWorld.getMVWorld(name))
        }
        sender.sendMessage("§a已初始化地图")
        return true
    }
}
