package work.alsace.mapmanager.common.version

import work.alsace.mapmanager.MapManagerImpl
import work.alsace.mapmanager.common.function.DynamicWorldImpl
import work.alsace.mapmanager.common.function.MainYamlImpl
import work.alsace.mapmanager.common.function.MapAgentImpl

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
                plugin.setMainYaml(MainYamlImpl(plugin))
                plugin.setDynamicWorld(DynamicWorldImpl(plugin))
                plugin.setMapAgent(MapAgentImpl(plugin))
            }

            else -> {
                throw UnsupportedOperationException("Unsupported server version: $version")
            }
        }
    }
}
