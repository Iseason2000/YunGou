package top.iseason.bukkit.yungou

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerLoginEvent
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import top.iseason.bukkit.yungou.data.Config
import top.iseason.bukkit.yungou.data.Lang
import top.iseason.bukkit.yungou.data.Lotteries
import top.iseason.bukkittemplate.config.DatabaseConfig
import top.iseason.bukkittemplate.config.dbTransaction
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.formatBy
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessage
import top.iseason.bukkittemplate.utils.other.submit

object PlayerListener : Listener {
    @EventHandler(ignoreCancelled = true)
    fun onPlayerLoginEvent(event: PlayerLoginEvent) {
        if (!DatabaseConfig.isConnected) return
        submit(async = true, delay = Config.tip_delay) {
            val player = event.player
            val uniqueId = player.uniqueId
            var count = 0L
            dbTransaction {
                count = Lotteries.slice(Lotteries.id)
                    .select { Lotteries.uid eq uniqueId and (Lotteries.hasReceive eq false) }.count()
            }
            if (count == 0L) return@submit
            player.sendColorMessage(Lang.player_login.formatBy(count))
        }
    }
}