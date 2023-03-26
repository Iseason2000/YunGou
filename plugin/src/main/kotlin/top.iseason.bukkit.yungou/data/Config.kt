package top.iseason.bukkit.yungou.data

import org.jetbrains.exposed.sql.*
import top.iseason.bukkittemplate.config.SimpleYAMLConfig
import top.iseason.bukkittemplate.config.annotations.Comment
import top.iseason.bukkittemplate.config.annotations.FilePath
import top.iseason.bukkittemplate.config.annotations.Key

@FilePath("config.yml")
object Config : SimpleYAMLConfig() {

    @Comment("", "开奖倒计时，单位秒")
    @Key
    var countdown = 30

    @Comment("", "玩家进服之后提醒领取奖品的延迟, 单位tick")
    @Key
    var tip_delay: Long = 60

}