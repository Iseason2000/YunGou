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
import top.iseason.bukkit.bukkittemplate.dependency.DependencyLoader
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
            val config = when (dbType) {
                "SQLite" -> HikariConfig().apply {
                    DependencyLoader.builder().repository("https://maven.aliyun.com/repository/public")
                        .groupId("org.xerial")
                        .artifactId("sqlite-jdbc")
                        .version("3.36.0.3").build().load()
                    jdbcUrl = "jdbc:sqlite:$url"
                    driverClassName = "org.sqlite.JDBC"
                }
                "H2" -> HikariConfig().apply {
                    DependencyLoader.builder().repository("https://maven.aliyun.com/repository/public")
                        .groupId("com.h2database")
                        .artifactId("h2")
                        .version("2.1.214").build().load()
                    jdbcUrl = "jdbc:h2:$url"
                    driverClassName = "org.h2.Driver"
                }
                "PostgreSQL" -> HikariConfig().apply {
                    DependencyLoader.builder().repository("https://maven.aliyun.com/repository/public")
                        .groupId("com.impossibl.pgjdbc-ng")
                        .artifactId("pgjdbc-ng")
                        .version("0.8.9").build().load()
                    jdbcUrl = "jdbc:postgresql://$url"
                    driverClassName = "com.impossibl.postgres.jdbc.PGDriver"
                    username = user
                    password = Config.password
                }
                "Oracle" -> HikariConfig().apply {
                    DependencyLoader.builder().repository("https://maven.aliyun.com/repository/public")
                        .groupId("com.oracle.database.jdbc")
                        .artifactId("ojdbc8")
                        .version("21.6.0.0.1").build().load()
                    jdbcUrl = "dbc:oracle:thin:@//$url"
                    driverClassName = "oracle.jdbc.OracleDriver"
                    username = user
                    password = Config.password
                }
                "SQLServer" -> HikariConfig().apply {
                    DependencyLoader.builder().repository("https://maven.aliyun.com/repository/public")
                        .groupId("com.microsoft.sqlserver")
                        .artifactId("mssql-jdbc")
                        .version("10.2.1.jre8").build().load()
                    jdbcUrl = "jdbc:sqlserver://$url"
                    driverClassName = "com.microsoft.sqlserver.jdbc.SQLServerDriver"
                    username = user
                    password = Config.password
                }
                "MySQL", "MariaDB" -> HikariConfig().apply {
                    DependencyLoader.builder().repository("https://maven.aliyun.com/repository/public")
                        .groupId("mysql")
                        .artifactId("mysql-connector-java")
                        .version("8.0.29").build().load()
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