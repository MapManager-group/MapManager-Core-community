package work.alsace.mapmanager

import net.luckperms.api.LuckPerms
import net.luckperms.api.node.types.InheritanceNode
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.Logger
import org.bukkit.Bukkit
import org.bukkit.command.CommandExecutor
import org.bukkit.command.TabExecutor
import org.bukkit.plugin.java.JavaPlugin
import org.mvplugins.multiverse.core.MultiverseCoreApi
import work.alsace.mapmanager.common.command.*
import work.alsace.mapmanager.common.function.VersionCheckImpl
import work.alsace.mapmanager.common.listener.BlockListener
import work.alsace.mapmanager.common.listener.PlayerListener
import work.alsace.mapmanager.common.log.Log4JFilter
import work.alsace.mapmanager.common.version.VersionBridge
import work.alsace.mapmanager.service.DynamicWorld
import work.alsace.mapmanager.service.MainYaml
import work.alsace.mapmanager.service.MapAgent
import work.alsace.mapmanager.service.VersionCheck
import java.io.File
import java.io.IOException
import java.util.*

class MapManagerImpl : JavaPlugin(), MapManager {
    private var instance: MapManager? = null
    private var dynamicWorld: DynamicWorld? = null
    private var mapAgent: MapAgent? = null
    private var luckPerms: LuckPerms? = null
    private var yaml: MainYaml? = null
    private var versionCheck: VersionCheck? = null
    var coreApi: MultiverseCoreApi? = null
    override fun onEnable() {
        server.consoleSender.sendMessage("[§6MapManager§7] §f启动中...")

        logger.info("正在加载配置...")
        try {
            loadConfig()
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

        logger.info("正在获取LuckPerms API...")
        (LogManager.getRootLogger() as Logger).addFilter(Log4JFilter())
        luckPerms = Bukkit.getServicesManager().getRegistration(LuckPerms::class.java)?.provider
//        multiverseCore = Bukkit.getServer().pluginManager.getPlugin("Multiverse-Core") as MultiverseCore?
        val provider = Bukkit.getServicesManager().getRegistration(MultiverseCoreApi::class.java)
        if (provider != null) {
            coreApi = provider.provider
        }
        if (coreApi == null) {
            logger.warning("加载MultiverseCore失败")
        }
        initPermission()

        VersionBridge().serverVersionChecks(this)

        logger.info("正在注册指令和事件...")
        registerCommand("import", ImportCommand(this))
        registerCommand("delete", DeleteCommand(this))
        registerCommand("mapadmin", MapAdminCommand(this))
        registerCommand("write", WriteCommand(this))
        registerCommand("worldtp", WorldTPCommand(this))
        registerCommand("create", CreateCommand(this))
        registerCommand("world", WorldCommand(this))
        setVersionCheck(VersionCheckImpl(this))
        server.pluginManager.registerEvents(BlockListener(this), this)
        server.pluginManager.registerEvents(PlayerListener(this), this)
        server.consoleSender.sendMessage("[§6MapManager§7] §f加载成功！")
        instance = this
    }

    override fun onDisable() {
        mapAgent?.save()
        server.consoleSender.sendMessage("[§6MapManager§7] §f已卸载")
    }

    fun registerCommand(cmd: String, executor: CommandExecutor) {
        cmd.let { getCommand(it)?.setExecutor(executor) }
    }

    fun registerCommand(cmd: String, executor: TabExecutor) {
        Objects.requireNonNull(getCommand(cmd))?.setExecutor(executor)
        Objects.requireNonNull(getCommand(cmd))?.tabCompleter = executor
    }

    override fun getDynamicWorld(): DynamicWorld {
        return dynamicWorld!!
    }

    override fun setDynamicWorld(dynamicWorld: DynamicWorld) {
        this.dynamicWorld = dynamicWorld
    }

    override fun getMapAgent(): MapAgent {
        return (mapAgent as MapAgent)
    }

    override fun setMapAgent(mapAgent: MapAgent) {
        this.mapAgent = mapAgent
    }

    override fun getLuckPerms(): LuckPerms {
        return luckPerms!!
    }

    override fun getMainYaml(): MainYaml {
        return yaml!!
    }

    override fun setMainYaml(yaml: MainYaml) {
        this.yaml = yaml
    }

    override fun getVersionCheck(): VersionCheck {
        return versionCheck!!
    }

    override fun setVersionCheck(versionCheck: VersionCheck) {
        this.versionCheck = versionCheck
    }

    @Throws(IOException::class)
    private fun loadConfig() {
        val configFile = File(dataFolder, "config.yml")
        if (!configFile.exists()) {
            saveResource("config.yml", false)
        }
        val worldsFile = File(dataFolder, "worlds.json")
        val groupsFile = File(dataFolder, "groups.json")
        if (!worldsFile.exists()) {
            if (!worldsFile.createNewFile()) {
                logger.warning("无法创建 worlds.json 文件")
            }
        }
        if (!groupsFile.exists()) {
            if (!groupsFile.createNewFile()) {
                logger.warning("无法创建 groups.json 文件")
            }
        }
        logger.info("运行所需文件创建完成")
    }

    private fun initPermission() {
        val manager = luckPerms?.groupManager
        if (manager == null) {
            logger.warning("加载LuckPerms出现问题，请联系开发者")
            return
        }
        val groups = listOf("apply", "public", "worldbase")
        groups.forEach { groupName ->
            manager.loadGroup(groupName).thenAcceptAsync { optionalGroup ->
                if (!optionalGroup.isPresent) {
                    manager.createAndLoadGroup(groupName).thenAccept { newGroup ->
                        newGroup?.let {
                            val data = it.data()
                            data.add(InheritanceNode.builder("default").build())
                            manager.saveGroup(it).thenRun {
                                logger.info("权限组 ${it.name} 已创建并初始化完毕")
                            }
                        } ?: logger.warning("无法创建权限组 $groupName")
                    }
                } else {
                    logger.info("权限组 $groupName 已存在，无需创建")
                }
            }.exceptionally { throwable ->
                logger.warning("加载权限组 $groupName 时出错: ${throwable.message}")
                null
            }
        }
    }

    override fun getInstance(): MapManager? {
        return instance
    }

    fun getMVApi(): MultiverseCoreApi? {
        return coreApi
    }
}
