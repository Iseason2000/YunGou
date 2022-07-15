package top.iseason.bukkit.yungou

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable

class Cargo(id: EntityID<String>) : StringEntity(id) {
    companion object : StringEntityClass<Cargo>(Cargos)

    //    var name by Cargos.name
    var item by Cargos.item
    var count by Cargos.count
    var startTime by Cargos.startTime
    var endTime by Cargos.endTime
}

abstract class StringEntity(id: EntityID<String>) : Entity<String>(id)

abstract class StringEntityClass<out E : Entity<String>> constructor(
    table: IdTable<String>,
    entityType: Class<E>? = null,
    entityCtor: ((EntityID<String>) -> E)? = null
) : EntityClass<String, E>(table, entityType, entityCtor)
