package top.iseason.bukkit.yungou

import top.iseason.bukkit.yungou.command.mainCommand
import top.iseason.bukkit.yungou.data.*
import top.iseason.bukkit.yungou.placeholders.PAPI
import top.iseason.bukkittemplate.BukkitPlugin
import top.iseason.bukkittemplate.command.CommandHandler
import top.iseason.bukkittemplate.config.DatabaseConfig
import top.iseason.bukkittemplate.config.SimpleYAMLConfig
import top.iseason.bukkittemplate.debug.SimpleLogger
import top.iseason.bukkittemplate.debug.info
import top.iseason.bukkittemplate.hook.PlaceHolderHook
import top.iseason.bukkittemplate.utils.bukkit.EventUtils.register
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.toColor

object YunGou : BukkitPlugin {

    override fun onEnable() {
        //如果使用命令模块，取消注释
        mainCommand()
        CommandHandler.updateCommands()
        PlaceHolderHook.checkHooked()
        if (PlaceHolderHook.hasHooked)
            PAPI.register()
        Config.load(false)
        Lang.load(false)
        DatabaseConfig.load(false)
        DatabaseConfig.initTables(Cargos, Lotteries, Records)
        PlayerListener.register()
        SimpleLogger.prefix = "&a[&6${javaPlugin.description.name}&a]&r ".toColor()
        val trimIndent = """
            　
             __     __           _____             
             \ \   / /          / ____|            
              \ \_/ /   _ _ __ | |  __  ___  _   _ 
               \   / | | | '_ \| | |_ |/ _ \| | | |
                | || |_| | | | | |__| | (_) | |_| |
                |_| \__,_|_| |_|\_____|\___/ \__,_|
            　                                                           
              作者：Iseason       QQ: 1347811744
            
        """.trimIndent()
        info(trimIndent)
        info("&a插件已启用!!")
    }

    override fun onAsyncEnable() {
        SimpleYAMLConfig.notifyMessage = "&7配置文件 &6%s &7已重载!"
        try {
            javaPlugin.saveResource("placeholder.yml", true)
        } catch (_: Exception) {
        }
    }

    override fun onDisable() {
        runCatching {
            if (PlaceHolderHook.hasHooked)
                PAPI.unregister()
        }
        info("&6插件已卸载!")
    }

}
