package top.iseason.bukkit.yungou.data

import org.bukkit.Bukkit
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.canAddItem
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.formatBy
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessage

class Lottery(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Lottery>(Lotteries)

    var uid by Lotteries.uid
    var cargo by Cargo referencedOn Lotteries.cargo
    var serial by Lotteries.serial
    var time by Lotteries.time
    var hasReceive by Lotteries.hasReceive

    //发放奖品
    fun offeringPrizes() {
        val car: Cargo
        try {
            car = cargo
        } catch (e: Exception) {
            return
        }
        val player = Bukkit.getPlayer(uid) ?: return
        if (hasReceive) {
            player.sendColorMessage(Lang.receive_success.formatBy(car.id))
            return
        }
        val item = ItemUtils.fromByteArray(car.item.bytes)
        if (player.canAddItem(item) == 0) {
            player.inventory.addItem(item)
            hasReceive = true
            player.sendColorMessage(Lang.receive_success.formatBy(car.id))
        } else {
            player.sendColorMessage(Lang.receive_inventory_full.formatBy(cargo.id))
        }
    }
}