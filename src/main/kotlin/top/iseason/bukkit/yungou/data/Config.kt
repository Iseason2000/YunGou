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
import top.iseason.bukkit.bukkittemplate.dependency.DependencyDownloader
import top.iseason.bukkit.yungou.YunGou
import java.io.File
import java.sql.SQLException

@FilePath("config.yml")
object Config : SimpleYAMLConfig() {

    @Comment("数据库类型:支持 MySQL、MariaDB、SQLite、H2、Oracle、PostgreSQL、SQLServer")
    @Key
    var dbType = "SQLite"

    @Comment("数据库地址")
    @Key
    var url = File(YunGou.javaPlugin.dataFolder, "yungou.db").absoluteFile.toString()

    @Comment("", "数据库名")
    @Key
    var dbName = "yungou"

    @Comment("", "数据库用户名，如果有的话")
    @Key
    var user = "test"

    @Comment("", "数据库密码，如果有的话")
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
            val dd = DependencyDownloader()
            dd.repositories.clear()
            dd.addRepository("https://maven.aliyun.com/repository/public")
            val config = when (dbType) {
                "SQLite" -> HikariConfig().apply {
                    dd.downloadDependency("org.xerial:sqlite-jdbc:3.36.0.3")
                    jdbcUrl = "jdbc:sqlite:$url"
                    driverClassName = "org.sqlite.JDBC"
                }
                "H2" -> HikariConfig().apply {
                    dd.downloadDependency("com.h2database:h2:2.1.214")
                    jdbcUrl = "jdbc:h2:$url"
                    driverClassName = "org.h2.Driver"
                }
                "PostgreSQL" -> HikariConfig().apply {
                    dd.downloadDependency("com.impossibl.pgjdbc-ng:pgjdbc-ng:0.8.9")
                    jdbcUrl = "jdbc:postgresql://$url"
                    driverClassName = "com.impossibl.postgres.jdbc.PGDriver"
                    username = user
                    password = Config.password
                }
                "Oracle" -> HikariConfig().apply {
                    dd.downloadDependency("com.oracle.database.jdbc:ojdbc8:21.6.0.0.1")
                    jdbcUrl = "dbc:oracle:thin:@//$url"
                    driverClassName = "oracle.jdbc.OracleDriver"
                    username = user
                    password = Config.password
                }
                "SQLServer" -> HikariConfig().apply {
                    dd.downloadDependency("com.microsoft.sqlserver:mssql-jdbc:10.2.1.jre8")
                    jdbcUrl = "jdbc:sqlserver://$url"
                    driverClassName = "com.microsoft.sqlserver.jdbc.SQLServerDriver"
                    username = user
                    password = Config.password
                }
                "MySQL", "MariaDB" -> HikariConfig().apply {
                    dd.downloadDependency("mysql:mysql-connector-java:8.0.29")
                    jdbcUrl = "jdbc:mysql://$url?charactorEncoding=utf-8mb4"
                    driverClassName = "com.mysql.cj.jdbc.Driver"
                    username = user
                    password = Config.password
                }
                else -> throw Exception("错误的数据库类型!")
            }
            with(config) {
                maximumPoolSize = 10
                addDataSourceProperty("cachePrepStmts", "true")
                addDataSourceProperty("prepStmtCacheSize", "250")
                addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
                poolName = "云购"
            }
            ds = HikariDataSource(config)
            YunGou.mysql = Database.connect(ds!!)
            transaction {
//                    addLogger(StdOutSqlLogger)
                if (!dbType.equals("sqlite", true)) {
                    val schema = Schema(dbName)
                    SchemaUtils.createSchema(schema)
                    SchemaUtils.setSchema(schema)
                }
                SchemaUtils.create(Cargos, Records, Lotteries)
            }
            isConnected = true
            info("&a数据库链接成功!")
        } catch (e: Exception) {
            e.printStackTrace()
            info("&c数据库链接失败!")
        } catch (e: SQLException) {
            info("&c数据库链接失败!")
        }

    }

}