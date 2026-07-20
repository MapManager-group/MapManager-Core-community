package work.alsace.mapmanager.common.version

import work.alsace.mapmanager.MapManagerImpl
import work.alsace.mapmanager.common.function.DynamicWorldImpl
import work.alsace.mapmanager.common.function.MapAgentImpl
import work.alsace.mapmanager.function.MainYamlV120

class VersionBridge {
    /**
     * 检测服务器版本并加载对应的类
     * @param plugin 插件主类
     */
    @Suppress("DEPRECATION")
    fun serverVersionChecks(plugin: MapManagerImpl) {
        val version = plugin.server.unsafe.dataVersion
        plugin.logger.info("Server version: $version")
        when {
            version >= 3465 -> {
                plugin.setMainYaml(MainYamlV120(plugin))
                plugin.setDynamicWorld(DynamicWorldImpl(plugin))
                plugin.setMapAgent(MapAgentImpl(plugin))
            }

            else -> {
                throw UnsupportedOperationException("Unsupported server version: $version")
            }
        }
    }
}
