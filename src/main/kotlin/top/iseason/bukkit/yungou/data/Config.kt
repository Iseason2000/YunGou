package top.iseason.bukkit.yungou.data

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.bukkit.configuration.file.FileConfiguration
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Schema
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import top.iseason.bukkit.bukkittemplate.config.SimpleYAMLConfig
import top.iseason.bukkit.bukkittemplate.config.annotations.Comment
import top.iseason.bukkit.bukkittemplate.config.annotations.FilePath
import top.iseason.bukkit.bukkittemplate.config.annotations.Key
import top.iseason.bukkit.bukkittemplate.debug.info
import top.iseason.bukkit.yungou.YunGou
import java.sql.SQLException

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

    @Comment("", "开奖倒计时，单位秒")
    @Key
    var countdown = 30

    var isConnected = false

    var isInit = false
    override val onLoaded: FileConfiguration.() -> Unit = {
        if (!isInit) {
            reConnectedDB()
            isInit = true
        }
    }
    override val onSaved: (FileConfiguration.() -> Unit) = {

    }

    private var ds: HikariDataSource? = null

    fun closeDB() {
        try {
            ds?.close()
            TransactionManager.closeAndUnregister(YunGou.mysql)
        } catch (_: Exception) {
        }
    }

    fun reConnectedDB() {
        isConnected = false
        info("&6数据库链接中...")
        try {
            closeDB()
            val config = HikariConfig().apply {
                jdbcUrl = "jdbc:mysql://$url?charactorEncoding=utf-8mb4"
                driverClassName = "com.mysql.cj.jdbc.Driver"
                username = user
                password = Config.password
                maximumPoolSize = 10
                addDataSourceProperty("cachePrepStmts", "true")
                addDataSourceProperty("prepStmtCacheSize", "250")
                addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
                poolName = "云购"
            }
            val schema = Schema(dbName)
            ds = HikariDataSource(config)
            YunGou.mysql = Database.connect(ds!!)
            transaction {
//                    addLogger(StdOutSqlLogger)
                SchemaUtils.createSchema(schema)
                SchemaUtils.setSchema(schema)
                SchemaUtils.create(Cargos, Records, Lotteries)
            }
            isConnected = true
            info("&a数据库链接成功!")
        } catch (e: Exception) {
            info("&c数据库链接失败!")
        } catch (e: SQLException) {
            info("&c数据库链接失败!")
        }

    }

}