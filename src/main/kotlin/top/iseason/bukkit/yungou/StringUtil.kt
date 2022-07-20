package top.iseason.bukkit.yungou

import org.bukkit.inventory.ItemStack
import org.bukkit.util.io.BukkitObjectInputStream
import org.bukkit.util.io.BukkitObjectOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

object ItemUtil {
    fun ItemStack.toByteArray(): ByteArray {
        val outputStream = ByteArrayOutputStream()
        BukkitObjectOutputStream(outputStream).use { it.writeObject(this) }
        return outputStream.toByteArray()
    }

    fun fromByteArray(bytes: ByteArray): ItemStack {
        BukkitObjectInputStream(ByteArrayInputStream(bytes)).use { return it.readObject() as ItemStack }
    }
}

fun String.formatBy(vararg values: Any): String {
    var temp = this
    values.forEachIndexed { index, any ->
        temp = temp.replace("{$index}", any.toString())
    }
    return temp
}
