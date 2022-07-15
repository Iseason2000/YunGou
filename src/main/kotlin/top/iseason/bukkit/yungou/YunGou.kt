package top.iseason.bukkit.yungou

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction
import top.iseason.bukkit.bukkittemplate.KotlinPlugin
import top.iseason.bukkit.bukkittemplate.command.CommandBuilder
import top.iseason.bukkit.bukkittemplate.config.ConfigWatcher
import top.iseason.bukkit.bukkittemplate.config.SimpleYAMLConfig
import top.iseason.bukkit.bukkittemplate.debug.SimpleLogger
import top.iseason.bukkit.bukkittemplate.debug.info
import top.iseason.bukkit.bukkittemplate.utils.toColor

object YunGou : KotlinPlugin() {

    lateinit var mysql: Database

    override fun onAsyncLoad() {
        command1()
    }

    override fun onEnable() {
        //如果使用命令模块，取消注释
        CommandBuilder.onEnable()

        //如果使用UI模块,取消注释
//        UIListener.onEnable()

        SimpleLogger.prefix = "&a[&6${javaPlugin.description.name}&a]&r ".toColor()
        info("&a插件已启用!")
    }

    override fun onAsyncEnable() {
        SimpleYAMLConfig.notifyMessage = "&7配置文件 &6%s &7已重载!"
        Config

    }

    override fun onDisable() {

        //如果使用命令模块，取消注释
        CommandBuilder.onDisable()

        //如果使用UI模块,取消注释
//        UIListener.onDisable()

        //如果使用配置模块，取消注销
        ConfigWatcher.onDisable()

        info("&6插件已卸载!")
    }

}