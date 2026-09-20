package com.example.data.launcher

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build

data class MinecraftStatus(
    val isInstalled: Boolean,
    val versionName: String? = null,
    val versionCode: Long = 0,
    val packageName: String = MINECRAFT_PACKAGE_NAME
) {
    companion object {
        const val MINECRAFT_PACKAGE_NAME = "com.mojang.minecraftpe"
    }
}

class MinecraftDetector(private val context: Context) {

    fun detectMinecraft(): MinecraftStatus {
        val pm = context.packageManager
        return try {
            val packageInfo: PackageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(
                    MinecraftStatus.MINECRAFT_PACKAGE_NAME,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(MinecraftStatus.MINECRAFT_PACKAGE_NAME, 0)
            }

            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }

            MinecraftStatus(
                isInstalled = true,
                versionName = packageInfo.versionName ?: "Bedrock Edition",
                versionCode = versionCode
            )
        } catch (e: PackageManager.NameNotFoundException) {
            MinecraftStatus(isInstalled = false)
        } catch (e: Exception) {
            MinecraftStatus(isInstalled = false)
        }
    }

    fun launchMinecraft(): Result<Unit> {
        val status = detectMinecraft()
        if (!status.isInstalled) {
            return Result.failure(IllegalStateException("Minecraft Bedrock is not installed."))
        }

        val pm = context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage(MinecraftStatus.MINECRAFT_PACKAGE_NAME)
            ?: Intent(Intent.ACTION_VIEW, Uri.parse("minecraft://")).apply {
                setPackage(MinecraftStatus.MINECRAFT_PACKAGE_NAME)
            }

        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        return try {
            context.startActivity(launchIntent)
            Result.success(Unit)
        } catch (e: ActivityNotFoundException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun openPlayStoreForMinecraft(): Result<Unit> {
        val playStoreUri = Uri.parse("market://details?id=${MinecraftStatus.MINECRAFT_PACKAGE_NAME}")
        val webUri = Uri.parse("https://play.google.com/store/apps/details?id=${MinecraftStatus.MINECRAFT_PACKAGE_NAME}")

        return try {
            val intent = Intent(Intent.ACTION_VIEW, playStoreUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success(Unit)
        } catch (e: ActivityNotFoundException) {
            try {
                val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
                Result.success(Unit)
            } catch (ex: Exception) {
                Result.failure(ex)
            }
        }
    }
}
