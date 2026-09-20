package com.example.data.storage

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.models.*
import com.example.data.nbt.LittleEndianNbtParser
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Storage manager for KG Bedrock Launcher.
 * Maintains an isolated workspace inside app-specific storage (`KGLauncher/`),
 * protecting Minecraft internal data while offering full import/export,
 * secure unzipping (Zip Slip prevention), and FileProvider sharing.
 */
class KGStorageManager(private val context: Context) {

    private val rootWorkspace: File = File(context.filesDir, "KGLauncher").apply {
        if (!exists()) mkdirs()
    }

    val rootDir: File get() = rootWorkspace

    val worldsDir: File = File(rootWorkspace, "worlds").apply { if (!exists()) mkdirs() }
    val skinsDir: File = File(rootWorkspace, "skins").apply { if (!exists()) mkdirs() }
    val packsDir: File = File(rootWorkspace, "packs").apply { if (!exists()) mkdirs() }
    val modelsDir: File = File(rootWorkspace, "models").apply { if (!exists()) mkdirs() }
    val replaysDir: File = File(rootWorkspace, "replays").apply { if (!exists()) mkdirs() }
    val backupsDir: File = File(rootWorkspace, "backups").apply { if (!exists()) mkdirs() }
    val screenshotsDir: File = File(rootWorkspace, "screenshots").apply { if (!exists()) mkdirs() }
    val exportsDir: File = File(rootWorkspace, "exports").apply { if (!exists()) mkdirs() }

    private val nbtParser = LittleEndianNbtParser()

    // ----------------------------------------------------
    // STORAGE STATS
    // ----------------------------------------------------

    fun getStorageUsageBytes(): Long {
        return calculateDirSize(rootWorkspace)
    }

