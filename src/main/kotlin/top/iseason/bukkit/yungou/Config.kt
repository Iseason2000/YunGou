package top.iseason.bukkit.yungou

import com.mysql.cj.jdbc.exceptions.SQLError
import org.bukkit.configuration.file.FileConfiguration
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import top.iseason.bukkit.bukkittemplate.config.SimpleYAMLConfig
import top.iseason.bukkit.bukkittemplate.config.annotations.Comment
import top.iseason.bukkit.bukkittemplate.config.annotations.FilePath
import top.iseason.bukkit.bukkittemplate.config.annotations.Key
import top.iseason.bukkit.bukkittemplate.debug.info
import top.iseason.bukkit.bukkittemplate.debug.warn
import top.iseason.bukkit.bukkittemplate.utils.submit
import java.sql.SQLException
import java.time.LocalDateTime

@FilePath("config.yml")
object Config : SimpleYAMLConfig() {

    @Comment("mysql地址")
    @Key
    var url = "localhost:3306"

    @Comment("", "mysql数据库名")
    @Key
    var dbName = "yungou"

    @Comment("", "mysql用户名")
    @Key
    var user = "test"

    @Comment("", "mysql密码")
    @Key
    var password = "password"

    override val onLoaded: FileConfiguration.() -> Unit = {
        info("&6数据库链接中...")
        submit(async = true) {
            try {
                val schema = Schema(dbName)
                YunGou.mysql = Database.connect(
                    "jdbc:mysql://$url?autoReconnect=true&useSSL=false",
                    driver = "com.mysql.cj.jdbc.Driver",
                    user = user,
                    password = password
                )
                transaction {
                    SchemaUtils.createSchema(schema)
                    SchemaUtils.setSchema(schema)
                    SchemaUtils.create(Cargos)
                    Cargo.new("test") {
                        item = "asifhuasdasdasdfasdasdasdasdasdasdargdtghdfiagf"
                        count = 100
                        startTime = LocalDateTime.now()
                        endTime = LocalDateTime.now().plusDays(10)
                    }
                }
                info("&a数据库链接成功!")
            } catch (e: Exception) {
                e.printStackTrace()
                info("&a数据库链接失败!")
            } catch (e: SQLException) {
                e.printStackTrace()
                info("&a数据库链接失败!")
            }
        }
    }
    override val onSaved: (FileConfiguration.() -> Unit) = {
//        println("saved")
    }

}