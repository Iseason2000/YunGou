package top.iseason.bukkit.yungou.data

import org.bukkit.configuration.ConfigurationSection
import top.iseason.bukkittemplate.config.SimpleYAMLConfig
import top.iseason.bukkittemplate.config.annotations.Comment
import top.iseason.bukkittemplate.config.annotations.FilePath
import top.iseason.bukkittemplate.config.annotations.Key
import top.iseason.bukkittemplate.debug.SimpleLogger
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils

@Key
@FilePath("lang.yml")
object Lang : SimpleYAMLConfig() {
    @Comment("消息前缀")
    var prefix = "&6[&a云购&6]&r "

    var receive_success = "&a奖励 &6{0} &a已发放到你的背包"
    var receive_broadcastMessage = "&a恭喜玩家 &6{0} &a获得 &6{1}"
    var receive_inventory_full = "&6你的背包空间不足，无法领取奖励 &a{0} &6 ,请腾出空间后输入/yungou get {0} 领取"

    var player_login = "&7你有 &6{0} &7个奖励未领取，请输入&6/yungou get [id] &7领取"

    @Comment("", "命令消息")
    var command = ""
    var command__debug_on = "&a调试模式已开启!"
    var command__debug_off = "&6调试模式已关闭!"

    var command__add_no_item = "&c请拿着需要上架的物品"
    var command__add_id_exist = "&cid已存在"
    var command__add_id_success = "&a商品 &6{0} &aX &6{1} &a份 创建成功! 冷却时间: &6{2} &a分钟"

    var command__show_id_not_exist = "&cid不存在"
    var command__show_id = "&7商品ID: &6{0}"
    var command__show_num = "&7商品份数: &6{0}"
    var command__show_enable = "&7开启状态: &6{0}"
    var command__show_time = "&7上架时间: &6{0}"
    var command__show_serial = "&7商品期号: &6{0}"
    var command__show_lastTime = "&7上次开奖: &6{0}"
    var command__show_cooldown = "&7冷却时间: &6{0} 分钟"

    var command__get_failure = "&6商品ID &a{0} 不存在"
    var command__get_all_empty = "&6你没有待领取的物品!"
    var command__get_all_remain = "&a你领取了 {0} 个物品，背包空间已满无法继续领取!"
    var command__get_all_success = "&a你领取了所有的物品，共 {0} 个"


    var command__list_head = "&7你的获奖商品有:"
    var command__list_body = "&6商品: {0} 第 {1} 期"

    var command__remove_success = "&a商品已删除!"
    var command__remove_failure = "&c商品不存在!"

    var command__buy_id_unexist = "&cid不存在"
    var command__buy_not_enable = "&c商品 &6{0} &c未开售"
    var command__buy_is_cooldown = "&c商品 &6{0} &c冷却中"
    var command__buy_can_not_buy = "&c商品剩余 &6{0} &c个,无法购买 &6{1} &c个"
    var command__buy_start = "&a商品 &6{0} &a已售完,将在&6 {1} &a秒后开奖"
    var command__buy_error = "&c购买异常，请联系管理员!"
    var command__buy_success = "&a已购买 &6{0} &aX &6{1}"

    var command__toogle = "&a商品当前状态为: &6{0}"

    var placeholder__no_record = "没有记录"
    var placeholder__record = "玩家 {0} 获得了{1} 第 {2} 期 时间 {3}"

    override fun onLoaded(section: ConfigurationSection) {
        SimpleLogger.prefix = prefix
        MessageUtils.defaultPrefix = prefix
    }
}