package work.alsace.mapmanager.common.command

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player
import work.alsace.mapmanager.MapManagerImpl
import work.alsace.mapmanager.service.DynamicWorld
import work.alsace.mapmanager.service.MapAgent

class DeleteCommand(plugin: MapManagerImpl) : TabExecutor {
    private val messagePrefix = "§6[MapManager] §r"
    private val lastTime: MutableMap<String?, DeletionNode?> = HashMap()
    private val world: DynamicWorld
    private val map: MapAgent
    private val emptyList: MutableList<String?> = ArrayList(0)

    init {
        world = plugin.getDynamicWorld()
        map = plugin.getMapAgent()
    }

    override fun onTabComplete(
        sender: CommandSender,
        cmd: Command,
        label: String,
        args: Array<String>
    ): MutableList<out String?> {
        return if (sender.hasPermission("mapmanager.command.delete") && args.size == 1) world.getWorlds(args[0]) else emptyList
    }

    override fun onCommand(sender: CommandSender, cmd: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("mapmanager.command.delete")) {
            sender.sendMessage("${messagePrefix}§c权限不足，无法执行此命令。")
            return true
        }
        val name = sender.name
        if (args.isEmpty()) {
            if (sender !is Player) {
                sender.sendMessage("${messagePrefix}§c此操作仅限玩家执行。")
                return true
            }
            sender.sendMessage("${messagePrefix}§e将在 10 秒内删除地图 ${sender.world.name}；请输入 /delete confirm 确认。")
            putNode(name, sender.world.name)
            return true
        }
        if (args[0] == "confirm") {
            val node = getNode(name)
            if (node != null) {
                if (node.time + 10000 < System.currentTimeMillis()) {
                    sender.sendMessage("${messagePrefix}§c确认已超时，请重新执行删除命令。")
                    return true
                }
            }
            if (node == null) {
                sender.sendMessage("${messagePrefix}§c没有待确认的删除操作。")
                return true
            }
            // Process Command
            lastTime.remove(name)
            sender.sendMessage("${messagePrefix}§e正在删除地图，请稍候…")
            try {
                if (node.world?.let { map.deleteWorld(it) } == true) sender.sendMessage("${messagePrefix}§a地图删除完成。") else sender.sendMessage("${messagePrefix}§c地图删除失败，请查看控制台日志。")
            } catch (e: Exception) {
                e.printStackTrace()
                sender.sendMessage("${messagePrefix}§c地图删除失败，请查看控制台日志。")
            }
        } else {
            sender.sendMessage("${messagePrefix}§e将在 10 秒内删除地图 ${args[0]}；请输入 /delete confirm 确认。")
            putNode(name, args[0])
        }
        return true
    }

    class DeletionNode(var time: Long, var world: String?)

    private fun putNode(operator: String?, world: String?) {
        lastTime[operator] = DeletionNode(System.currentTimeMillis(), world)
    }

    private fun getNode(operator: String?): DeletionNode? {
        return lastTime[operator]
    }
}
