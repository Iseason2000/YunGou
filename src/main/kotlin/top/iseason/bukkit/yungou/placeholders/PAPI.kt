package top.iseason.bukkit.yungou.placeholders

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import top.iseason.bukkit.bukkittemplate.utils.EasyCoolDown
import top.iseason.bukkit.bukkittemplate.utils.WeakCoolDown
import top.iseason.bukkit.yungou.data.*
import top.iseason.bukkit.yungou.formatBy
import java.lang.Long.max
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

object PAPI : PlaceholderExpansion() {

    val coolDown = WeakCoolDown<String>()
    val cargoCache = WeakHashMap<String, Cargo>()
    val cargoBuy = WeakHashMap<String, Int>()
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
        val args = params.split("_")
        when (args[0].lowercase()) {
            "cargo" -> {
                val id = args.getOrNull(1) ?: return null
                val type = args.getOrNull(2) ?: return null
                var cargo: Cargo? = cargoCache[id]
                if (coolDown.check(id, 1000L) && cargo != null) {
                    transaction {
                        cargo!!.refresh()
                    }
                } else try {
                    transaction {
                        cargo = Cargo.findById(id)
                    }
                    cargoCache[id] = cargo
                } catch (e: Exception) {
                    if (!EasyCoolDown.check("reconnected-by-placeholder", 5000L)) {
                        Config.reConnectedDB()
                    }
                    return null
                }
                if (!Config.isConnected) return null
                if (cargo == null) return null
                val count = try {
                    args.getOrNull(3)?.toInt() ?: 1
                } catch (e: Exception) {
                    1
                }
                if ("canbuy".equals(type, true)) {
                    if (!Config.isConnected || !cargo!!.enable || cargo!!.isCoolDown()) return "false"
                    var existNum: Int? = cargoBuy[id]
                    if (!(coolDown.check(id, 500L) && existNum != null)) {
                        try {
                            transaction {
                                existNum = Records.slice(Records.num.sum())
                                    .select { Records.cargo eq id and (Records.serial eq cargo!!.serial) }.firstOrNull()
                                    ?.get(Records.num.sum()) ?: 0
                            }
                        } catch (_: Exception) {
                        }
                    }
                    if (existNum == null) return "false"
                    return (existNum!! + count <= cargo!!.num).toString()
                }
                if ("hasbuy".equals(type, true)) {
                    if (!Config.isConnected || !cargo!!.enable) return "0"
                    var existNum: Int? = null
                    try {
                        transaction {
                            existNum = Records.slice(Records.num.sum())
                                .select { Records.cargo eq id and (Records.serial eq cargo!!.serial) }.firstOrNull()
                                ?.get(Records.num.sum()) ?: 0
                        }
                    } catch (_: Exception) {
                    }
                    if (existNum == null) return "0"
                    return existNum.toString()
                }
                when (type.lowercase()) {
//                    "id" -> return cargo!!.id.value
                    "num" -> return cargo!!.num.toString()
                    "enable" -> return cargo!!.enable.toString()
                    "starttime" -> return cargo!!.startTime.toString()
                    "lasttime" -> return cargo!!.lastTime.toString()
                    "serial" -> return cargo!!.serial.toString()
                    "iscooldown" -> return cargo!!.isCoolDown().toString()
                    "cooldown" -> return cargo!!.coolDown.toString()
                    "cooldownremain" -> return if (cargo!!.lastTime == null) null else max(
                        Duration.between(
                            LocalDateTime.now(),
                            cargo!!.lastTime!!.plusMinutes(cargo!!.coolDown.toLong())
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
                var limit: ResultRow? = null
                if ("*".equals(id, true)) {
                    transaction {
                        limit = Lotteries.selectAll().limit(1, count.toLong()).orderBy(Lotteries.time, SortOrder.DESC)
                            .firstOrNull()
                    }
                } else
                    transaction {
                        limit = Lotteries.select { Lotteries.cargo eq id }.limit(1, count.toLong())
                            .orderBy(Lotteries.time, SortOrder.DESC)
                            .firstOrNull()
                    }
                if (limit == null)
                    return Lang.placeholder__no_record
                return Lang.placeholder__record.formatBy(
                    Bukkit.getOfflinePlayer(limit!![Lotteries.uid]).name,
                    limit!![Lotteries.cargo],
                    limit!![Lotteries.serial],
                    limit!![Lotteries.time]
                )
            }
            else -> return null
        }
    }
}