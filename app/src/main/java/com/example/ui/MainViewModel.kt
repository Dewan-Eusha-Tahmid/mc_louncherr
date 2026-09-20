package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.launcher.MinecraftDetector
import com.example.data.launcher.MinecraftStatus
import com.example.data.models.*
import com.example.data.speech.KGSpeechManager
import com.example.data.storage.KGStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class LauncherUiState(
    val currentRoute: String = "dashboard",
    val minecraftStatus: MinecraftStatus = MinecraftStatus(isInstalled = false),
    val worlds: List<WorldInfo> = emptyList(),
    val skins: List<SkinModel> = emptyList(),
    val packs: List<PackInfo> = emptyList(),
    val models: List<CustomModelProject> = emptyList(),
    val replays: List<ReplayProject> = emptyList(),
    val settings: LauncherSettings = LauncherSettings(),
    val storageUsageText: String = "0 KB",
    val activeWorld: WorldInfo? = null,
    val activeModel: CustomModelProject = CustomModelProject(id = "model_active", name = "Default Entity"),
    val activeReplay: ReplayProject = ReplayProject(id = "replay_active", name = "Default Replay")
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val storageManager = KGStorageManager(application)
    val minecraftDetector = MinecraftDetector(application)
    val speechManager = KGSpeechManager(application)

    private val _uiState = MutableStateFlow(
        LauncherUiState(
            settings = LauncherSettings(workspacePath = storageManager.rootDir.absolutePath)
        )
    )
    val uiState: StateFlow<LauncherUiState> = _uiState.asStateFlow()

    init {
        refreshAll()
    }

    fun refreshAll() {
        viewModelScope.launch(Dispatchers.IO) {
            val status = minecraftDetector.detectMinecraft()
            val worlds = storageManager.listWorlds()
            val skins = storageManager.listSkins()
            val packs = storageManager.listPacks()
            val usage = storageManager.getWorkspaceStorageFormatted()

            // Initialize default sample model & replay if empty
            val defaultModel = CustomModelProject(
                id = "model_default",
                name = "Custom Bedrock Entity",
                bones = listOf(
                    ModelBone("root", pivot = floatArrayOf(0f, 0f, 0f), cubes = listOf(ModelCube(origin = floatArrayOf(-4f, 0f, -4f), size = floatArrayOf(8f, 8f, 8f)))),
                    ModelBone("head", parent = "root", pivot = floatArrayOf(0f, 8f, 0f), cubes = listOf(ModelCube(origin = floatArrayOf(-3f, 8f, -3f), size = floatArrayOf(6f, 6f, 6f))))
                )
            )

            val defaultReplay = ReplayProject(
                id = "replay_default",
                name = "Cinematic Intro",
                keyframes = listOf(
                    CameraKeyframe(tick = 0L, x = 0f, y = 72f, z = 0f, pitch = 10f, yaw = 0f),
                    CameraKeyframe(tick = 20L, x = 15f, y = 80f, z = 25f, pitch = 20f, yaw = 45f),
                    CameraKeyframe(tick = 40L, x = 30f, y = 75f, z = 40f, pitch = 15f, yaw = 90f)
                )
            )

            _uiState.value = _uiState.value.copy(
                minecraftStatus = status,
                worlds = worlds,
                skins = skins,
                packs = packs,
                models = listOf(defaultModel),
                replays = listOf(defaultReplay),
                storageUsageText = usage,
                activeModel = defaultModel,
                activeReplay = defaultReplay
            )
        }
    }

    fun navigateTo(route: String) {
        _uiState.value = _uiState.value.copy(currentRoute = route)
    }

    fun openWorldExplorer(world: WorldInfo) {
        _uiState.value = _uiState.value.copy(activeWorld = world, currentRoute = "world_explorer")
    }

    fun openCaveViewer(world: WorldInfo) {
        _uiState.value = _uiState.value.copy(activeWorld = world, currentRoute = "cave_viewer")
    }

    fun updateSettings(newSettings: LauncherSettings) {
        _uiState.value = _uiState.value.copy(settings = newSettings)
    }

    // World operations
    fun importWorld(uri: Uri, onSuccess: (WorldInfo) -> Unit, onError: (Exception) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = storageManager.importWorld(uri)
            result.onSuccess { world ->
                refreshAll()
                launch(Dispatchers.Main) { onSuccess(world) }
            }.onFailure { err ->
                launch(Dispatchers.Main) { onError(err as? Exception ?: Exception(err.message)) }
            }
        }
    }

    fun createSampleWorld(name: String, seed: Long, gameType: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            storageManager.createSampleWorld(name, seed, gameType)
            refreshAll()
        }
    }

    fun duplicateWorld(world: WorldInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            storageManager.duplicateWorld(world)
            refreshAll()
        }
    }

    fun renameWorld(world: WorldInfo, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            storageManager.renameWorld(world, newName)
            refreshAll()
        }
    }

    fun deleteWorld(world: WorldInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            storageManager.deleteWorld(world)
            refreshAll()
        }
    }

    fun backupWorld(world: WorldInfo, onDone: (File) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = storageManager.backupWorld(world)
            res.onSuccess { file ->
                launch(Dispatchers.Main) { onDone(file) }
            }
        }
    }

    fun exportWorld(world: WorldInfo, onDone: (File) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = storageManager.exportWorld(world)
            res.onSuccess { file ->
                launch(Dispatchers.Main) { onDone(file) }
            }
        }
    }

    // Skin operations
    fun importSkin(uri: Uri, isAlex: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            storageManager.importSkin(uri, isAlex)
            refreshAll()
        }
    }

    fun deleteSkin(skin: SkinModel) {
        viewModelScope.launch(Dispatchers.IO) {
            storageManager.deleteSkin(skin)
            refreshAll()
        }
    }

    fun renameSkin(skin: SkinModel, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            storageManager.renameSkin(skin, newName)
            refreshAll()
        }
    }

    fun equipSkin(skin: SkinModel): File? {
        return storageManager.exportSkinForEquip(skin)
    }

    // Pack operations
    fun importPack(uri: Uri, isResource: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            storageManager.importPack(uri, isResource)
            refreshAll()
        }
    }

    fun deletePack(pack: PackInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            File(pack.filePath).deleteRecursively()
            refreshAll()
        }
    }

    fun togglePackEnabled(pack: PackInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = _uiState.value.packs.map {
                if (it.id == pack.id) it.copy(isEnabled = !it.isEnabled) else it
            }
            _uiState.value = _uiState.value.copy(packs = updated)
        }
    }

    // Model operations
    fun saveModel(project: CustomModelProject) {
        viewModelScope.launch(Dispatchers.IO) {
            storageManager.saveModelProject(project)
            refreshAll()
        }
    }

    fun exportModelZip(project: CustomModelProject, onDone: (File) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = storageManager.exportModelZip(project)
            res.onSuccess { file ->
                launch(Dispatchers.Main) { onDone(file) }
            }
        }
    }

    // Replay operations
    fun exportReplay(replay: ReplayProject, onDone: (File) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = storageManager.exportReplayProject(replay)
            res.onSuccess { file ->
                launch(Dispatchers.Main) { onDone(file) }
            }
        }
    }

    fun backupAllWorkspace(onDone: (File) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = storageManager.backupAllWorkspace()
            res.onSuccess { file ->
                launch(Dispatchers.Main) { onDone(file) }
            }
        }
    }

    fun clearCache() {
        viewModelScope.launch(Dispatchers.IO) {
            storageManager.clearCache()
            refreshAll()
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.destroy()
    }
}
