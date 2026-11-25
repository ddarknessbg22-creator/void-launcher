package com.voidlauncher.engine

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.FileProvider
import com.voidlauncher.model.EngineInfo
import com.voidlauncher.util.Net
import com.voidlauncher.util.VoidPaths
import java.io.File
import org.json.JSONObject

private data class EngineDef(
    val packageName: String,
    val displayName: String,
    val note: String,
)

/**
 * VOID does not bundle a JVM / LWJGL / GL translation stack — that is a full native
 * engine in its own right. Instead it detects an installed, proven engine and hands
 * the launch to it. This is a real handoff, not a simulated launch.
 */
class EngineRepository(context: Context) {
    private val app = context.applicationContext
    private val pm = app.packageManager

    private val known = listOf(
        EngineDef("net.kdt.pojavlaunch", "PojavLauncher", "Most compatible; Android ARM64"),
        EngineDef("net.kdt.pojavlaunch.debug", "PojavLauncher (debug)", "Development build"),
        EngineDef("com.amethyst.launcher", "Amethyst", "PojavLauncher successor"),
        EngineDef("org.fcl.launcher", "Fold Craft Launcher", "Foldable / large-screen focused"),
        EngineDef("com.zalith.launcher", "ZalithLauncher", "MobileGlues renderer / Sodium friendly"),
        EngineDef("com.movtery.zalithlauncher", "ZalithLauncher", "MobileGlues renderer / Sodium friendly"),
        EngineDef("com.rxlauncher.app", "RXLauncher", "Mod-focused launcher"),
        EngineDef("com.mcinabox.launcher", "MCinaBox", "Boardwalk-based launcher"),
    )

    fun detect(): List<EngineInfo> = known.map { def ->
        try {
            val info = pm.getPackageInfo(def.packageName, 0)
            EngineInfo(
                packageName = def.packageName,
                displayName = def.displayName,
                installed = true,
                versionName = info.versionName,
                note = def.note,
            )
        } catch (e: PackageManager.NameNotFoundException) {
            EngineInfo(
                packageName = def.packageName,
                displayName = def.displayName,
                installed = false,
                note = def.note,
            )
        }
    }

    fun installedEngines(): List<EngineInfo> = detect().filter { it.installed }

    fun preferred(preferredPackage: String?, available: List<EngineInfo>): EngineInfo? =
        available.firstOrNull { it.packageName == preferredPackage } ?: available.firstOrNull()

    fun launch(context: Context, engine: EngineInfo): Result<Unit> = runCatching {
        val intent = pm.getLaunchIntentForPackage(engine.packageName)
            ?: throw IllegalStateException("${engine.displayName} has no launchable activity")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /** Downloads the official ARM64 engine release from GitHub. Real bytes, real progress. */
    fun downloadEngine(
        context: Context,
        onProgress: (Long, Long) -> Unit = { _, _ -> },
    ): Result<File> = runCatching {
        // Tries the GitHub API first, then a stable redirect path if the API is rate limited.
        val url = Net.resolveEngineApkUrl()
        val file = File(VoidPaths.exports(VoidPaths.root(context.applicationContext)), "engine-arm64-v8a.apk")
        Net.downloadToFile(url, file, null, onProgress = onProgress)
        require(file.length() > 5_000_000L) { "Engine download was incomplete" }
        file
    }

    /** Hands the downloaded APK to Android's installer. Requires REQUEST_INSTALL_PACKAGES. */
    fun installEngine(context: Context, file: File): Result<Unit> = runCatching {
        val uri: Uri = FileProvider.getUriForFile(
            context.applicationContext,
            "${context.packageName}.fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    companion object {
        const val ENGINE_SOURCE_URL = "https://github.com/PojavLauncherTeam/PojavLauncher/releases"
        const val ENGINE_DOCS_URL = "https://pojav.ru/wiki/getting_started/INSTALL.html"
        const val RELEASES_API = "https://api.github.com/repos/ZalithLauncher/ZalithLauncher2/releases/latest"
    }
}
