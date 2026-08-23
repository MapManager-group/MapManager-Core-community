package work.alsace.mapmanager.common.function

import org.bukkit.plugin.Plugin
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.TypeDescription
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor
import org.yaml.snakeyaml.nodes.Tag
import org.yaml.snakeyaml.representer.Representer
import work.alsace.mapmanager.pojo.MainConfig
import work.alsace.mapmanager.service.MainYaml
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException

class MainYamlImpl(plugin: Plugin) : MainYaml {
    private val yaml: Yaml?
    private val file: File?

    init {
        val options = DumperOptions()
        options.defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
        options.isPrettyFlow = true
        val constructor: Constructor =
            CustomClassLoaderConstructor(
                MainYaml::class.java.classLoader,
                LoaderOptions())
        val representer = Representer(options)
        val description = TypeDescription(MainConfig::class.java, Tag("!MapManagerConfig"))
        representer.addTypeDescription(description)
        constructor.addTypeDescription(description)
        yaml = Yaml(constructor, representer, options)
        file = File(plugin.dataFolder, "config.yml")
    }

    override fun load(): MainConfig {
        if (!file?.exists()!!) {
            try {
                file.createNewFile()
            } catch (e: IOException) {
                e.printStackTrace()
                return MainConfig()
            }
            val tmp = MainConfig()
            save(tmp)
            return tmp
        }
        try {
            FileReader(file).use { reader -> return yaml?.loadAs(reader, MainConfig::class.java)!! }
        } catch (e: IOException) {
            e.printStackTrace()
            return MainConfig()
        }
    }

    override fun save(config: MainConfig) {
        try {
            file?.let { FileWriter(it).use { writer -> yaml?.dump(config, writer) } }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
