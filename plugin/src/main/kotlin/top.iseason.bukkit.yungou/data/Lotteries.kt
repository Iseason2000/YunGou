package top.iseason.bukkit.yungou.data

import org.bukkit.Bukkit
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.sum
import top.iseason.bukkittemplate.config.dbTransaction
import top.iseason.bukkittemplate.debug.debug
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.broadcast
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.formatBy
import top.iseason.bukkittemplate.utils.other.RandomUtils
import top.iseason.bukkittemplate.utils.other.submit
import java.time.LocalDateTime
import java.util.*

//获奖名单
object Lotteries : IntIdTable() {
    val uid = uuid("uid")
    val cargo = reference("cargo", Cargos, ReferenceOption.CASCADE, ReferenceOption.CASCADE)
    val serial = integer("serial")
    val time = datetime("time")
    val hasReceive = bool("hasReceive").default(false)

    //开奖
    fun drawLottery(id: String): UUID? {
        val records = mutableMapOf<UUID, Int>()
        var total = 0
        var winner: UUID? = null
        lateinit var cargo: Cargo
        dbTransaction {
            cargo = Cargo.findById(id)!!
            val list =
                Records.slice(Records.uid, Records.num.sum())
                    .select { Records.cargo eq id and (Records.serial eq cargo.serial) }.groupBy(Records.uid)
            if (list.empty()) {
                return@dbTransaction
            }
            for (resultRow in list) {
                val num = resultRow[Records.num.sum()]!!
                records[resultRow[Records.uid]] = num
                total += num
            }

            val random = RandomUtils.getInteger(0, total)
            var temp = 0
            for (record in records) {
                temp += record.value
                //中奖
                if (random <= temp) {
                    winner = record.key
                    break
                }
            }
            val new = Lottery.new {
                uid = winner!!
                this.cargo = cargo
                serial = cargo.serial
                time = LocalDateTime.now()
            }
            val name = Bukkit.getOfflinePlayer(winner!!).name ?: winner.toString()
            submit(async = true, delay = Config.countdown * 20L) {
                broadcast(Lang.receive_broadcastMessage.formatBy(name, id))
                debug(Lang.receive_broadcastMessage.formatBy(name, id))
                dbTransaction {
                    new.offeringPrizes()
                }
            }
            cargo.serial += 1
            cargo.lastTime = LocalDateTime.now()
        }
        return winner
    }

}