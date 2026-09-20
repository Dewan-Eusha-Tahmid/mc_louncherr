package com.example.data.nbt

import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Parsed Level.dat metadata from Minecraft Bedrock little-endian NBT.
 */
data class BedrockLevelData(
    val levelName: String = "Bedrock World",
    val seed: Long = 0L,
    val spawnX: Int = 0,
    val spawnY: Int = 64,
    val spawnZ: Int = 0,
    val gameType: Int = 0,
    val difficulty: Int = 1,
    val time: Long = 0L,
    val generator: Int = 1,
    val storageVersion: Int = 10,
    val rawTags: Map<String, Any> = emptyMap()
)

/**
 * Robust Little-Endian NBT Parser for Minecraft Bedrock level.dat files.
 * Minecraft Bedrock level.dat uses an 8-byte header:
 * - 4 bytes: Little-endian integer (storage version, e.g. 8, 9, 10)
 * - 4 bytes: Little-endian integer (NBT data payload byte count)
 * Followed by Root Compound tag in Little-Endian byte order.
 */
class LittleEndianNbtParser {

    companion object {
        const val TAG_END: Byte = 0
        const val TAG_BYTE: Byte = 1
        const val TAG_SHORT: Byte = 2
        const val TAG_INT: Byte = 3
        const val TAG_LONG: Byte = 4
        const val TAG_FLOAT: Byte = 5
        const val TAG_DOUBLE: Byte = 6
        const val TAG_BYTE_ARRAY: Byte = 7
        const val TAG_STRING: Byte = 8
        const val TAG_LIST: Byte = 9
        const val TAG_COMPOUND: Byte = 10
        const val TAG_INT_ARRAY: Byte = 11
        const val TAG_LONG_ARRAY: Byte = 12
    }

    fun parse(file: File): BedrockLevelData {
        if (!file.exists() || file.length() < 8) {
            return BedrockLevelData()
        }
        return try {
            val bytes = file.readBytes()
            parseBytes(bytes)
        } catch (e: Exception) {
            e.printStackTrace()
            BedrockLevelData()
        }
    }

    fun parseBytes(bytes: ByteArray): BedrockLevelData {
        if (bytes.size < 8) return BedrockLevelData()

        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val headerVersion = buffer.int
        val nbtLength = buffer.int

        // If buffer has root compound
        val tags = mutableMapOf<String, Any>()
        try {
            // Find root TAG_COMPOUND (Tag 10)
            while (buffer.hasRemaining()) {
                val tagType = buffer.get()
                if (tagType == TAG_COMPOUND) {
                    val rootName = readString(buffer)
                    parseCompound(buffer, tags)
                    break
                }
            }
        } catch (e: Exception) {
            // Gracefully salvage parsed tags
        }

        val levelName = (tags["LevelName"] as? String)
            ?: (tags["level_name"] as? String)
            ?: "Bedrock World"

        val seed = when (val s = tags["RandomSeed"]) {
            is Long -> s
            is Number -> s.toLong()
            else -> 0L
        }

        val spawnX = (tags["SpawnX"] as? Number)?.toInt() ?: 0
        val spawnY = (tags["SpawnY"] as? Number)?.toInt() ?: 64
        val spawnZ = (tags["SpawnZ"] as? Number)?.toInt() ?: 0
        val gameType = (tags["GameType"] as? Number)?.toInt() ?: 0
        val difficulty = (tags["Difficulty"] as? Number)?.toInt() ?: 1
        val time = (tags["Time"] as? Number)?.toLong() ?: 0L
        val generator = (tags["Generator"] as? Number)?.toInt() ?: 1

        return BedrockLevelData(
            levelName = levelName,
            seed = seed,
            spawnX = spawnX,
            spawnY = spawnY,
            spawnZ = spawnZ,
            gameType = gameType,
            difficulty = difficulty,
            time = time,
            generator = generator,
            storageVersion = headerVersion,
            rawTags = tags
        )
    }

    private fun parseCompound(buffer: ByteBuffer, targetMap: MutableMap<String, Any>) {
        while (buffer.hasRemaining()) {
            val tagType = buffer.get()
            if (tagType == TAG_END) break

            val tagName = readString(buffer)
            val value = readTagPayload(buffer, tagType)
            if (value != null) {
                targetMap[tagName] = value
            }
        }
    }

