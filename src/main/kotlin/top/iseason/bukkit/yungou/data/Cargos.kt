package top.iseason.bukkit.yungou.data

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import top.iseason.bukkit.bukkittemplate.debug.SimpleLogger

object Cargos : StringIdTable() {
    val item = blob("item")
    val num = integer("num")
    val enable = bool("enable").default(true)
    val startTime = datetime("startTime")
    val serial = integer("serial") //累计开了几次奖
    val lastTime = datetime("lastTime").nullable() //上次开奖
    val coolDown = integer("coolDown")

    //是否存在某个id的商品
    fun has(id: String): Boolean {
        return try {
            var has = false
            transaction {
                if (SimpleLogger.isDebug) addLogger(StdOutSqlLogger)
                has = !Cargos.slice(Cargos.id).select { Cargos.id eq id }.limit(1).empty()
            }
            has
        } catch (e: Exception) {
            false
        }
    }
}

open class StringIdTable(name: String = "", columnName: String = "id") : IdTable<String>(name) {
    final override val id: Column<EntityID<String>> = varchar(columnName, 20).entityId()
    final override val primaryKey = PrimaryKey(id)
}