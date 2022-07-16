package top.iseason.bukkit.yungou

import org.bukkit.permissions.PermissionDefault
import org.jetbrains.exposed.sql.transactions.transaction
import top.iseason.bukkit.bukkittemplate.command.commandRoot
import top.iseason.bukkit.yungou.data.Cargo
import top.iseason.bukkit.yungou.data.Config
import top.iseason.bukkit.yungou.data.Record
import java.time.LocalDateTime
import java.util.*

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
            "test", alias = arrayOf(""), default = PermissionDefault.OP, description = "", async = true
        ) {
            onExecute {
                transaction {
                    val c = Cargo.findById("test") ?: Cargo.new("test") {
                        item = "afasf5s1d65fgv4sdf5"
                        num = 100
                        startTime = LocalDateTime.now()
                        endTime = LocalDateTime.now().plusDays(10)
                    }
                    Record.new {
                        uid = UUID.randomUUID()
                        cargo = c
                        num = 1
                        time = LocalDateTime.now()
                    }
                }
                true
            }
        }
    }
}