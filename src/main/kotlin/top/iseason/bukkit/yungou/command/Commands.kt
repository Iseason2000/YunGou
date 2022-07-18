package top.iseason.bukkit.yungou.command

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.permissions.PermissionDefault
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.jetbrains.exposed.sql.transactions.transaction
import top.iseason.bukkit.bukkittemplate.command.Param
import top.iseason.bukkit.bukkittemplate.command.ParamSuggestCache
import top.iseason.bukkit.bukkittemplate.command.ParmaException
import top.iseason.bukkit.bukkittemplate.command.commandRoot
import top.iseason.bukkit.bukkittemplate.debug.SimpleLogger
import top.iseason.bukkit.bukkittemplate.debug.debug
import top.iseason.bukkit.bukkittemplate.utils.EasyCoolDown
import top.iseason.bukkit.bukkittemplate.utils.broadcast
import top.iseason.bukkit.bukkittemplate.utils.sendColorMessage
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
                if (!Config.isConnected) return@onExecute true
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
            "show",
            alias = arrayOf(""),
            description = "展示一个云购商品的信息",
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
                if (!Config.isConnected) return@onExecute true
                val id = getParam<String>(0)
                var cargo: Cargo? = null
                transaction {
                    cargo = Cargo.findById(id) ?: throw ParmaException(Lang.command__show_id_not_exist)
                }
                it.sendColorMessage("${SimpleLogger.prefix}${Lang.command__show_id.formatBy(cargo!!.id)}")
                it.sendColorMessage("${SimpleLogger.prefix}${Lang.command__show_num.formatBy(cargo!!.num)}")
                it.sendColorMessage("${SimpleLogger.prefix}${Lang.command__show_enable.formatBy(cargo!!.enable)}")
                it.sendColorMessage("${SimpleLogger.prefix}${Lang.command__show_time.formatBy(cargo!!.startTime)}")
                it.sendColorMessage("${SimpleLogger.prefix}${Lang.command__show_serial.formatBy(cargo!!.serial)}")
                it.sendColorMessage("${SimpleLogger.prefix}${Lang.command__show_lastTime.formatBy(cargo!!.lastTime.toString())}")
                it.sendColorMessage("${SimpleLogger.prefix}${Lang.command__show_cooldown.formatBy(cargo!!.coolDown)}")
                true
            }
        }
        node(
            "get",
            alias = arrayOf(""),
            description = "领取一个云购商品",
            async = true,
            isPlayerOnly = true,
            params = arrayOf(
                Param("[id]", suggestRuntime = {
                    val player = (this as? Player) ?: return@Param emptyList()
                    if (EasyCoolDown.check(player, 2000L)) {
                        return@Param emptyList()
                    }
                    var result: List<String>? = null
                    transaction {
                        result = Lotteries.slice(Lotteries.cargo)
                            .select { Lotteries.uid eq player.uniqueId and (Lotteries.hasReceive eq false) }.map {
                                it[Lotteries.cargo].value
                            }
                    }
                    result ?: emptyList()
                })
            )
        ) {
            onExecute {
                if (!Config.isConnected) return@onExecute true
                val player = it as Player
                val id = getParam<String>(0)
                transaction {
//                    addLogger(StdOutSqlLogger)
                    val firstOrNull = Lottery.find(
                        Lotteries.uid eq player.uniqueId and (Lotteries.cargo eq id) and (Lotteries.hasReceive eq false)
                    ).limit(1).firstOrNull() ?: throw ParmaException(Lang.command__get_failure.formatBy(id))
//                    onSuccess(Lang.command__get_success.formatBy(id))
                    firstOrNull.offeringPrizes()
                }
                true
            }
        }

        node(
            "list",
            alias = arrayOf(""),
            description = "列出中奖的商品",
            async = true,
            isPlayerOnly = true
        ) {
            onExecute { sender ->
                if (!Config.isConnected) return@onExecute true
                val player = sender as Player
                player.sendColorMessage("${SimpleLogger.prefix}${Lang.command__list_head}")
                transaction {
                    Lotteries.slice(Lotteries.cargo, Lotteries.serial)
                        .select { Lotteries.uid eq player.uniqueId and (Lotteries.hasReceive eq false) }.forEach {
                            player.sendColorMessage(
                                "${SimpleLogger.prefix}${
                                    Lang.command__list_body.formatBy(
                                        it[Lotteries.cargo].value,
                                        it[Lotteries.serial]
                                    )
                                }"
                            )
                        }
                }
                true
            }
        }
        node(
            "remove",
            alias = arrayOf(""),
            default = PermissionDefault.OP,
            description = "删除一个云购商品,同时删除所有有关记录",
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
                if (!Config.isConnected) return@onExecute true
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
                if (!Config.isConnected) return@onExecute true
                val player = getParam<Player>(0)
                val id = getParam<String>(1)
                val count = getOptionalParam<Int>(2) ?: 1
                if (count <= 0) throw ParmaException("&c请输入大于0的份数")
                transaction {
//                    addLogger(StdOutSqlLogger)
                    val cargo = Cargo.findById(id) ?: throw ParmaException(Lang.command__buy_id_unexist)
                    if (!cargo.enable) throw ParmaException(Lang.command__buy_not_enable.formatBy(id))
                    if (cargo.isCoolDown()) throw ParmaException(Lang.command__buy_is_cooldown.formatBy(id))
                    val existNum = Records.slice(Records.num.sum())
                        .select { Records.cargo eq id and (Records.serial eq cargo.serial) }.firstOrNull()
                        ?.get(Records.num.sum()) ?: 0
                    val after = existNum + count
                    if (after > cargo.num) throw ParmaException(
                        Lang.command__buy_can_not_buy.formatBy(
                            cargo.num - existNum,
                            count
                        )
                    )
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
                    if (after == cargo.num) {
                        //开奖
                        broadcast("${SimpleLogger.prefix}${Lang.command__buy_start.formatBy(id, Config.countdown)}")
                        Lotteries.drawLottery(id)
                        debug("&6$id &a已开奖")
                    }
                    Unit
                }
                true
            }
        }
        node(
            "force",
            alias = arrayOf(""),
            default = PermissionDefault.OP,
            description = "强制开启倒计时",
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
                if (!Config.isConnected) return@onExecute true
                val id = getParam<String>(0)
                if (Lotteries.drawLottery(id) == null) {
                    it.sendColorMessage("${SimpleLogger.prefix}&c没有人购买这一期的商品")
                    return@onExecute true
                }
                broadcast("${SimpleLogger.prefix}${Lang.command__buy_start.formatBy(id, Config.countdown)}")
                it.sendColorMessage("${SimpleLogger.prefix}&a强制开启成功")
                debug("&6强制开启了商品 &c$id")
                true
            }
        }
        node(
            "toggle",
            alias = arrayOf(""),
            default = PermissionDefault.OP,
            description = "切换商品开启状态",
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
                transaction {
                    val cargo = Cargo.findById(id) ?: throw ParmaException(Lang.command__buy_id_unexist)
                    cargo.enable = !cargo.enable
                    it.sendColorMessage("${SimpleLogger.prefix}${Lang.command__toogle.formatBy(cargo.enable)}")
                }
                true
            }
        }
    }
}