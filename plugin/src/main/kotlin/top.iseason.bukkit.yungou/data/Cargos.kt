package top.iseason.bukkit.yungou.data

import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import top.iseason.bukkittemplate.config.StringIdTable
import top.iseason.bukkittemplate.config.dbTransaction
import top.iseason.bukkittemplate.utils.other.EasyCoolDown

object Cargos : StringIdTable() {
    val item = blob("item")
    val command = varchar("command", 255).nullable().default(null)
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
            dbTransaction {
                has = !Cargos.slice(Cargos.id).select { Cargos.id eq id }.limit(1).empty()
            }
            has
        } catch (e: Exception) {
            false
        }
    }

    private var keys = listOf<String>()

    fun getKeys(): List<String> {
        if (!EasyCoolDown.check("cargos_cool_down", 2000)) {
            keys = dbTransaction {
                return@dbTransaction Cargos.slice(Cargos.id).selectAll().map {
                    it[Cargos.id].value
                }
            }
        }
        return keys

    }
}
