package top.iseason.bukkit.yungou

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.TransactionManager
import top.iseason.bukkit.bukkittemplate.KotlinPlugin
import top.iseason.bukkit.bukkittemplate.command.CommandBuilder
import top.iseason.bukkit.bukkittemplate.config.ConfigWatcher
import top.iseason.bukkit.bukkittemplate.config.SimpleYAMLConfig
import top.iseason.bukkit.bukkittemplate.debug.SimpleLogger
import top.iseason.bukkit.bukkittemplate.debug.info
import top.iseason.bukkit.bukkittemplate.utils.toColor
import top.iseason.bukkit.yungou.command.mainCommand
import top.iseason.bukkit.yungou.data.Config
import top.iseason.bukkit.yungou.placeholders.PAPI

object YunGou : KotlinPlugin() {

    lateinit var mysql: Database
    override fun onAsyncLoad() {
        mainCommand()
    }

    override fun onEnable() {
        //如果使用命令模块，取消注释
        CommandBuilder.onEnable()
        PAPI.register()
        SimpleLogger.prefix = "&a[&6${javaPlugin.description.name}&a]&r ".toColor()
        info("&a插件已启用!")
    }

    override fun onAsyncEnable() {
        SimpleYAMLConfig.notifyMessage = "&7配置文件 &6%s &7已重载!"
        registerListeners(PlayerListener)
        Config
        try {
            javaPlugin.saveResource("placeholder.yml", true)
        } catch (_: Exception) {
        }
    }

    override fun onDisable() {
        Config.closeDB()
        //如果使用命令模块，取消注释
        CommandBuilder.onDisable()
        //如果使用配置模块，取消注销
        ConfigWatcher.onDisable()
        TransactionManager.closeAndUnregister(mysql)
        PAPI.unregister()
        info("&6插件已卸载!")
    }

}