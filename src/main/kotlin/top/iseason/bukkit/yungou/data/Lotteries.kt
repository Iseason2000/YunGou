package top.iseason.bukkit.yungou.data

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.datetime

//获奖名单
object Lotteries : IntIdTable() {
    val uid = uuid("uid")
    val cargo = reference("cargo", Cargos)
    val time = datetime("time")
    val hasReceive = bool("hasReceive").default(false)
}