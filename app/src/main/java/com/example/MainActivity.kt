package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.models.WorldInfo
import com.example.ui.MainViewModel
import com.example.ui.screens.*
import com.example.ui.theme.BedrockBlack
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ObsidianSurface
import java.io.File

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: MainViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            // SAF Activity Result Launchers
            val importWorldLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri: Uri? ->
                if (uri != null) {
                    viewModel.importWorld(
                        uri = uri,
                        onSuccess = { world ->
                            Toast.makeText(this, "Imported world: ${world.levelName}", Toast.LENGTH_SHORT).show()
                        },
                        onError = { err ->
                            Toast.makeText(this, "Failed to import world: ${err.message}", Toast.LENGTH_LONG).show()
                        }
                    )
                }
            }

            var pendingSkinIsAlex by remember { mutableStateOf(false) }
            val importSkinLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri: Uri? ->
                if (uri != null) {
                    viewModel.importSkin(uri, pendingSkinIsAlex)
                    Toast.makeText(this, "Imported skin!", Toast.LENGTH_SHORT).show()
                }
            }

            val importPackLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri: Uri? ->
                if (uri != null) {
                    viewModel.importPack(uri, isResource = true)
                    Toast.makeText(this, "Pack imported to workspace!", Toast.LENGTH_SHORT).show()
                }
            }

            fun shareFile(file: File) {
                try {
                    val uri = viewModel.storageManager.getFileUri(file)
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "*/*"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(Intent.createChooser(shareIntent, "Share with Minecraft or other apps"))
                } catch (e: Exception) {
                    Toast.makeText(this, "Cannot share file: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            MyApplicationTheme(themeOption = uiState.settings.theme) {
                Scaffold(
                    bottomBar = {
                        // Show bottom nav on main root screens
                        val showBottomNav = uiState.currentRoute in listOf("dashboard", "world_manager", "model_creator", "tools_modular", "settings")
                        if (showBottomNav) {
                            NavigationBar(
                                containerColor = ObsidianSurface,
                                contentColor = CyberCyan,
                                tonalElevation = 8.dp,
                                modifier = Modifier.testTag("launcher_bottom_navigation")
                            ) {
                                NavigationBarItem(
                                    selected = uiState.currentRoute == "dashboard",
                                    onClick = { viewModel.navigateTo("dashboard") },
                                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                    label = { Text("Home") }
                                )
                                NavigationBarItem(
                                    selected = uiState.currentRoute == "world_manager",
                                    onClick = { viewModel.navigateTo("world_manager") },
                                    icon = { Icon(Icons.Default.Public, contentDescription = "Worlds") },
                                    label = { Text("Worlds") }
                                )
                                NavigationBarItem(
                                    selected = uiState.currentRoute == "model_creator",
                                    onClick = { viewModel.navigateTo("model_creator") },
                                    icon = { Icon(Icons.Default.Category, contentDescription = "Models") },
                                    label = { Text("Models") }
                                )
                                NavigationBarItem(
                                    selected = uiState.currentRoute == "tools_modular",
                                    onClick = { viewModel.navigateTo("tools_modular") },
                                    icon = { Icon(Icons.Default.Build, contentDescription = "Tools") },
                                    label = { Text("Tools") }
                                )
                                NavigationBarItem(
                                    selected = uiState.currentRoute == "settings",
                                    onClick = { viewModel.navigateTo("settings") },
                                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                    label = { Text("Settings") }
                                )
                            }
                        }
                    },
                    containerColor = BedrockBlack,
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        Crossfade(targetState = uiState.currentRoute, label = "screen_transition") { route ->
                            when (route) {
                                "dashboard" -> {
                                    DashboardScreen(
                                        minecraftStatus = uiState.minecraftStatus,
                                        worldsCount = uiState.worlds.size,
                                        skinsCount = uiState.skins.size,
                                        packsCount = uiState.packs.size,
                                        modelsCount = uiState.models.size,
                                        storageUsageText = uiState.storageUsageText,
                                        onPlayClick = {
                                            viewModel.minecraftDetector.launchMinecraft().onFailure {
                                                Toast.makeText(this@MainActivity, "Could not launch Minecraft: ${it.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        onInstallPlayStoreClick = {
                                            viewModel.minecraftDetector.openPlayStoreForMinecraft().onFailure {
                                                Toast.makeText(this@MainActivity, "Could not open Google Play Store", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        onNavigateTo = { dest -> viewModel.navigateTo(dest) }
                                    )
                                }

                                "world_manager" -> {
                                    WorldManagerScreen(
                                        worlds = uiState.worlds,
                                        onImportWorld = { importWorldLauncher.launch(arrayOf("*/*")) },
                                        onCreateSampleWorld = { name, seed, gt ->
                                            viewModel.createSampleWorld(name, seed, gt)
                                        },
                                        onExploreWorld = { world -> viewModel.openWorldExplorer(world) },
                                        onCaveViewerWorld = { world -> viewModel.openCaveViewer(world) },
                                        onExportWorld = { world ->
                                            viewModel.exportWorld(world) { exportedFile ->
                                                Toast.makeText(this@MainActivity, "Exported to ${exportedFile.name}", Toast.LENGTH_SHORT).show()
                                                shareFile(exportedFile)
                                            }
                                        },
                                        onBackupWorld = { world ->
                                            viewModel.backupWorld(world) { backupFile ->
                                                Toast.makeText(this@MainActivity, "Backup created: ${backupFile.name}", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        onDuplicateWorld = { world -> viewModel.duplicateWorld(world) },
                                        onRenameWorld = { world, newName -> viewModel.renameWorld(world, newName) },
                                        onShareWorld = { world ->
                                            viewModel.exportWorld(world) { file -> shareFile(file) }
                                        },
                                        onDeleteWorld = { world -> viewModel.deleteWorld(world) }
                                    )
                                }

                                "world_explorer" -> {
                                    uiState.activeWorld?.let { world ->
                                        WorldExplorerScreen(
                                            world = world,
                                            onBack = { viewModel.navigateTo("world_manager") }
                                        )
                                    } ?: run {
                                        viewModel.navigateTo("world_manager")
                                    }
                                }

                                "cave_viewer" -> {
                                    uiState.activeWorld?.let { world ->
                                        CaveViewerScreen(
                                            world = world,
                                            onBack = { viewModel.navigateTo("world_manager") }
                                        )
                                    } ?: run {
                                        viewModel.navigateTo("world_manager")
                                    }
                                }

                                "skin_manager" -> {
                                    SkinManagerScreen(
                                        skins = uiState.skins,
                                        onImportSkin = { isAlex ->
                                            pendingSkinIsAlex = isAlex
                                            importSkinLauncher.launch(arrayOf("image/png", "*/*"))
                                        },
                                        onDeleteSkin = { skin -> viewModel.deleteSkin(skin) },
                                        onRenameSkin = { skin, newName -> viewModel.renameSkin(skin, newName) },
                                        onEquipSkin = { skin ->
                                            val file = viewModel.equipSkin(skin)
                                            if (file != null) {
                                                shareFile(file)
                                            }
                                        },
                                        onLaunchMinecraft = {
                                            viewModel.minecraftDetector.launchMinecraft().onFailure {
                                                viewModel.minecraftDetector.openPlayStoreForMinecraft()
                                            }
                                        }
                                    )
                                }

                                "model_creator" -> {
                                    ModelCreatorScreen(
                                        currentProject = uiState.activeModel,
                                        onSaveProject = { proj -> viewModel.saveModel(proj) },
                                        onExportZip = { proj ->
                                            viewModel.exportModelZip(proj) { file ->
                                                Toast.makeText(this@MainActivity, "Exported: ${file.name}", Toast.LENGTH_SHORT).show()
                                                shareFile(file)
                                            }
                                        },
                                        onImportModel = {
                                            Toast.makeText(this@MainActivity, "Select model zip", Toast.LENGTH_SHORT).show()
                                        },
                                        onNewModel = {
                                            viewModel.saveModel(com.example.data.models.CustomModelProject(id = "model_${System.currentTimeMillis()}", name = "New Entity"))
                                        }
                                    )
                                }

                                "replay_system" -> {
                                    ReplaySystemScreen(
                                        currentReplay = uiState.activeReplay,
                                        onSaveReplay = {},
                                        onExportReplay = { replay ->
                                            viewModel.exportReplay(replay) { file ->
                                                Toast.makeText(this@MainActivity, "Exported: ${file.name}", Toast.LENGTH_SHORT).show()
                                                shareFile(file)
                                            }
                                        },
                                        onBack = { viewModel.navigateTo("dashboard") }
                                    )
                                }

                                "voice_mic" -> {
                                    VoiceMicScreen(
                                        speechManager = viewModel.speechManager,
                                        onBack = { viewModel.navigateTo("dashboard") }
                                    )
                                }

                                "pack_manager" -> {
                                    PackManagerScreen(
                                        packs = uiState.packs,
                                        onImportPack = { importPackLauncher.launch(arrayOf("*/*")) },
                                        onTogglePackEnabled = { pack -> viewModel.togglePackEnabled(pack) },
                                        onDeletePack = { pack -> viewModel.deletePack(pack) },
                                        onDuplicatePack = {},
                                        onBack = { viewModel.navigateTo("dashboard") }
                                    )
                                }

                                "export_center" -> {
                                    ExportCenterScreen(
                                        worlds = uiState.worlds,
                                        skins = uiState.skins,
                                        packs = uiState.packs,
                                        models = uiState.models,
                                        replays = uiState.replays,
                                        onExportWorld = { w ->
                                            viewModel.exportWorld(w) { shareFile(it) }
                                        },
                                        onShareWorld = { w ->
                                            viewModel.exportWorld(w) { shareFile(it) }
                                        },
                                        onExportSkin = { s ->
                                            val f = viewModel.equipSkin(s)
                                            if (f != null) shareFile(f)
                                        },
                                        onExportModel = { m ->
                                            viewModel.exportModelZip(m) { shareFile(it) }
                                        },
                                        onExportReplay = { r ->
                                            viewModel.exportReplay(r) { shareFile(it) }
                                        },
                                        onBack = { viewModel.navigateTo("dashboard") }
                                    )
                                }

                                "tools_modular" -> {
                                    ToolsHubScreen(
                                        worlds = uiState.worlds,
                                        onBack = { viewModel.navigateTo("dashboard") }
                                    )
                                }

                                "settings" -> {
                                    SettingsScreen(
                                        settings = uiState.settings,
                                        onUpdateSettings = { s -> viewModel.updateSettings(s) },
                                        onBackupAllWorkspace = {
                                            viewModel.backupAllWorkspace { file ->
                                                shareFile(file)
                                            }
                                        },
                                        onClearCache = { viewModel.clearCache() }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
