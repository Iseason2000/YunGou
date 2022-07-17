package top.iseason.bukkit.yungou.command

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.permissions.PermissionDefault
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.jetbrains.exposed.sql.sum
import org.jetbrains.exposed.sql.transactions.transaction
import top.iseason.bukkit.bukkittemplate.command.Param
import top.iseason.bukkit.bukkittemplate.command.ParamSuggestCache
import top.iseason.bukkit.bukkittemplate.command.ParmaException
import top.iseason.bukkit.bukkittemplate.command.commandRoot
import top.iseason.bukkit.bukkittemplate.debug.SimpleLogger
import top.iseason.bukkit.bukkittemplate.debug.debug
import top.iseason.bukkit.bukkittemplate.utils.sendColorMessage
import top.iseason.bukkit.yungou.ItemUtil.toByteArray
import top.iseason.bukkit.yungou.data.*
import java.time.LocalDateTime

fun mainCommand() {
    var suggest: Collection<String>? = null
    fun getCargosNames() = transaction {
        return@transaction Cargos.slice(Cargos.id).selectAll().map {
            it[Cargos.id].value
        }
    }
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
                if (Cargos.has(name)) throw ParmaException("${SimpleLogger.prefix}&cid已存在")
                transaction {
                    Cargo.new(name) {
                        this.item = ExposedBlob(item.toByteArray())
                        startTime = LocalDateTime.now()
                        this.coolDown = coolDown
                        num = count
                    }
                }
                suggest = getCargosNames()
                onSuccess("${SimpleLogger.prefix}&a商品 &6$name &aX &6$count &a份 创建成功! 冷却时间: &6$coolDown &a秒")
                true
            }

        }
        node(
            "remove",
            alias = arrayOf(""),
            default = PermissionDefault.OP,
            description = "删除一个云购商品",
            async = true,
            params = arrayOf(
                Param("[id]", suggestRuntime = {
                    try {
                        if (suggest == null) suggest = getCargosNames()
                        return@Param suggest!!
                    } catch (e: Exception) {
                        return@Param emptyList()
                    }
                })
            )
        ) {
            onExecute {
                val id = getParam<String>(0)
                var count = 0
                transaction {
                    count = Cargos.deleteWhere { Cargos.id eq id }
                }
                if (count != 0) {
                    suggest = getCargosNames()
                    true
                } else false
            }
            onSuccess("${SimpleLogger.prefix}&a商品已删除!")
            onFailure("${SimpleLogger.prefix}&c商品不存在!")
        }
        node(
            "buy",
            alias = arrayOf(""),
            default = PermissionDefault.OP,
            description = "为玩家购买 count 个云购商品",
            async = true,
            params = arrayOf(
                Param("[player]", suggestRuntime = ParamSuggestCache.playerParam),
                Param("[id]", suggestRuntime = {
                    try {
                        if (suggest == null) suggest = getCargosNames()
                        return@Param suggest!!
                    } catch (e: Exception) {
                        return@Param emptyList()
                    }
                }),
                Param("<count>", suggest = listOf("1", "5", "10"))
            )
        ) {
            onExecute {
                val player = getParam<Player>(0)
                val id = getParam<String>(1)
                val count = getOptionalParam<Int>(2) ?: 1
                transaction {
//                    addLogger(StdOutSqlLogger)
                    val cargo = Cargo.findById(id) ?: throw ParmaException("${SimpleLogger.prefix}&cid不存在")
                    val existNum = Records.slice(Records.num.sum()).select { Records.cargo eq id }.firstOrNull()
                        ?.get(Records.num.sum()) ?: 0
                    if (existNum + count > cargo.num) throw ParmaException("${SimpleLogger.prefix}&c商品剩余 &6${cargo.num - existNum} &c个,无法购买 &6$count &c个")
                    try {
                        Record.new {
                            uid = player.uniqueId
                            this.cargo = cargo
                            num = count
                            time = LocalDateTime.now()
                        }
                    } catch (e: Exception) {
                        player.sendColorMessage("&c购买异常，请联系管理员!")
                        return@transaction false
                    }
                    player.sendColorMessage("&a已购买 &6$id &aX &6$count")
                    debug("&a已为 &6${player.name} &a购买 &6$id &aX &6$count")
                }
                true
            }
        }
    }

}