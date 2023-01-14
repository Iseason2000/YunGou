package top.iseason.bukkit.yungou.data

import org.jetbrains.exposed.dao.id.EntityID
import top.iseason.bukkittemplate.config.StringEntity
import top.iseason.bukkittemplate.config.StringEntityClass
import java.time.LocalDateTime

class Cargo(id: EntityID<String>) : StringEntity(id) {
    companion object : StringEntityClass<Cargo>(Cargos)

    var item by Cargos.item
    var num by Cargos.num
    var enable by Cargos.enable
    var startTime by Cargos.startTime
    var serial by Cargos.serial
    var lastTime by Cargos.lastTime
    var coolDown by Cargos.coolDown

    fun isCoolDown(): Boolean {
        val lt = lastTime ?: return false
        return lt.plusMinutes(coolDown.toLong()).isAfter(LocalDateTime.now())
    }
}
