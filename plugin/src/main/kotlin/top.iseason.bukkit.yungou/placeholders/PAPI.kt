package top.iseason.bukkit.yungou.placeholders

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.sum
import top.iseason.bukkit.yungou.data.*
import top.iseason.bukkittemplate.config.DatabaseConfig
import top.iseason.bukkittemplate.config.dbTransaction
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.formatBy
import top.iseason.bukkittemplate.utils.other.EasyCoolDown
import top.iseason.bukkittemplate.utils.other.WeakCoolDown
import java.lang.Long.max
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

object PAPI : PlaceholderExpansion() {

    private val coolDown = WeakCoolDown<String>()
    private val cargoCache = WeakHashMap<String, Cargo>()
    private val cargoBuy = WeakHashMap<String, Int>()
    private val playerBuy = WeakHashMap<UUID, Int>()
    private val lotteryAll = WeakHashMap<Int, Lottery>()
    private val lotteries = WeakHashMap<String, Lottery>()

    override fun getAuthor(): String {
        return "Iseason"
    }

    override fun getIdentifier(): String {
        return "yungou"
    }

    override fun getVersion(): String {
        return "1.0.0"
    }

    override fun persist(): Boolean {
        return true // This is required or else PlaceholderAPI will unregister the Expansion on reload
    }

    override fun onRequest(player: OfflinePlayer?, params: String): String? {
        if (params.isEmpty()) return null
        if (!DatabaseConfig.isConnected && !EasyCoolDown.check("reconnected-by-placeholder", 5000L)) {
            DatabaseConfig.reConnected()
        }
        val args = params.split("_")
        when (args[0].lowercase()) {
            "cargo" -> {
                val id = args.getOrNull(1) ?: return null
                val type = args.getOrNull(2) ?: return null
                val cargo = getCargo(id) ?: return null
                val count = try {
                    args.getOrNull(3)?.toInt() ?: 1
                } catch (e: Exception) {
                    1
                }
                if ("canbuy".equals(type, true)) {
                    if (!cargo.enable || cargo.isCoolDown()) return "false"
                    var existNum: Int? = cargoBuy[id]
                    if (!(coolDown.check(id, 500L) && existNum != null)) {
                        try {
                            dbTransaction {
                                existNum = Records.slice(Records.num.sum())
                                    .select { Records.cargo eq id and (Records.serial eq cargo.serial) }.firstOrNull()
                                    ?.get(Records.num.sum()) ?: 0
                            }
                        } catch (_: Exception) {
                        }
                    }
                    if (existNum == null) return "false"
                    return (existNum!! + count <= cargo.num).toString()
                }
                if ("hasbuy".equals(type, true)) {
                    if (!DatabaseConfig.isConnected || !cargo.enable) return "0"
                    var existNum: Int? = null
                    try {
                        dbTransaction {
                            existNum = Records.slice(Records.num.sum())
                                .select { Records.cargo eq id and (Records.serial eq cargo.serial) }.firstOrNull()
                                ?.get(Records.num.sum()) ?: 0
                        }
                    } catch (_: Exception) {
                    }
                    if (existNum == null) return "0"
                    return existNum.toString()
                }
                when (type.lowercase()) {
//                    "id" -> return cargo!!.id.value
                    "num" -> return cargo.num.toString()
                    "enable" -> return cargo.enable.toString()
                    "starttime" -> return cargo.startTime.toString()
                    "lasttime" -> return cargo.lastTime.toString()
                    "serial" -> return cargo.serial.toString()
                    "iscooldown" -> return cargo.isCoolDown().toString()
                    "cooldown" -> return cargo.coolDown.toString()
                    "cooldownremain" -> return if (cargo.lastTime == null) null else max(
                        Duration.between(
                            LocalDateTime.now(),
                            cargo.lastTime!!.plusMinutes(cargo.coolDown.toLong())
                        ).toMinutes(), 0L
                    ).toString()

                    else -> return null
                }
            }

            "lottery" -> {
                val id = args.getOrNull(1) ?: return null
                val count = try {
                    args.getOrNull(2)?.toInt() ?: return null
                } catch (e: Exception) {
                    return null
                }
                var lottery: Lottery?
                if ("*".equals(id, true)) {
                    lottery = lotteryAll[count]
                    if (lottery == null || !coolDown.check("all_$count", 3000)) {
                        dbTransaction {
//                            addLogger(StdOutSqlLogger)
                            lottery =
                                Lottery.all().limit(1, count.toLong()).orderBy(Lotteries.time to SortOrder.DESC)
                                    .firstOrNull()
                        }
                        lotteryAll[count] = lottery
                    }
                } else {
                    val key = "${id}_$count"
                    lottery = lotteries[key]
                    if (lottery == null || !coolDown.check(key, 3000)) {
                        dbTransaction {
                            lottery = Lottery.find { Lotteries.cargo eq id }.limit(1, count.toLong())
                                .orderBy(Lotteries.time to SortOrder.DESC)
                                .firstOrNull()
                        }
                        lotteries[key] = lottery
                    }
                }
                if (lottery == null)
                    return Lang.placeholder__no_record
                return Lang.placeholder__record.formatBy(
                    Bukkit.getOfflinePlayer(lottery!!.uid).name!!,
                    lottery!!._readValues!![Lotteries.cargo],
                    lottery!!.serial,
                    lottery!!.time
                )
            }

            "player" -> {
                val id = args.getOrNull(1) ?: return null
                val type = args.getOrNull(2) ?: return null
                if (player == null) return null
                val cargo = getCargo(id) ?: return null
                val uniqueId = player.uniqueId
                when (type.lowercase()) {
                    "hasbuy" -> {
                        var hasBuy = playerBuy[uniqueId]
                        if (hasBuy != null && coolDown.check(uniqueId.toString(), 500)) return hasBuy.toString()
                        try {
                            dbTransaction {
                                hasBuy = Records.slice(Records.num.sum())
                                    .select { Records.cargo eq id and (Records.serial eq cargo.serial) and (Records.uid eq uniqueId) }
                                    .firstOrNull()
                                    ?.get(Records.num.sum()) ?: 0
                            }
                        } catch (_: Exception) {
                        }
                        playerBuy[uniqueId] = hasBuy
                        return hasBuy.toString()
                    }

                    else -> return null
                }
            }

            else -> return null
        }
    }

    private fun getCargo(id: String): Cargo? {
        if (!DatabaseConfig.isConnected) return null
        var cargo: Cargo? = cargoCache[id]
        if (coolDown.check(id, 3000L) && cargo != null) {
            dbTransaction {
                cargo!!.refresh()
            }
        } else try {
            cargo = dbTransaction { Cargo.findById(id) }
            cargoCache[id] = cargo
        } catch (e: Exception) {
            return null
        }
        return cargo
    }
}