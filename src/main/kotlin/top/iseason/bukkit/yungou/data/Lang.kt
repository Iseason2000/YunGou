package top.iseason.bukkit.yungou.data

import org.bukkit.configuration.file.FileConfiguration
import top.iseason.bukkit.bukkittemplate.config.SimpleYAMLConfig
import top.iseason.bukkit.bukkittemplate.config.annotations.Comment
import top.iseason.bukkit.bukkittemplate.config.annotations.FilePath
import top.iseason.bukkit.bukkittemplate.config.annotations.Key
import top.iseason.bukkit.bukkittemplate.debug.SimpleLogger

@Key
@FilePath("lang.yml")
object Lang : SimpleYAMLConfig() {
    @Comment("消息前缀")
    var prefix = "&6[&a云购&6]&r "

    @Comment("", "命令消息")
    var command = ""
    var command__debug_on = "&a调试模式已开启!"
    var command__debug_off = "&6调试模式已关闭!"

    var command__add_no_item = "&c请拿着需要上架的物品"
    var command__add_id_exist = "&cid已存在"
    var command__add_id_success = "&a商品 &6{0} &aX &6{1} &a份 创建成功! 冷却时间: &6{2} &a分钟"
    var command__get_success = "&a已获得商品&6 {0}"
    var command__get_failure = "&6商品ID &a{0} 不存在"
    var command__remove_success = "&a商品已删除!"
    var command__remove_failure = "&c商品不存在!"

    var command__buy_id_unexist = "&cid不存在"
    var command__buy_can_not_buy = "&c商品剩余 &6{0} &c个,无法购买 &6{1} &c个"
    var command__buy_start = "&a商品 &6{0} &a已售完,将在&6 {1} &7秒后开奖"
    var command__buy_error = "&c购买异常，请联系管理员!"
    var command__buy_success = "&a已购买 &6{0} &aX &6{1}"
    override val onLoaded: FileConfiguration.() -> Unit = {
        SimpleLogger.prefix = prefix
    }
}