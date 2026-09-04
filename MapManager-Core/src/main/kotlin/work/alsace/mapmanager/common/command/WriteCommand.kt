package work.alsace.mapmanager.common.command

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import work.alsace.mapmanager.MapManagerImpl

class WriteCommand(private val plugin: MapManagerImpl) : CommandExecutor {
    private val messagePrefix = "§6[MapManager] §r"
    override fun onCommand(sender: CommandSender, cmd: Command, label: String, args: Array<String>): Boolean {
        if (!sender.hasPermission("mapmanager.command.write")) {
            sender.sendMessage("${messagePrefix}§c权限不足，无法执行此命令。")
            return true
        }
        sender.sendMessage("${messagePrefix}§bWorld nodes: ${plugin.getMapAgent().getNodeMap()}")
        sender.sendMessage("${messagePrefix}§bWorld groups: ${plugin.getMapAgent().getGroupMap()}")
        return true
    }
}