    private fun readTagPayload(buffer: ByteBuffer, tagType: Byte): Any? {
        return when (tagType) {
            TAG_BYTE -> buffer.get()
            TAG_SHORT -> buffer.short
            TAG_INT -> buffer.int
            TAG_LONG -> buffer.long
            TAG_FLOAT -> buffer.float
            TAG_DOUBLE -> buffer.double
            TAG_BYTE_ARRAY -> {
                val len = buffer.int
                if (len < 0 || len > buffer.remaining()) return null
                val arr = ByteArray(len)
                buffer.get(arr)
                arr
            }
            TAG_STRING -> readString(buffer)
            TAG_LIST -> {
                val subType = buffer.get()
                val len = buffer.int
                val list = mutableListOf<Any>()
                for (i in 0 until len) {
                    val item = readTagPayload(buffer, subType)
                    if (item != null) list.add(item)
                }
                list
            }
            TAG_COMPOUND -> {
                val subMap = mutableMapOf<String, Any>()
                parseCompound(buffer, subMap)
                subMap
            }
            TAG_INT_ARRAY -> {
                val len = buffer.int
                if (len < 0 || len * 4 > buffer.remaining()) return null
                val arr = IntArray(len)
                for (i in 0 until len) arr[i] = buffer.int
                arr
            }
            TAG_LONG_ARRAY -> {
                val len = buffer.int
                if (len < 0 || len * 8 > buffer.remaining()) return null
                val arr = LongArray(len)
                for (i in 0 until len) arr[i] = buffer.long
                arr
            }
            else -> null
        }
    }

    private fun readString(buffer: ByteBuffer): String {
        val len = buffer.short.toInt() and 0xFFFF
        if (len <= 0 || len > buffer.remaining()) return ""
        val bytes = ByteArray(len)
        buffer.get(bytes)
        return String(bytes, Charsets.UTF_8)
    }

    /**
     * Serializes a minimal Bedrock level.dat for exported or generated worlds.
     */
    fun serializeMinimalLevelDat(
        levelName: String,
        seed: Long,
        spawnX: Int = 0,
        spawnY: Int = 64,
        spawnZ: Int = 0,
        gameType: Int = 0
    ): ByteArray {
        val nbtBuffer = ByteBuffer.allocate(4096).order(ByteOrder.LITTLE_ENDIAN)
        // Root Compound tag
        nbtBuffer.put(TAG_COMPOUND)
        writeString(nbtBuffer, "")

        // LevelName
        nbtBuffer.put(TAG_STRING)
        writeString(nbtBuffer, "LevelName")
        writeString(nbtBuffer, levelName)

        // RandomSeed
        nbtBuffer.put(TAG_LONG)
        writeString(nbtBuffer, "RandomSeed")
        nbtBuffer.putLong(seed)

        // SpawnX
        nbtBuffer.put(TAG_INT)
        writeString(nbtBuffer, "SpawnX")
        nbtBuffer.putInt(spawnX)

        // SpawnY
        nbtBuffer.put(TAG_INT)
        writeString(nbtBuffer, "SpawnY")
        nbtBuffer.putInt(spawnY)

        // SpawnZ
        nbtBuffer.put(TAG_INT)
        writeString(nbtBuffer, "SpawnZ")
        nbtBuffer.putInt(spawnZ)

        // GameType
        nbtBuffer.put(TAG_INT)
        writeString(nbtBuffer, "GameType")
        nbtBuffer.putInt(gameType)

        // Difficulty
        nbtBuffer.put(TAG_INT)
        writeString(nbtBuffer, "Difficulty")
        nbtBuffer.putInt(1)

        // StorageVersion
        nbtBuffer.put(TAG_INT)
        writeString(nbtBuffer, "StorageVersion")
        nbtBuffer.putInt(10)

        // End Compound
        nbtBuffer.put(TAG_END)

        val nbtSize = nbtBuffer.position()
        val totalBuffer = ByteBuffer.allocate(8 + nbtSize).order(ByteOrder.LITTLE_ENDIAN)
        totalBuffer.putInt(10) // storage version header
        totalBuffer.putInt(nbtSize) // nbt byte length
        totalBuffer.put(nbtBuffer.array(), 0, nbtSize)

        return totalBuffer.array()
    }

    private fun writeString(buffer: ByteBuffer, str: String) {
        val bytes = str.toByteArray(Charsets.UTF_8)
        buffer.putShort(bytes.size.toShort())
        buffer.put(bytes)
    }
}
