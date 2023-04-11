package top.iseason.bukkit.yungou.data

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.datetime

//购买记录
object Records : IntIdTable("Records") {
    val uid = uuid("uid")
    val cargo = reference("cargo", Cargos, ReferenceOption.CASCADE, ReferenceOption.CASCADE)
    val num = integer("num")
    val serial = integer("serial")
    val time = datetime("time")

}