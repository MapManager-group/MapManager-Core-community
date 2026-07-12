package work.alsace.mapmanager.common.command

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor
import org.bukkit.entity.Player
import work.alsace.mapmanager.MapManagerImpl
import work.alsace.mapmanager.service.DynamicWorld
import work.alsace.mapmanager.service.MapAgent

class DeleteCommand(plugin: MapManagerImpl) : TabExecutor {
    private val lastTime: MutableMap<String?, DeletionNode?> = HashMap()
    private val world: DynamicWorld
    private val map: MapAgent
    private val emptyList: MutableList<String?> = ArrayList(0)
    private val nullNode: DeletionNode = DeletionNode(-1, "§~.nullWorld")

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
            sender.sendMessage("§c你没有权限使用此命令")
            return true
        }
        val name = sender.name
        if (args.isEmpty()) {
            if (sender !is Player) {
                sender.sendMessage("§c仅限玩家执行")
                return false
            }
            sender.sendMessage("§e若要确认删除世界" + sender.world.name + "，请在10秒内输入/delete confirm")
            putNode(name, sender.world.name)
            return true
        }
        if (args[0] == "confirm") {
            val node = getNode(name)
            if (node != null) {
                if (node.time + 10000 < System.currentTimeMillis()) {
                    sender.sendMessage("§c已超过确认时间")
                    return false
                }
            }
            // Process Command
            lastTime.remove(name)
            sender.sendMessage("§e删除中...")
            try {
                if (node != null) {
                    if (node.world?.let { map.deleteWorld(it) } == true) sender.sendMessage("§a删除成功") else sender.sendMessage("§c删除失败，详细请查看控制台")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                sender.sendMessage("§c删除失败，详细请查看控制台")
            }
        } else {
            sender.sendMessage("§e若要确认删除世界" + args[0] + "，请在10秒内输入/delete confirm")
            putNode(name, args[0])
        }
        return true
    }

    class DeletionNode(var time: Long, var world: String?)

    private fun putNode(operator: String?, world: String?) {
        lastTime[operator] = DeletionNode(System.currentTimeMillis(), world)
    }

    private fun getNode(operator: String?): DeletionNode? {
        return lastTime.getOrDefault(operator, nullNode)
    }
}
