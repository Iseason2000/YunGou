package top.iseason.bukkit.yungou.data

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class Record(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Record>(Records)

    var uid by Records.uid
    var cargo by Cargo referencedOn Records.cargo
    var num by Records.num
    var serial by Records.serial
    var time by Records.time
}