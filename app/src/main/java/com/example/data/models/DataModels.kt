package com.example.data.models

import java.io.File
import java.util.UUID

/**
 * Representation of a Minecraft Bedrock World within the isolated KG Workspace.
 */
data class WorldInfo(
    val id: String,
    val folderName: String,
    val levelName: String,
    val seed: Long = 0L,
    val spawnX: Int = 0,
    val spawnY: Int = 64,
    val spawnZ: Int = 0,
    val gameType: Int = 0, // 0: Survival, 1: Creative, 2: Adventure
    val difficulty: Int = 1,
    val sizeBytes: Long = 0L,
    val lastModified: Long = System.currentTimeMillis(),
    val iconFile: File? = null,
    val folderPath: File,
    val generator: Int = 1,
    val time: Long = 0L
) {
    val gameTypeLabel: String
        get() = when (gameType) {
            1 -> "Creative"
            2 -> "Adventure"
            else -> "Survival"
        }

    val formattedSize: String
        get() = when {
            sizeBytes >= 1024 * 1024 -> String.format("%.2f MB", sizeBytes / (1024.0 * 1024.0))
            sizeBytes >= 1024 -> String.format("%.1f KB", sizeBytes / 1024.0)
            else -> "$sizeBytes B"
        }
}

/**
 * Skin data structure for 64x64 or 64x32 Minecraft skins.
 */
data class SkinModel(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Skin",
    val file: File,
    val isAlex: Boolean = false, // slim 3px arms
    val sizeBytes: Long = 0L,
    val lastModified: Long = System.currentTimeMillis()
)

/**
 * Custom 3D Model data structures for Bedrock Geometry (format_version: "1.12.0").
 */
data class CustomModelProject(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Custom Bedrock Entity",
    val geometryIdentifier: String = "geometry.custom_model",
    val bones: List<ModelBone> = emptyList(),
    val textureWidth: Int = 64,
    val textureHeight: Int = 64,
    val projectFolder: File? = null,
    val animations: List<ModelAnimation> = emptyList(),
    val lastModified: Long = System.currentTimeMillis()
)

data class ModelBone(
    val name: String,
    val parent: String? = null,
    val pivot: FloatArray = floatArrayOf(0f, 0f, 0f),
    val rotation: FloatArray = floatArrayOf(0f, 0f, 0f),
    val cubes: List<ModelCube> = emptyList()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ModelBone) return false
        return name == other.name && parent == other.parent && cubes == other.cubes
    }

    override fun hashCode(): Int = name.hashCode()
}

data class ModelCube(
    val id: String = UUID.randomUUID().toString(),
    val origin: FloatArray = floatArrayOf(-4f, 0f, -2f),
    val size: FloatArray = floatArrayOf(8f, 8f, 4f),
    val uv: FloatArray = floatArrayOf(0f, 0f), // UV origin in texture
    val inflate: Float = 0f,
    val mirror: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ModelCube) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

data class ModelAnimation(
    val name: String = "animation.custom_model.idle",
    val loop: Boolean = true,
    val animationLength: Float = 2.0f,
    val keyframes: List<AnimationKeyframe> = emptyList()
)

data class AnimationKeyframe(
    val time: Float = 0f,
    val boneName: String,
    val positionOffset: FloatArray = floatArrayOf(0f, 0f, 0f),
    val rotationOffset: FloatArray = floatArrayOf(0f, 0f, 0f),
    val scaleOffset: FloatArray = floatArrayOf(1f, 1f, 1f)
)

/**
 * Honest Replay Companion format keyframe (camera trajectory planner).
 */
data class CameraKeyframe(
    val id: String = UUID.randomUUID().toString(),
    val tick: Long = 0L,
    val x: Float = 0f,
    val y: Float = 64f,
    val z: Float = 0f,
    val pitch: Float = 0f,
    val yaw: Float = 0f,
    val transitionDurationSeconds: Float = 1.0f
) {
    fun toBedrockCommand(): String {
        return "/camera @s set minecraft:free pos ${String.format("%.2f", x)} ${String.format("%.2f", y)} ${String.format("%.2f", z)} rot ${String.format("%.1f", pitch)} ${String.format("%.1f", yaw)}"
    }
}

data class ReplayProject(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Replay Path",
    val keyframes: List<CameraKeyframe> = emptyList(),
    val isLoop: Boolean = false,
    val createdTime: Long = System.currentTimeMillis()
) {
    fun generateBedrockCommands(): List<String> {
        return keyframes.map { it.toBedrockCommand() }
    }
}

enum class PackType {
    RESOURCE,
    BEHAVIOR
}

/**
 * Bedrock Resource / Behavior Pack Info.
 */
data class PackInfo(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val uuid: String = UUID.randomUUID().toString(),
    val version: String = "1.0.0",
    val minEngineVersion: String = "1.20.0",
    val formatVersion: Int = 2,
    val packType: PackType = PackType.RESOURCE,
    val isEnabled: Boolean = true,
    val folder: File,
    val iconFile: File? = null,
    val sizeBytes: Long = 0L
) {
    val filePath: String get() = folder.absolutePath
}

enum class LauncherThemeOption {
    CYBER_BLUE,
    OBSIDIAN_DARK,
    EMERALD_GREEN,
    REDSTONE_CRIMSON
}

data class LauncherSettings(
    val theme: LauncherThemeOption = LauncherThemeOption.CYBER_BLUE,
    val isLowPerformanceMode: Boolean = false,
    val workspacePath: String = "KGLauncher"
)
