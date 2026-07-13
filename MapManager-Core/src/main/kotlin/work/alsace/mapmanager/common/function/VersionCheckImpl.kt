package work.alsace.mapmanager.common.function

import net.querz.nbt.io.NBTUtil
import net.querz.nbt.io.NamedTag
import net.querz.nbt.tag.CompoundTag
import org.mvplugins.multiverse.core.world.helpers.WorldNameChecker
import work.alsace.mapmanager.MapManagerImpl
import work.alsace.mapmanager.service.VersionCheck
import java.io.File
import java.io.IOException

class VersionCheckImpl(private val plugin: MapManagerImpl) : VersionCheck {
    val checker = WorldNameChecker()
    /**
     * 检测主目录下地图文件版本是否合法
     * @param world String 地图名
     * @return true为合法，false不合法
     */
    override fun isMapVersionCorrect(world: String): Boolean {
        val worldDir = File(plugin.server.worldContainer, world)
        return isMapVersionCorrect(worldDir)
    }

    /**
     * 检测地图文件版本是否合法
     * @param dir File 地图文件夹
     * @return true为合法，false不合法
     */
    override fun isMapVersionCorrect(dir: File): Boolean {
        if (!File(dir, "level.dat").exists()) {
            plugin.logger.info("${dir}不是有效的地图文件，缺少level.dat")
            return false
        }
        if (!checker.isValidWorldFolder(dir)) {
            plugin.logger.info("${dir}不是有效的地图目录")
            return false
        }
        if (!checker.isValidWorldName(dir.name)) {
            plugin.logger.info("${dir}不是有效的地图名称")
            return false
        }
        if (!isLevelFileVersionCorrect(dir)) {
            plugin.logger.info("${dir}地图版本不符合导入规范")
            return false
        }
        return true
    }

    /**
     * 检测地图文件版本是否合法
     * @param file File 地图文件夹
     * @return true为合法，false不合法
     */
    private fun isLevelFileVersionCorrect(file: File): Boolean {
        try {
            val levelDatFile = File("$file/level.dat")
            val namedTag: NamedTag = NBTUtil.read(levelDatFile)
            val levelDatTag: CompoundTag = namedTag.tag as CompoundTag
            return if (levelDatTag.containsKey("Data") && levelDatTag.get("Data") is CompoundTag) {
                val dataTag: CompoundTag = levelDatTag.getCompoundTag("Data")
                if (dataTag.containsKey("version") && dataTag.get("version") != null) {
                    val version: Int = dataTag.getInt("DataVersion")
                    plugin.logger.info("level.dat version: $version")
                    version <= serverVersion
                } else {
                    plugin.logger.info("Version information not found in level.dat")
                    false
                }
            } else {
                plugin.logger.info("Invalid level.dat format")
                false
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return false
    }

    /**
     * 获取服务器版本
     * @return 服务器版本
     */
    @Suppress("DEPRECATION")
    override val serverVersion: Int
        get() = plugin.server.unsafe.dataVersion
}
