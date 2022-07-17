package top.iseason.bukkit.yungou.data

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class Lottery(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Lottery>(Lotteries)

    var uid by Lotteries.uid
    var cargo by Lotteries.cargo
    var time by Lotteries.time
    var hasReceive by Lotteries.hasReceive
}