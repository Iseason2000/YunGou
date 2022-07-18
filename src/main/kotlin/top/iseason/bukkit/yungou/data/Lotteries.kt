package top.iseason.bukkit.yungou.data

import org.bukkit.Bukkit
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.sum
import org.jetbrains.exposed.sql.transactions.transaction
import top.iseason.bukkit.bukkittemplate.debug.SimpleLogger
import top.iseason.bukkit.bukkittemplate.debug.debug
import top.iseason.bukkit.bukkittemplate.utils.RandomUtils
import top.iseason.bukkit.bukkittemplate.utils.broadcast
import top.iseason.bukkit.bukkittemplate.utils.submit
import top.iseason.bukkit.yungou.formatBy
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
        transaction {
//                addLogger(StdOutSqlLogger)
            cargo = Cargo.findById(id)!!
            val list =
                Records.slice(Records.uid, Records.num.sum()).select { Records.cargo eq id }.groupBy(Records.uid)
            if (list.empty()) {
                return@transaction
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
            submit(async = true, delay = Config.countdown * 20L) {
                broadcast("${SimpleLogger.prefix}${Lang.receive_broadcast.formatBy(Bukkit.getPlayer(winner).name, id)}")
                debug(Lang.receive_broadcast.formatBy(Bukkit.getPlayer(winner).name, id))
                transaction {
                    new.offeringPrizes()
                }
            }
            cargo.serial += 1
            cargo.lastTime = LocalDateTime.now()
        }
        return winner
    }

}