    fun getWorkspaceStorageFormatted(): String {
        val bytes = getStorageUsageBytes()
        return when {
            bytes >= 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }

    private fun calculateDirSize(dir: File): Long {
        var size: Long = 0
        val files = dir.listFiles() ?: return 0L
        for (file in files) {
            size += if (file.isDirectory) calculateDirSize(file) else file.length()
        }
        return size
    }

    // ----------------------------------------------------
    // WORLD OPERATIONS
    // ----------------------------------------------------

    fun listWorlds(): List<WorldInfo> {
        val list = mutableListOf<WorldInfo>()
        val dirs = worldsDir.listFiles { f -> f.isDirectory } ?: return emptyList()

        for (dir in dirs) {
            val levelDat = File(dir, "level.dat")
            val levelNameFile = File(dir, "levelname.txt")
            val iconFile = File(dir, "world_icon.jpeg").takeIf { it.exists() }

            val parsedData = if (levelDat.exists()) nbtParser.parse(levelDat) else null
            val levelName = when {
                parsedData != null && parsedData.levelName.isNotBlank() -> parsedData.levelName
                levelNameFile.exists() -> levelNameFile.readText().trim()
                else -> dir.name
            }

            val size = calculateDirSize(dir)
            list.add(
                WorldInfo(
                    id = dir.name,
                    folderName = dir.name,
                    levelName = levelName,
                    seed = parsedData?.seed ?: 0L,
                    spawnX = parsedData?.spawnX ?: 0,
                    spawnY = parsedData?.spawnY ?: 64,
                    spawnZ = parsedData?.spawnZ ?: 0,
                    gameType = parsedData?.gameType ?: 0,
                    difficulty = parsedData?.difficulty ?: 1,
                    sizeBytes = size,
                    lastModified = dir.lastModified(),
                    iconFile = iconFile,
                    folderPath = dir,
                    generator = parsedData?.generator ?: 1,
                    time = parsedData?.time ?: 0L
                )
            )
        }
        return list.sortedByDescending { it.lastModified }
    }

    fun importWorld(uri: Uri): Result<WorldInfo> {
        return try {
            val worldId = "world_" + UUID.randomUUID().toString().take(8)
            val targetDir = File(worldsDir, worldId)
            if (!targetDir.exists()) targetDir.mkdirs()

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                unzipSecure(inputStream, targetDir)
            } ?: return Result.failure(IOException("Could not open input stream for URI: $uri"))

            // Verify if unzipped inside a nested folder
            val subFolders = targetDir.listFiles { f -> f.isDirectory }
            if (subFolders != null && subFolders.size == 1 && !File(targetDir, "level.dat").exists()) {
                val nestedDir = subFolders[0]
                if (File(nestedDir, "level.dat").exists()) {
                    nestedDir.listFiles()?.forEach { file ->
                        file.renameTo(File(targetDir, file.name))
                    }
                    nestedDir.delete()
                }
            }

            val levelDat = File(targetDir, "level.dat")
            val levelNameFile = File(targetDir, "levelname.txt")
            val iconFile = File(targetDir, "world_icon.jpeg").takeIf { it.exists() }
            val parsed = if (levelDat.exists()) nbtParser.parse(levelDat) else null
            val levelName = when {
                parsed != null && parsed.levelName.isNotBlank() -> parsed.levelName
                levelNameFile.exists() -> levelNameFile.readText().trim()
                else -> "Imported World"
            }

            val worldInfo = WorldInfo(
                id = targetDir.name,
                folderName = targetDir.name,
                levelName = levelName,
                seed = parsed?.seed ?: 0L,
                spawnX = parsed?.spawnX ?: 0,
                spawnY = parsed?.spawnY ?: 64,
                spawnZ = parsed?.spawnZ ?: 0,
                gameType = parsed?.gameType ?: 0,
                difficulty = parsed?.difficulty ?: 1,
                sizeBytes = calculateDirSize(targetDir),
                lastModified = targetDir.lastModified(),
                iconFile = iconFile,
                folderPath = targetDir
            )
            Result.success(worldInfo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun createSampleWorld(name: String, seed: Long, gameType: Int = 0): WorldInfo {
        val worldId = "world_" + UUID.randomUUID().toString().take(8)
        val targetDir = File(worldsDir, worldId).apply { mkdirs() }

        // Write levelname.txt
        File(targetDir, "levelname.txt").writeText(name)

        // Write level.dat
        val levelDatBytes = nbtParser.serializeMinimalLevelDat(
            levelName = name,
            seed = seed,
            spawnX = 0,
            spawnY = 64,
            spawnZ = 0,
            gameType = gameType
        )
        File(targetDir, "level.dat").writeBytes(levelDatBytes)

        // Create db folder
        File(targetDir, "db").mkdirs()

        return WorldInfo(
            id = worldId,
            folderName = worldId,
            levelName = name,
            seed = seed,
            spawnX = 0,
            spawnY = 64,
            spawnZ = 0,
            gameType = gameType,
            sizeBytes = calculateDirSize(targetDir),
            lastModified = targetDir.lastModified(),
            folderPath = targetDir
        )
    }

    fun exportWorld(world: WorldInfo): Result<File> {
        return try {
            val safeName = world.levelName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val exportFile = File(exportsDir, "${safeName}_${System.currentTimeMillis()}.mcworld")
            zipDirectory(world.folderPath, exportFile)
            Result.success(exportFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun backupWorld(world: WorldInfo): Result<File> {
        return try {
            val safeName = world.levelName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val backupFile = File(backupsDir, "${safeName}_backup_${System.currentTimeMillis()}.zip")
            zipDirectory(world.folderPath, backupFile)
            Result.success(backupFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun restoreWorldFromBackup(backupFile: File): Result<WorldInfo> {
        return try {
            val worldId = "world_restored_" + UUID.randomUUID().toString().take(8)
            val targetDir = File(worldsDir, worldId).apply { mkdirs() }
            backupFile.inputStream().use { inputStream ->
                unzipSecure(inputStream, targetDir)
            }
            val levelDat = File(targetDir, "level.dat")
            val parsed = if (levelDat.exists()) nbtParser.parse(levelDat) else null
            val levelName = parsed?.levelName ?: "Restored World"
            Result.success(
                WorldInfo(
                    id = worldId,
                    folderName = worldId,
                    levelName = levelName,
                    seed = parsed?.seed ?: 0L,
                    folderPath = targetDir,
                    sizeBytes = calculateDirSize(targetDir)
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun duplicateWorld(world: WorldInfo): Result<WorldInfo> {
        return try {
            val newId = "world_" + UUID.randomUUID().toString().take(8)
            val newDir = File(worldsDir, newId)
            world.folderPath.copyRecursively(newDir, overwrite = true)
            val newName = "${world.levelName} (Copy)"
            File(newDir, "levelname.txt").writeText(newName)

            val levelDat = File(newDir, "level.dat")
            if (levelDat.exists()) {
                val parsed = nbtParser.parse(levelDat)
                val newBytes = nbtParser.serializeMinimalLevelDat(
                    levelName = newName,
                    seed = parsed.seed,
                    spawnX = parsed.spawnX,
                    spawnY = parsed.spawnY,
                    spawnZ = parsed.spawnZ,
                    gameType = parsed.gameType
                )
                levelDat.writeBytes(newBytes)
            }

            Result.success(
                world.copy(
                    id = newId,
                    folderName = newId,
                    levelName = newName,
                    folderPath = newDir,
                    lastModified = System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun renameWorld(world: WorldInfo, newName: String): Result<WorldInfo> {
        return try {
            File(world.folderPath, "levelname.txt").writeText(newName)
            val levelDat = File(world.folderPath, "level.dat")
            if (levelDat.exists()) {
                val parsed = nbtParser.parse(levelDat)
                val newBytes = nbtParser.serializeMinimalLevelDat(
                    levelName = newName,
                    seed = parsed.seed,
                    spawnX = parsed.spawnX,
                    spawnY = parsed.spawnY,
                    spawnZ = parsed.spawnZ,
                    gameType = parsed.gameType
                )
                levelDat.writeBytes(newBytes)
            }
            Result.success(world.copy(levelName = newName, lastModified = System.currentTimeMillis()))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun deleteWorld(world: WorldInfo): Boolean {
        return world.folderPath.deleteRecursively()
    }

    // ----------------------------------------------------
    // SKIN OPERATIONS
    // ----------------------------------------------------

    fun listSkins(): List<SkinModel> {
        val files = skinsDir.listFiles { f -> f.isFile && f.name.endsWith(".png", ignoreCase = true) }
            ?: return emptyList()
        return files.map { file ->
            val name = file.nameWithoutExtension
            val isAlex = name.contains("alex", ignoreCase = true) || name.contains("slim", ignoreCase = true)
            SkinModel(
                id = file.name,
                name = name,
                file = file,
                isAlex = isAlex,
                sizeBytes = file.length(),
                lastModified = file.lastModified()
            )
        }.sortedByDescending { it.lastModified }
    }

    fun importSkin(uri: Uri, isAlex: Boolean = false): Result<SkinModel> {
        return try {
            val fileName = "skin_${System.currentTimeMillis()}${if (isAlex) "_alex" else "_steve"}.png"
            val targetFile = File(skinsDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return Result.failure(IOException("Could not open skin URI"))

            Result.success(
                SkinModel(
                    id = targetFile.name,
                    name = targetFile.nameWithoutExtension,
                    file = targetFile,
                    isAlex = isAlex,
                    sizeBytes = targetFile.length(),
                    lastModified = targetFile.lastModified()
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun equipSkinExport(skin: SkinModel): File {
        val exportsSkinDir = File(exportsDir, "skins").apply { if (!exists()) mkdirs() }
        val target = File(exportsSkinDir, "${skin.name}.png")
        skin.file.copyTo(target, overwrite = true)
        return target
    }

    fun deleteSkin(skin: SkinModel): Boolean {
        return skin.file.delete()
    }

    fun renameSkin(skin: SkinModel, newName: String): Result<SkinModel> {
        return try {
            val target = File(skinsDir, "$newName.png")
            if (skin.file.renameTo(target)) {
                Result.success(skin.copy(id = target.name, name = newName, file = target))
            } else {
                Result.failure(IOException("Rename failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ----------------------------------------------------
    // RESOURCE / BEHAVIOR PACK OPERATIONS
    // ----------------------------------------------------

    fun listPacks(): List<PackInfo> {
        val dirs = packsDir.listFiles { f -> f.isDirectory } ?: return emptyList()
        val list = mutableListOf<PackInfo>()

        for (dir in dirs) {
            val manifestFile = File(dir, "manifest.json")
            var name = dir.name
            var description = ""
            var uuid = ""
            var version = "1.0.0"
            var formatVersion = 2
            var isBehavior = false

            if (manifestFile.exists()) {
                try {
                    val json = JSONObject(manifestFile.readText())
                    formatVersion = json.optInt("format_version", 2)
                    val header = json.optJSONObject("header")
                    if (header != null) {
                        name = header.optString("name", name)
                        description = header.optString("description", "")
                        uuid = header.optString("uuid", "")
                        val verArr = header.optJSONArray("version")
                        if (verArr != null) {
                            version = "${verArr.optInt(0, 1)}.${verArr.optInt(1, 0)}.${verArr.optInt(2, 0)}"
                        }
                    }
                    val modules = json.optJSONArray("modules")
                    if (modules != null) {
                        for (i in 0 until modules.length()) {
                            val m = modules.optJSONObject(i)
                            if (m?.optString("type") == "data" || m?.optString("type") == "script") {
                                isBehavior = true
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Fallback to directory metadata
                }
            }

            val iconFile = File(dir, "pack_icon.png").takeIf { it.exists() }
            val disabledMarker = File(dir, ".disabled")

            list.add(
                PackInfo(
                    id = dir.name,
                    name = name,
                    description = description,
                    uuid = uuid,
                    version = version,
                    formatVersion = formatVersion,
                    packType = if (isBehavior) PackType.BEHAVIOR else PackType.RESOURCE,
                    isEnabled = !disabledMarker.exists(),
                    folder = dir,
                    iconFile = iconFile,
                    sizeBytes = calculateDirSize(dir)
                )
            )
        }
        return list.sortedBy { it.name }
    }

    fun importPack(uri: Uri, isResource: Boolean = true): Result<PackInfo> {
        return try {
            val packId = "pack_" + UUID.randomUUID().toString().take(8)
            val targetDir = File(packsDir, packId).apply { mkdirs() }

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                unzipSecure(inputStream, targetDir)
            } ?: return Result.failure(IOException("Could not open pack stream"))

            val manifestFile = File(targetDir, "manifest.json")
            var name = "Imported Pack"
            var desc = ""
            var uuid = UUID.randomUUID().toString()
            var version = "1.0.0"
            var isBehavior = !isResource

            if (manifestFile.exists()) {
                val json = JSONObject(manifestFile.readText())
                val header = json.optJSONObject("header")
                if (header != null) {
                    name = header.optString("name", name)
                    desc = header.optString("description", "")
                    uuid = header.optString("uuid", uuid)
                }
            }

            Result.success(
                PackInfo(
                    id = packId,
                    name = name,
                    description = desc,
                    uuid = uuid,
                    version = version,
                    formatVersion = 2,
                    packType = if (isBehavior) PackType.BEHAVIOR else PackType.RESOURCE,
                    folder = targetDir,
                    sizeBytes = calculateDirSize(targetDir)
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun togglePackEnabled(pack: PackInfo): PackInfo {
        val marker = File(pack.folder, ".disabled")
        if (pack.isEnabled) {
            marker.createNewFile()
        } else {
            marker.delete()
        }
        return pack.copy(isEnabled = !pack.isEnabled)
    }

    fun deletePack(pack: PackInfo): Boolean {
        return pack.folder.deleteRecursively()
    }

    // ----------------------------------------------------
    // CUSTOM MODEL CREATOR OPERATIONS
    // ----------------------------------------------------

    fun listModels(): List<CustomModelProject> {
        val dirs = modelsDir.listFiles { f -> f.isDirectory } ?: return emptyList()
        val list = mutableListOf<CustomModelProject>()

        for (dir in dirs) {
            val projectFile = File(dir, "project.json")
            val modelJsonFile = File(dir, "model/model.json")
            var name = dir.name
            var geoId = "geometry.custom_model"
            var bones = emptyList<ModelBone>()

            if (projectFile.exists()) {
                try {
                    val pJson = JSONObject(projectFile.readText())
                    name = pJson.optString("name", name)
                    geoId = pJson.optString("geometry_identifier", geoId)
                } catch (e: Exception) {}
            }

            if (modelJsonFile.exists()) {
                try {
                    bones = parseBedrockGeometryBones(modelJsonFile.readText())
                } catch (e: Exception) {}
            }

            list.add(
                CustomModelProject(
                    id = dir.name,
                    name = name,
                    geometryIdentifier = geoId,
                    bones = bones,
                    projectFolder = dir,
                    lastModified = dir.lastModified()
                )
            )
        }
        return list.sortedByDescending { it.lastModified }
    }

    fun createNewModelProject(name: String, geometryId: String = "geometry.custom_model"): CustomModelProject {
        val projId = "model_" + UUID.randomUUID().toString().take(8)
        val projDir = File(modelsDir, projId).apply { mkdirs() }

        File(projDir, "model").mkdirs()
        File(projDir, "textures").mkdirs()
        File(projDir, "animations").mkdirs()
        File(projDir, "metadata").mkdirs()
        File(projDir, "previews").mkdirs()

        // Default initial bones: "root" and "body" with a default cube
        val defaultBones = listOf(
            ModelBone(
                name = "root",
                pivot = floatArrayOf(0f, 0f, 0f),
                cubes = listOf(
                    ModelCube(
                        origin = floatArrayOf(-4f, 0f, -4f),
                        size = floatArrayOf(8f, 8f, 8f),
                        uv = floatArrayOf(0f, 0f)
                    )
                )
            )
        )

        val project = CustomModelProject(
            id = projId,
            name = name,
            geometryIdentifier = geometryId,
            bones = defaultBones,
            projectFolder = projDir,
            lastModified = System.currentTimeMillis()
        )

        saveModelProject(project)
        return project
    }

    fun saveModelProject(project: CustomModelProject): Result<Unit> {
        return try {
            val dir = project.projectFolder ?: File(modelsDir, project.id).apply { mkdirs() }
            File(dir, "model").mkdirs()
            File(dir, "animations").mkdirs()
            File(dir, "metadata").mkdirs()

            // 1. project.json
            val pJson = JSONObject().apply {
                put("id", project.id)
                put("name", project.name)
                put("geometry_identifier", project.geometryIdentifier)
                put("texture_width", project.textureWidth)
                put("texture_height", project.textureHeight)
                put("last_modified", System.currentTimeMillis())
            }
            File(dir, "project.json").writeText(pJson.toString(2))

            // 2. model/model.json in Minecraft Bedrock format_version: "1.12.0"
            val bedrockModelJson = generateBedrockGeometryJson(project)
            File(dir, "model/model.json").writeText(bedrockModelJson)

            // 3. animations/animation.json
            val animJson = generateBedrockAnimationJson(project.animations)
            File(dir, "animations/animation.json").writeText(animJson)

            // 4. metadata/metadata.json
            val metaJson = JSONObject().apply {
                put("app", "KG Bedrock Launcher")
                put("spec", "Minecraft Bedrock Format 1.12.0")
                put("export_ready", true)
            }
            File(dir, "metadata/metadata.json").writeText(metaJson.toString(2))

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun exportModelZip(project: CustomModelProject): Result<File> {
        return try {
            saveModelProject(project)
            val dir = project.projectFolder ?: File(modelsDir, project.id)
            val safeName = project.name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val exportFile = File(exportsDir, "${safeName}.kgmodel.zip")
            zipDirectory(dir, exportFile)
            Result.success(exportFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun importModelProject(uri: Uri): Result<CustomModelProject> {
        return try {
            val projId = "model_" + UUID.randomUUID().toString().take(8)
            val targetDir = File(modelsDir, projId).apply { mkdirs() }

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                unzipSecure(inputStream, targetDir)
            } ?: return Result.failure(IOException("Could not open model stream"))

            val projectJsonFile = File(targetDir, "project.json")
            val modelJsonFile = File(targetDir, "model/model.json")
            var name = "Imported Model"
            var geoId = "geometry.imported"

            if (projectJsonFile.exists()) {
                val p = JSONObject(projectJsonFile.readText())
                name = p.optString("name", name)
                geoId = p.optString("geometry_identifier", geoId)
            }

            val bones = if (modelJsonFile.exists()) {
                parseBedrockGeometryBones(modelJsonFile.readText())
            } else emptyList()

            val proj = CustomModelProject(
                id = projId,
                name = name,
                geometryIdentifier = geoId,
                bones = bones,
                projectFolder = targetDir
            )
            Result.success(proj)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun deleteModel(project: CustomModelProject): Boolean {
        return project.projectFolder?.deleteRecursively() ?: false
    }

    // ----------------------------------------------------
    // REPLAY COMPANION SYSTEM
    // ----------------------------------------------------

    fun listReplays(): List<ReplayProject> {
        val files = replaysDir.listFiles { f -> f.isFile && f.name.endsWith(".json") } ?: return emptyList()
        val list = mutableListOf<ReplayProject>()

        for (file in files) {
            try {
                val json = JSONObject(file.readText())
                val id = json.optString("id", file.nameWithoutExtension)
                val name = json.optString("name", json.optString("title", "Replay"))
                val isLoop = json.optBoolean("isLoop", false)
                val keyframesArr = json.optJSONArray("keyframes")
                val keyframes = mutableListOf<CameraKeyframe>()

                if (keyframesArr != null) {
                    for (i in 0 until keyframesArr.length()) {
                        val kObj = keyframesArr.optJSONObject(i) ?: continue
                        keyframes.add(
                            CameraKeyframe(
                                id = kObj.optString("id", UUID.randomUUID().toString()),
                                tick = kObj.optLong("tick", (i * 20).toLong()),
                                x = kObj.optDouble("x", kObj.optDouble("posX", 0.0)).toFloat(),
                                y = kObj.optDouble("y", kObj.optDouble("posY", 64.0)).toFloat(),
                                z = kObj.optDouble("z", kObj.optDouble("posZ", 0.0)).toFloat(),
                                pitch = kObj.optDouble("pitch", 0.0).toFloat(),
                                yaw = kObj.optDouble("yaw", 0.0).toFloat(),
                                transitionDurationSeconds = kObj.optDouble("duration", 1.0).toFloat()
                            )
                        )
                    }
                }

                list.add(
                    ReplayProject(
                        id = id,
                        name = name,
                        keyframes = keyframes,
                        isLoop = isLoop,
                        createdTime = file.lastModified()
                    )
                )
            } catch (e: Exception) {}
        }
        return list.sortedByDescending { it.createdTime }
    }

    fun saveReplay(project: ReplayProject): Result<File> {
        return try {
            val file = File(replaysDir, "${project.id}.json")
            val json = JSONObject().apply {
                put("id", project.id)
                put("name", project.name)
                put("isLoop", project.isLoop)
                put("createdTime", project.createdTime)

                val arr = JSONArray()
                project.keyframes.forEach { k ->
                    val kObj = JSONObject().apply {
                        put("id", k.id)
                        put("tick", k.tick)
                        put("x", k.x)
                        put("y", k.y)
                        put("z", k.z)
                        put("pitch", k.pitch)
                        put("yaw", k.yaw)
                        put("duration", k.transitionDurationSeconds)
                    }
                    arr.put(kObj)
                }
                put("keyframes", arr)
            }
            file.writeText(json.toString(2))
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun exportReplayMcfunction(project: ReplayProject): File {
        val safeName = project.name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val exportFile = File(exportsDir, "${safeName}_replay.mcfunction")
        val sb = StringBuilder()
        sb.append("# KG Bedrock Launcher - Replay Cinematic Path\n")
        sb.append("# Title: ${project.name}\n\n")

        for ((index, kf) in project.keyframes.withIndex()) {
            sb.append("# Keyframe $index (tick: ${kf.tick})\n")
            sb.append(kf.toBedrockCommand())
            sb.append("\n")
        }

        exportFile.writeText(sb.toString())
        return exportFile
    }

    // ----------------------------------------------------
    // UNIVERSAL FILE SHARING
    // ----------------------------------------------------

    fun getShareableUri(file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun getFileUri(file: File): Uri = getShareableUri(file)

    fun exportSkinForEquip(skin: SkinModel): File? = equipSkinExport(skin)

    fun exportReplayProject(replay: ReplayProject): Result<File> {
        return try {
            val file = exportReplayMcfunction(replay)
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun backupAllWorkspace(): Result<File> {
        return try {
            val destZip = File(exportsDir, "kg_backup_all.zip")
            zipDirectory(rootWorkspace, destZip)
            Result.success(destZip)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun clearCache() {
        try {
            context.cacheDir.deleteRecursively()
        } catch (e: Exception) {}
    }

    // ----------------------------------------------------
    // HELPER METHODS: BEDROCK GEOMETRY JSON & ZIP SLIP FIX
    // ----------------------------------------------------

    private fun generateBedrockGeometryJson(project: CustomModelProject): String {
        val root = JSONObject()
        root.put("format_version", "1.12.0")

        val geoArray = JSONArray()
        val geoObj = JSONObject().apply {
            val desc = JSONObject().apply {
                put("identifier", project.geometryIdentifier)
                put("texture_width", project.textureWidth)
                put("texture_height", project.textureHeight)
                put("visible_bounds_width", 2)
                put("visible_bounds_height", 2)
                put("visible_bounds_offset", JSONArray(listOf(0, 1, 0)))
            }
            put("description", desc)

            val bonesArr = JSONArray()
            project.bones.forEach { bone ->
                val boneObj = JSONObject().apply {
                    put("name", bone.name)
                    bone.parent?.let { put("parent", it) }
                    put("pivot", JSONArray(listOf(bone.pivot[0], bone.pivot[1], bone.pivot[2])))
                    put("rotation", JSONArray(listOf(bone.rotation[0], bone.rotation[1], bone.rotation[2])))

                    val cubesArr = JSONArray()
                    bone.cubes.forEach { cube ->
                        val cubeObj = JSONObject().apply {
                            put("origin", JSONArray(listOf(cube.origin[0], cube.origin[1], cube.origin[2])))
                            put("size", JSONArray(listOf(cube.size[0], cube.size[1], cube.size[2])))
                            put("uv", JSONArray(listOf(cube.uv[0], cube.uv[1])))
                            if (cube.inflate != 0f) put("inflate", cube.inflate)
                            if (cube.mirror) put("mirror", true)
                        }
                        cubesArr.put(cubeObj)
                    }
                    put("cubes", cubesArr)
                }
                bonesArr.put(boneObj)
            }
            put("bones", bonesArr)
        }
        geoArray.put(geoObj)
        root.put("minecraft:geometry", geoArray)

        return root.toString(2)
    }

    private fun generateBedrockAnimationJson(animations: List<ModelAnimation>): String {
        val root = JSONObject()
        root.put("format_version", "1.8.0")
        val animsObj = JSONObject()

        animations.forEach { anim ->
            val animData = JSONObject().apply {
                put("loop", anim.loop)
                put("animation_length", anim.animationLength)
                val bonesObj = JSONObject()
                anim.keyframes.groupBy { it.boneName }.forEach { (boneName, kfs) ->
                    val boneData = JSONObject()
                    val rotMap = JSONObject()
                    val posMap = JSONObject()
                    kfs.forEach { kf ->
                        val timeStr = String.format("%.2f", kf.time)
                        rotMap.put(timeStr, JSONArray(listOf(kf.rotationOffset[0], kf.rotationOffset[1], kf.rotationOffset[2])))
                        posMap.put(timeStr, JSONArray(listOf(kf.positionOffset[0], kf.positionOffset[1], kf.positionOffset[2])))
                    }
                    boneData.put("rotation", rotMap)
                    boneData.put("position", posMap)
                    bonesObj.put(boneName, boneData)
                }
                put("bones", bonesObj)
            }
            animsObj.put(anim.name, animData)
        }
        root.put("animations", animsObj)
        return root.toString(2)
    }

    private fun parseBedrockGeometryBones(jsonString: String): List<ModelBone> {
        val list = mutableListOf<ModelBone>()
        val json = JSONObject(jsonString)
        val geometries = json.optJSONArray("minecraft:geometry") ?: return list
        if (geometries.length() == 0) return list
        val firstGeo = geometries.getJSONObject(0)
        val bonesArr = firstGeo.optJSONArray("bones") ?: return list

        for (i in 0 until bonesArr.length()) {
            val bObj = bonesArr.getJSONObject(i)
            val name = bObj.optString("name", "bone_$i")
            val parent = if (bObj.has("parent")) bObj.getString("parent") else null

            val pivotArr = bObj.optJSONArray("pivot")
            val pivot = floatArrayOf(
                pivotArr?.optDouble(0, 0.0)?.toFloat() ?: 0f,
                pivotArr?.optDouble(1, 0.0)?.toFloat() ?: 0f,
                pivotArr?.optDouble(2, 0.0)?.toFloat() ?: 0f
            )

            val rotArr = bObj.optJSONArray("rotation")
            val rotation = floatArrayOf(
                rotArr?.optDouble(0, 0.0)?.toFloat() ?: 0f,
                rotArr?.optDouble(1, 0.0)?.toFloat() ?: 0f,
                rotArr?.optDouble(2, 0.0)?.toFloat() ?: 0f
            )

            val cubesArr = bObj.optJSONArray("cubes")
            val cubes = mutableListOf<ModelCube>()
            if (cubesArr != null) {
                for (j in 0 until cubesArr.length()) {
                    val cObj = cubesArr.getJSONObject(j)
                    val origArr = cObj.optJSONArray("origin")
                    val szArr = cObj.optJSONArray("size")
                    val uvArr = cObj.optJSONArray("uv")

                    cubes.add(
                        ModelCube(
                            origin = floatArrayOf(
                                origArr?.optDouble(0, 0.0)?.toFloat() ?: 0f,
                                origArr?.optDouble(1, 0.0)?.toFloat() ?: 0f,
                                origArr?.optDouble(2, 0.0)?.toFloat() ?: 0f
                            ),
                            size = floatArrayOf(
                                szArr?.optDouble(0, 1.0)?.toFloat() ?: 1f,
                                szArr?.optDouble(1, 1.0)?.toFloat() ?: 1f,
                                szArr?.optDouble(2, 1.0)?.toFloat() ?: 1f
                            ),
                            uv = floatArrayOf(
                                uvArr?.optDouble(0, 0.0)?.toFloat() ?: 0f,
                                uvArr?.optDouble(1, 0.0)?.toFloat() ?: 0f
                            ),
                            inflate = cObj.optDouble("inflate", 0.0).toFloat(),
                            mirror = cObj.optBoolean("mirror", false)
                        )
                    )
                }
            }
            list.add(ModelBone(name = name, parent = parent, pivot = pivot, rotation = rotation, cubes = cubes))
        }
        return list
    }

    /**
     * Zip Slip Vulnerability Protection:
     * Validates that every extracted file's canonical path strictly starts with
     * the canonical path of the target directory.
     */
    fun unzipSecure(inputStream: InputStream, targetDir: File) {
        val canonicalTargetDir = targetDir.canonicalPath
        ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                val newFile = File(targetDir, entry.name)
                val canonicalDest = newFile.canonicalPath

                if (!canonicalDest.startsWith(canonicalTargetDir + File.separator) && canonicalDest != canonicalTargetDir) {
                    throw SecurityException("Zip Slip Vulnerability detected in zip entry: ${entry.name}")
                }

                if (entry.isDirectory) {
                    newFile.mkdirs()
                } else {
                    newFile.parentFile?.mkdirs()
                    FileOutputStream(newFile).use { fos ->
                        zis.copyTo(fos)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    private fun zipDirectory(dirToZip: File, zipFile: File) {
        zipFile.parentFile?.mkdirs()
        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
            zipDirRecursive(dirToZip, dirToZip, zos)
        }
    }

    private fun zipDirRecursive(root: File, current: File, zos: ZipOutputStream) {
        val files = current.listFiles() ?: return
        for (file in files) {
            val relativePath = root.toURI().relativize(file.toURI()).path
            if (file.isDirectory) {
                val dirEntry = ZipEntry(if (relativePath.endsWith("/")) relativePath else "$relativePath/")
                zos.putNextEntry(dirEntry)
                zos.closeEntry()
                zipDirRecursive(root, file, zos)
            } else {
                val fileEntry = ZipEntry(relativePath)
                zos.putNextEntry(fileEntry)
                FileInputStream(file).use { fis ->
                    fis.copyTo(zos)
                }
                zos.closeEntry()
            }
        }
    }
}
