package top.iseason.bukkit.yungou

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.permissions.PermissionDefault
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.jetbrains.exposed.sql.transactions.transaction
import top.iseason.bukkit.bukkittemplate.command.Param
import top.iseason.bukkit.bukkittemplate.command.ParmaException
import top.iseason.bukkit.bukkittemplate.command.commandRoot
import top.iseason.bukkit.bukkittemplate.debug.SimpleLogger
import top.iseason.bukkit.yungou.ItemUtil.toByteArray
import top.iseason.bukkit.yungou.data.Cargo
import top.iseason.bukkit.yungou.data.Config
import java.time.LocalDateTime

fun mainCommand() {
    commandRoot(
        "yungou",
        alias = arrayOf("yg"),
        default = PermissionDefault.OP,
        description = "云购命令节点"
    ) {
        node(
            "reConnect", alias = arrayOf("重连"), default = PermissionDefault.OP, description = "重新链接数据库"
        ) {
            onExecute {
                Config.reConnected()
                true
            }
            onSuccess("&a配置已重载")
        }
        node(
            "add",
            alias = arrayOf(""),
            default = PermissionDefault.OP,
            description = "添加一个云购商品",
            async = true,
            isPlayerOnly = true,
            params = arrayOf(
                Param("[id]"),
                Param("<count>", suggest = listOf("20", "50", "100")),
                Param("<coolDown>", suggest = listOf("300", "600", "1200", "3600"))
            )
        ) {
            onExecute {
                val player = it as Player
                val item = player.equipment.itemInMainHand
                if (item == null || item.type == Material.AIR) throw ParmaException("${SimpleLogger.prefix}&c请拿着需要上架的物品")
                val name = getParam<String>(0)
                val count = getOptionalParam<Int>(1) ?: 1
                val coolDown = getOptionalParam<Int>(1) ?: 1
                transaction {
                    if (Cargo.findById(name) != null) throw ParmaException("${SimpleLogger.prefix}&cid已存在")
                    Cargo.new(name) {
                        this.item = ExposedBlob(item.toByteArray())
                        startTime = LocalDateTime.now()
                        this.coolDown = coolDown
                        num = count
                    }
                }
                onSuccess("${SimpleLogger.prefix}&a商品 &6$name &aX &6$count &a份 创建成功! 冷却时间: &6$coolDown &a秒")
                true
            }

        }
    }
}