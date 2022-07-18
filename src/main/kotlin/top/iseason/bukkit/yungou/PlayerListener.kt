package top.iseason.bukkit.yungou

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerLoginEvent
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import top.iseason.bukkit.bukkittemplate.debug.SimpleLogger
import top.iseason.bukkit.bukkittemplate.utils.sendColorMessage
import top.iseason.bukkit.bukkittemplate.utils.submit
import top.iseason.bukkit.yungou.data.Config
import top.iseason.bukkit.yungou.data.Lang
import top.iseason.bukkit.yungou.data.Lotteries

object PlayerListener : Listener {
    @EventHandler(ignoreCancelled = true)
    fun onPlayerLoginEvent(event: PlayerLoginEvent) {
        if (!Config.isConnected) return
        submit(async = true) {
            val player = event.player
            val uniqueId = player.uniqueId
            var count = 0L
            transaction {
                count = Lotteries.slice(Lotteries.id)
                    .select { Lotteries.uid eq uniqueId and (Lotteries.hasReceive eq false) }.count()
            }
            if (count == 0L) return@submit
            player.sendColorMessage("${SimpleLogger.prefix}${Lang.player_login.formatBy(count)}")
        }
    }
}