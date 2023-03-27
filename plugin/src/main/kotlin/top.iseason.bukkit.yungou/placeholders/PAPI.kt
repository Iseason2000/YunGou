package top.iseason.bukkit.yungou.placeholders

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.sum
import top.iseason.bukkit.yungou.data.*
import top.iseason.bukkittemplate.BukkitTemplate
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
    private val hasBuyCache = WeakHashMap<String, String>()
    val playerBuy = WeakHashMap<UUID, String>()
    private val lotteryAll = WeakHashMap<String, Lottery?>()
    private val lotteries = WeakHashMap<String, Lottery?>()

    override fun getAuthor(): String {
        return BukkitTemplate.getPlugin().description.authors.joinToString()
    }

    override fun getIdentifier(): String {
        return BukkitTemplate.getPlugin().description.name
    }

    override fun getVersion(): String {
        return BukkitTemplate.getPlugin().description.version
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
                    val existNum: Int = dbTransaction {
                        Records.slice(Records.num.sum())
                            .select { Records.cargo eq id and (Records.serial eq cargo.serial) }.firstOrNull()
                            ?.get(Records.num.sum()) ?: 0
                    }
                    return (existNum + count <= cargo.num).toString()
                }
                if ("hasbuy".equals(type, true)) {
                    if (!DatabaseConfig.isConnected || !cargo.enable) return "0"
                    val cache = hasBuyCache[params]
                    if (cache != null && coolDown.check(params, 1000)) {
                        return cache
                    }
                    val existNum: Int = dbTransaction {
                        Records.slice(Records.num.sum())
                            .select { Records.cargo eq id and (Records.serial eq cargo.serial) }.firstOrNull()
                            ?.get(Records.num.sum()) ?: 0
                    }
                    hasBuyCache[params] = existNum.toString()
                    return existNum.toString()
                }
                when (type.lowercase()) {
//                    "id" -> return cargo!!.id.value
                    "num" -> return cargo.num.toString()
                    "sold" -> {
                        if (!DatabaseConfig.isConnected) return "0"
                        return dbTransaction {
                            Records.slice(Records.num.sum())
                                .select { Records.cargo eq id and (Records.serial eq cargo.serial) }.firstOrNull()
                                ?.get(Records.num.sum()) ?: 0
                        }.toString()
                    }

                    "remain" -> {
                        if (!DatabaseConfig.isConnected) return "0"
                        return (cargo.num - dbTransaction {
                            Records.slice(Records.num.sum())
                                .select { Records.cargo eq id and (Records.serial eq cargo.serial) }.firstOrNull()
                                ?.get(Records.num.sum()) ?: 0
                        }).toString()
                    }

                    "enable" -> return cargo.enable.toString()
                    "starttime" -> return cargo.startTime.toString()
                    "lasttime" -> return cargo.lastTime.toString()
                    "serial" -> return cargo.serial.toString()
                    "iscooldown" -> return cargo.isCoolDown().toString()
                    "cooldown" -> return cargo.coolDown.toString()
                    "cooldownremain" -> return if (cargo.lastTime == null) "0" else max(
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
                if ("*" == id) {
                    lottery = lotteryAll[params]
                    if (!lotteries.containsKey(params) || !coolDown.check(params, 30000)) {
                        lottery = dbTransaction {
                            Lottery.all().limit(1, count.toLong()).orderBy(Lotteries.time to SortOrder.DESC)
                                .firstOrNull()
                        }
                        lotteryAll[params] = lottery
                    }
                } else {
                    lottery = lotteries[params]
                    if (!lotteries.containsKey(params) || !coolDown.check(params, 30000)) {
                        lottery = dbTransaction {
                            Lottery.find { Lotteries.cargo eq id }.limit(1, count.toLong())
                                .orderBy(Lotteries.time to SortOrder.DESC)
                                .firstOrNull()
                        }
                        lotteries[params] = lottery
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
                        if (hasBuy != null && coolDown.check(uniqueId.toString(), 5000)) return hasBuy.toString()
                        hasBuy = dbTransaction {
                            Records.slice(Records.num.sum())
                                .select { Records.cargo eq id and (Records.serial eq cargo.serial) and (Records.uid eq uniqueId) }
                                .firstOrNull()
                                ?.get(Records.num.sum()) ?: 0
                        }.toString()
                        playerBuy[uniqueId] = hasBuy
                        return hasBuy
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
        if (coolDown.check(id, 1000L) && cargo != null) {
            return cargo
        } else try {
            cargo = dbTransaction { Cargo.findById(id) }
            cargoCache[id] = cargo
        } catch (e: Exception) {
            return null
        }
        return cargo
    }
}