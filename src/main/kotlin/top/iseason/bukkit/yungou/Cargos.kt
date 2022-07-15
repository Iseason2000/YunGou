package top.iseason.bukkit.yungou

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.datetime

object Cargos : StringIdTable() {
    val item = text("item")
    val count = integer("count")
    val startTime = datetime("startTime")
    val endTime = datetime("endTime")
}

open class StringIdTable(name: String = "", columnName: String = "id") : IdTable<String>(name) {
    final override val id: Column<EntityID<String>> = varchar(columnName, 20).entityId()
    final override val primaryKey = PrimaryKey(id)
}