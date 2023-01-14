package top.iseason.bukkit.yungou.command

import org.bukkit.entity.Player
import org.bukkit.permissions.PermissionDefault
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import top.iseason.bukkit.yungou.data.*
import top.iseason.bukkittemplate.command.*
import top.iseason.bukkittemplate.config.DatabaseConfig
import top.iseason.bukkittemplate.config.dbTransaction
import top.iseason.bukkittemplate.debug.SimpleLogger
import top.iseason.bukkittemplate.debug.debug
import top.iseason.bukkittemplate.utils.bukkit.EntityUtils.getHeldItem
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.checkAir
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.toByteArray
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.broadcast
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.formatBy
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessage
import top.iseason.bukkittemplate.utils.other.EasyCoolDown
import java.time.LocalDateTime

fun mainCommand() {
    val cargosSuggest = Param.RuntimeSuggestParams { Cargos.getKeys() }
    var suggest: Collection<String>? = null
    var lastUpdate = 0L
    fun getCargosNames() = dbTransaction {
        return@dbTransaction Cargos.slice(Cargos.id).selectAll().map {
            it[Cargos.id].value
        }
    }
    command(
        "yungou"
    ) {
        alias = arrayOf("yg")
        description = "云购命令节点"
        node(
            "debug"
        ) {
            default = PermissionDefault.OP
            description = "切换调试模式"
            executor {
                SimpleLogger.isDebug = !SimpleLogger.isDebug
                if (SimpleLogger.isDebug) {
                    it.sendColorMessage(Lang.command__debug_on)
                } else {
                    it.sendColorMessage(Lang.command__debug_off)
                }
            }
        }
        node(
            "reConnect"
        ) {
            default = PermissionDefault.OP
            description = "重新链接数据库"
            executor {
                DatabaseConfig.reConnected()
            }
        }
        node("add") {
            alias = arrayOf("")
            default = PermissionDefault.OP
            description = "添加一个云购商品"
            async = true
            isPlayerOnly = true
            params = listOf(
                Param("[id]"),
                Param("<份数>", suggest = listOf("20", "50", "100")),
                Param("<冷却时间>", suggest = listOf("3", "5", "10", "60"))
            )
            executor {
                if (!DatabaseConfig.isConnected) return@executor
                val player = it as Player
                val item = player.getHeldItem()
                if (item.checkAir()) throw ParmaException(Lang.command__add_no_item)
                val name = getParam<String>(0)
                val count = getOptionalParam<Int>(1) ?: 1
                val coolDown = getOptionalParam<Int>(2) ?: 1
                if (Cargos.has(name)) throw ParmaException(Lang.command__add_id_exist)
                dbTransaction {
                    if (SimpleLogger.isDebug) addLogger(StdOutSqlLogger)
                    Cargo.new(name) {
                        this.item = ExposedBlob(item!!.toByteArray())
                        startTime = LocalDateTime.now()
                        serial = 0
                        num = count
                        this.coolDown = coolDown
                    }
                }
                it.sendColorMessage(Lang.command__add_id_success.formatBy(name, count, coolDown))
            }

        }
        node("show") {
            alias = arrayOf("")
            description = "展示一个云购商品的信息"
            async = true
            params = listOf(
                Param("[id]", suggestRuntime = cargosSuggest)
            )
            executor {
                if (!DatabaseConfig.isConnected) return@executor
                val id = getParam<String>(0)
                var cargo: Cargo? = null
                dbTransaction {
                    if (SimpleLogger.isDebug) addLogger(StdOutSqlLogger)
                    cargo = Cargo.findById(id) ?: throw ParmaException(Lang.command__show_id_not_exist)
                }
                it.sendColorMessage(Lang.command__show_id.formatBy(cargo!!.id))
                it.sendColorMessage(Lang.command__show_num.formatBy(cargo!!.num))
                it.sendColorMessage(Lang.command__show_enable.formatBy(cargo!!.enable))
                it.sendColorMessage(Lang.command__show_time.formatBy(cargo!!.startTime))
                it.sendColorMessage(Lang.command__show_serial.formatBy(cargo!!.serial))
                it.sendColorMessage(Lang.command__show_lastTime.formatBy(cargo!!.lastTime.toString()))
                it.sendColorMessage(Lang.command__show_cooldown.formatBy(cargo!!.coolDown))
            }
        }
        node(
            "get"
        ) {
            description = "领取一个云购商品"
            async = true
            isPlayerOnly = true
            params = listOf(
                Param("[id]", suggestRuntime = {
                    val player = (this as? Player) ?: return@Param emptyList()
                    if (EasyCoolDown.check(player, 2000L)) {
                        return@Param emptyList()
                    }
                    var result: List<String>? = null
                    dbTransaction {
                        if (SimpleLogger.isDebug) addLogger(StdOutSqlLogger)
                        result = Lotteries.slice(Lotteries.cargo)
                            .select { Lotteries.uid eq player.uniqueId and (Lotteries.hasReceive eq false) }.map {
                                it[Lotteries.cargo].value
                            }
                    }
                    result ?: emptyList()
                })
            )
            executor {
                if (!DatabaseConfig.isConnected) return@executor
                val player = it as Player
                val id = getParam<String>(0)
                dbTransaction {
                    val firstOrNull = Lottery.find(
                        Lotteries.uid eq player.uniqueId and (Lotteries.cargo eq id) and (Lotteries.hasReceive eq false)
                    ).limit(1).firstOrNull() ?: throw ParmaException(Lang.command__get_failure.formatBy(id))
//                    onSuccess(Lang.command__get_success.formatBy(id))
                    firstOrNull.offeringPrizes()
                }
            }
        }
        node(
            "list"
        ) {
            description = "列出中奖的商品"
            async = true
            isPlayerOnly = true
            executor {
                if (!DatabaseConfig.isConnected) return@executor
                val player = it as Player
                player.sendColorMessage(Lang.command__list_head)
                dbTransaction {
                    if (SimpleLogger.isDebug) addLogger(StdOutSqlLogger)
                    Lotteries.slice(Lotteries.cargo, Lotteries.serial)
                        .select { Lotteries.uid eq player.uniqueId and (Lotteries.hasReceive eq false) }.forEach {
                            player.sendColorMessage(
                                Lang.command__list_body.formatBy(
                                    it[Lotteries.cargo].value,
                                    it[Lotteries.serial]
                                )
                            )
                        }
                }
            }
        }
        node(
            "remove"
        ) {
            default = PermissionDefault.OP
            description = "删除一个云购商品,同时删除所有有关记录"
            async = true
            params = listOf(
                Param("[id]", suggestRuntime = cargosSuggest)
            )
            executor {
                if (!DatabaseConfig.isConnected) return@executor
                val id = getParam<String>(0)
                val success = dbTransaction {
                    if (SimpleLogger.isDebug) addLogger(StdOutSqlLogger)
//                    exec("SET foreign_key_checks = 0;")
                    Records.deleteWhere { Records.cargo eq id }
                    Lotteries.deleteWhere { Lotteries.cargo eq id }
                    Cargos.deleteWhere { Cargos.id eq id } != 0
//                    exec("SET foreign_key_checks = 1;")
                }
                debug("&6删除了商品 &c$id")
                if (success) it.sendColorMessage(Lang.command__remove_success)
                else it.sendColorMessage(Lang.command__remove_failure)

            }
        }
        node("buy") {
            default = PermissionDefault.OP
            description = "为玩家购买 <份数> 个云购商品"
            async = true
            params = listOf(
                Param("[玩家]", suggestRuntime = ParamSuggestCache.playerParam),
                Param("[id]", suggestRuntime = cargosSuggest),
                Param("<份数>", suggest = listOf("1", "5", "10"))
            )
            executor {
                if (!DatabaseConfig.isConnected) return@executor
                val player = getParam<Player>(0)
                val id = getParam<String>(1)
                val count = getOptionalParam<Int>(2) ?: 1
                if (count <= 0) throw ParmaException("&c请输入大于0的份数")
                dbTransaction {
                    if (SimpleLogger.isDebug) addLogger(StdOutSqlLogger)
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
                        player.sendColorMessage(Lang.command__buy_error)
                        return@dbTransaction
                    }
                    player.sendColorMessage(Lang.command__buy_success.formatBy(id, count))
                    debug("&a已为 &6${player.name} &a购买 &6$id &aX &6$count")
                    if (after == cargo.num) {
                        //开奖
                        broadcast(Lang.command__buy_start.formatBy(id, Config.countdown))
                        Lotteries.drawLottery(id)
                        debug("&6$id &a已开奖")
                    }
                }
            }
        }
        node("force") {
            default = PermissionDefault.OP
            description = "强制开启倒计时"
            async = true
            params = listOf(
                Param("[id]", suggestRuntime = cargosSuggest)
            )
            executor {
                if (!DatabaseConfig.isConnected) return@executor
                val id = getParam<String>(0)
                if (Lotteries.drawLottery(id) == null) {
                    it.sendColorMessage("&c没有人购买这一期的商品")
                    return@executor
                }
                broadcast(Lang.command__buy_start.formatBy(id, Config.countdown))
                it.sendColorMessage("&a强制开启成功")
                debug("&6强制开启了商品 &c$id")
            }
        }
        node("toggle") {
            alias = arrayOf("")
            default = PermissionDefault.OP
            description = "切换商品开启状态"
            async = true
            params = listOf(
                Param("[id]", suggestRuntime = {
                    val currentTimeMillis = System.currentTimeMillis()
                    if (System.currentTimeMillis() - lastUpdate < 2000L) return@Param suggest ?: emptyList()
                    lastUpdate = currentTimeMillis
                    suggest = getCargosNames()
                    suggest!!
                })
            )
            executor {
                val id = getParam<String>(0)
                dbTransaction {
                    if (SimpleLogger.isDebug) addLogger(StdOutSqlLogger)
                    val cargo = Cargo.findById(id) ?: throw ParmaException(Lang.command__buy_id_unexist)
                    cargo.enable = !cargo.enable
                    it.sendColorMessage(Lang.command__toogle.formatBy(cargo.enable))
                }
            }
        }
    }
}