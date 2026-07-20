package work.alsace.mapmanager.common.command

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import work.alsace.mapmanager.MapManagerImpl
import work.alsace.mapmanager.service.DynamicWorld

class InitCommand(private val plugin: MapManagerImpl) : TabExecutor {
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
            sender.sendMessage("§c你没有权限使用此命令")
            return true
        }
        val name = args[0]

        val dynamicWorld = plugin.getDynamicWorld()
        if (dynamicWorld.isExist(name)) {
            dynamicWorld.initWorld(dynamicWorld.getMVWorld(name))
        }
        sender.sendMessage("§a已初始化地图")
        return true
    }
}
