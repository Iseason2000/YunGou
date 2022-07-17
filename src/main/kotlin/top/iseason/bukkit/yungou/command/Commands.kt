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
import top.iseason.bukkit.bukkittemplate.utils.broadcast
import top.iseason.bukkit.bukkittemplate.utils.bukkit.giveItems
import top.iseason.bukkit.bukkittemplate.utils.sendColorMessage
import top.iseason.bukkit.yungou.ItemUtil
import top.iseason.bukkit.yungou.ItemUtil.toByteArray
import top.iseason.bukkit.yungou.data.*
import top.iseason.bukkit.yungou.formatBy
import java.time.LocalDateTime

fun mainCommand() {
    var suggest: Collection<String>? = null
    var lastUpdate = 0L
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
            "debug", default = PermissionDefault.OP, description = "切换调试模式"
        ) {
            onExecute {
                SimpleLogger.isDebug = !SimpleLogger.isDebug
                if (SimpleLogger.isDebug) {
                    it.sendColorMessage("${SimpleLogger.prefix}${Lang.command__debug_on}")
                } else {
                    it.sendColorMessage("${SimpleLogger.prefix}${Lang.command__debug_off}")
                }
                true
            }
        }
        node(
            "reConnect", default = PermissionDefault.OP, description = "重新链接数据库"
        ) {
            onExecute {
                Config.reConnectedDB()
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
                Param("<份数>", suggest = listOf("20", "50", "100")),
                Param("<冷却时间>", suggest = listOf("3", "5", "10", "60"))
            )
        ) {
            onExecute {
                val player = it as Player
                val item = player.equipment.itemInMainHand
                if (item == null || item.type == Material.AIR) throw ParmaException(Lang.command__add_no_item)
                val name = getParam<String>(0)
                val count = getOptionalParam<Int>(1) ?: 1
                val coolDown = getOptionalParam<Int>(2) ?: 1
                if (Cargos.has(name)) throw ParmaException(Lang.command__add_id_exist)
                transaction {
                    Cargo.new(name) {
                        this.item = ExposedBlob(item.toByteArray())
                        startTime = LocalDateTime.now()
                        serial = 0
                        num = count
                        this.coolDown = coolDown
                    }
                }
                onSuccess(Lang.command__add_id_success.formatBy(name, count, coolDown))
                true
            }

        }
        node(
            "get",
            alias = arrayOf(""),
            default = PermissionDefault.OP,
            description = "获取一个云购商品",
            async = true,
            isPlayerOnly = true,
            params = arrayOf(
                Param("[id]", suggestRuntime = {
                    val currentTimeMillis = System.currentTimeMillis()
                    if (System.currentTimeMillis() - lastUpdate < 2000L) return@Param suggest ?: emptyList()
                    lastUpdate = currentTimeMillis
                    suggest = getCargosNames()
                    suggest!!
                })
            )
        ) {
            onExecute {
                val player = it as Player
                val id = getParam<String>(0)
                var itemBlob: ExposedBlob? = null
                transaction {
                    itemBlob = Cargos.slice(Cargos.item).select { Cargos.id eq id }.firstOrNull()?.get(Cargos.item)
                }
                if (itemBlob == null) {
                    player.sendColorMessage("${SimpleLogger.prefix}${Lang.command__get_failure.formatBy(id)}")
                } else {
                    player.giveItems(ItemUtil.fromByteArray(itemBlob!!.bytes))
                    player.sendColorMessage("${SimpleLogger.prefix}${Lang.command__get_success.formatBy(id)}")
                }
                true
            }
            onSuccess(Lang.command__remove_success)
            onFailure(Lang.command__remove_failure)
        }
        node(
            "remove",
            alias = arrayOf(""),
            default = PermissionDefault.OP,
            description = "删除一个云购商品",
            async = true,
            params = arrayOf(
                Param("[id]", suggestRuntime = {
                    val currentTimeMillis = System.currentTimeMillis()
                    if (System.currentTimeMillis() - lastUpdate < 2000L) return@Param suggest ?: emptyList()
                    lastUpdate = currentTimeMillis
                    suggest = getCargosNames()
                    suggest!!
                })
            )
        ) {
            onExecute {
                val id = getParam<String>(0)
                var count = 0
                transaction {
                    count = Cargos.deleteWhere { Cargos.id eq id }
                }
                debug("&6删除了商品 &c$id")
                count != 0
            }
            onSuccess(Lang.command__remove_success)
            onFailure(Lang.command__remove_failure)
        }
        node(
            "buy",
            alias = arrayOf(""),
            default = PermissionDefault.OP,
            description = "为玩家购买 <份数> 个云购商品",
            async = true,
            params = arrayOf(
                Param("[玩家]", suggestRuntime = ParamSuggestCache.playerParam),
                Param("[id]", suggestRuntime = {
                    try {
                        if (suggest == null) suggest = getCargosNames()
                        return@Param suggest!!
                    } catch (e: Exception) {
                        return@Param emptyList()
                    }
                }),
                Param("<份数>", suggest = listOf("1", "5", "10"))
            )
        ) {
            onExecute {
                val player = getParam<Player>(0)
                val id = getParam<String>(1)
                val count = getOptionalParam<Int>(2) ?: 1
                transaction {
//                    addLogger(StdOutSqlLogger)
                    val cargo = Cargo.findById(id) ?: throw ParmaException(Lang.command__buy_id_unexist)
                    val existNum = Records.slice(Records.num.sum()).select { Records.cargo eq id }.firstOrNull()
                        ?.get(Records.num.sum()) ?: 0
                    val after = existNum + count
                    if (after > cargo.num) throw ParmaException(
                        Lang.command__buy_can_not_buy.formatBy(
                            cargo.num - existNum,
                            count
                        )
                    )
                    else if (after == cargo.num) {
                        //开奖
                        broadcast("${SimpleLogger.prefix}${Lang.command__buy_start.formatBy(id, Config.countdown)}")
                        //todo
                    }
                    try {
                        Record.new {
                            uid = player.uniqueId
                            this.cargo = cargo
                            num = count
                            serial = cargo.serial
                            time = LocalDateTime.now()
                        }
                    } catch (e: Exception) {
                        player.sendColorMessage("${SimpleLogger.prefix}${Lang.command__buy_error}")
                        return@transaction false
                    }
                    player.sendColorMessage("${SimpleLogger.prefix}${Lang.command__buy_success.formatBy(id, count)}")
                    debug("&a已为 &6${player.name} &a购买 &6$id &aX &6$count")
                }
                true
            }
        }
    }

}