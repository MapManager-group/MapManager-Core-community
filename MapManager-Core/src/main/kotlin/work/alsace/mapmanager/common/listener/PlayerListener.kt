package work.alsace.mapmanager.common.listener

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityTeleportEvent
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import work.alsace.mapmanager.MapManagerImpl

class PlayerListener(private val plugin: MapManagerImpl) : Listener {
    /**
     * 玩家进入游戏时，取消卸载世界任务。
     * @param event 玩家进入游戏事件。
     */
    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val worldName = event.player.world.name
        plugin.getDynamicWorld().cancelUnloadTask(worldName)
    }

    /**
     * 玩家切换世界时，检测是否有权限
     */
    @EventHandler
    fun onPlayerTeleport(event: EntityTeleportEvent) {
        val player = event.entity as? Player ?: return
        val world = event.to?.world ?: return

        if (!player.hasPermission("multiverse.access.${world.name}")) {
            event.isCancelled = true
            player.sendMessage("§c你没有权限进入此地图")
        }
    }
}
