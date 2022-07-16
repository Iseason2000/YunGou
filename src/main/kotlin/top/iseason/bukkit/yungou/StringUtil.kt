package top.iseason.bukkit.yungou

import com.tuershen.nbtlibrary.NBTLibraryMain
import org.bukkit.inventory.ItemStack
import top.iseason.bukkit.yungou.StringUtil.gzip
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets.UTF_8
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object StringUtil {
    fun String.gzip(): ByteArray {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).bufferedWriter(UTF_8).use { it.write(this) }
        return bos.toByteArray()
    }

    fun unGzip(content: ByteArray): String =
        GZIPInputStream(content.inputStream()).bufferedReader(UTF_8).use { it.readText() }
}

object ItemUtil {
    fun ItemStack.toByteArray() = NBTLibraryMain.libraryApi.serializeItem.serialize(this).gzip()
    fun fromByteArray(bytes: ByteArray): ItemStack =
        NBTLibraryMain.libraryApi.serializeItem.deserialize(StringUtil.unGzip(bytes))
}
