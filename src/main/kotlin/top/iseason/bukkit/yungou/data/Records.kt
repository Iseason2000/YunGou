package top.iseason.bukkit.yungou.data

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.datetime

object Records : IntIdTable() {
    val uid = uuid("uid")
    val cargo = reference("cargo", Cargos)
    val num = integer("num")
    val time = datetime("time")
}