package work.alsace.mapmanager.common.command

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import work.alsace.mapmanager.MapManagerImpl
import work.alsace.mapmanager.service.DynamicWorld

class InitCommand(private val plugin: MapManagerImpl) : TabExecutor {
    private val messagePrefix = "§6[MapManager] §r"
    private val emptyList: MutableList<String?> = ArrayList(0)

    private val world: DynamicWorld = plugin.getDynamicWorld()

    override fun onTabComplete(
        sender: CommandSender,
        cmd: Command,
        label: String,
        args: Array<String>
    ): MutableList<out String?> {
        return if (sender.hasPermission("mapmanager.command.init") && args.size == 1) world.getWorlds(args[0]) else emptyList
    }

    override fun onCommand(sender: CommandSender, cmd: Command, p2: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mapmanager.command.init")) {
            sender.sendMessage("${messagePrefix}§c权限不足，无法执行此命令。")
            return true
        }
        if (args.isEmpty()) {
            sender.sendMessage("${messagePrefix}§c缺少地图名。")
            return true
        }
        val name = args[0]

        val dynamicWorld = plugin.getDynamicWorld()
        if (dynamicWorld.isExist(name)) {
            sender.sendMessage("${messagePrefix}§a地图已初始化。")
            dynamicWorld.initWorld(dynamicWorld.getMVWorld(name))
            return true
        }
        sender.sendMessage("${messagePrefix}§c地图不存在。")
        return true
    }
}
