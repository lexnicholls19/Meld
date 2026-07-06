package com.lexnicholls.lovecounter.util

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.core.net.toUri
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.lexnicholls.lovecounter.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
    private val sharedPrefs = context.getSharedPreferences("update_prefs", Context.MODE_PRIVATE)

    init {
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(3600) // Check every hour
            .build()
        remoteConfig.setConfigSettingsAsync(configSettings)
        
        // Initialize last seen version on first run
        if (!sharedPrefs.contains("last_installed_version")) {
            sharedPrefs.edit { putInt("last_installed_version", BuildConfig.VERSION_CODE) }
        }
    }

    data class UpdateInfo(
        val latestVersionCode: Int,
        val latestVersionName: String,
        val downloadUrl: String,
        val changelog: String,
        val isUpdateAvailable: Boolean
    )

    fun checkForUpdates(onResult: (UpdateInfo) -> Unit) {
        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val latestCode = remoteConfig.getLong("latest_version_code").toInt()
                val latestName = remoteConfig.getString("latest_version_name")
                val downloadUrl = remoteConfig.getString("update_url")
                
                val lang = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
                    .getString("app_language", "system") ?: "system"
                
                val changelog = if (lang == "es") {
                    remoteConfig.getString("changelog_es")
                } else {
                    remoteConfig.getString("changelog_en")
                }

                val currentCode = BuildConfig.VERSION_CODE
                onResult(
                    UpdateInfo(
                        latestCode,
                        latestName,
                        downloadUrl,
                        changelog,
                        latestCode > currentCode
                    )
                )
            }
        }
    }

    fun downloadAndInstall(downloadUrl: String, versionName: String) {
        val destination = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "Meld_v$versionName.apk")
        if (destination.exists()) destination.delete()

        val request = DownloadManager.Request(downloadUrl.toUri())
            .setTitle("Meld Update v$versionName")
            .setDescription("Downloading new version...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(destination))

        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = manager.enqueue(request)

        // Registrar receiver para instalar cuando termine
        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (id == downloadId) {
                    installApk(destination)
                    context.unregisterReceiver(this)
                }
            }
        }
        
        ContextCompat.registerReceiver(
            context,
            onComplete,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    private fun installApk(file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun getNewVersionChangelog(): String? {
        val lastSeenVersion = sharedPrefs.getInt("last_installed_version", 0)
        val currentVersion = BuildConfig.VERSION_CODE
        
        if (currentVersion > lastSeenVersion) {
            // No podemos obtener el changelog de Remote Config aquí fácilmente porque no sabemos de qué versión venimos
            // Pero podemos mostrar un mensaje genérico o intentar recuperarlo si lo guardamos antes.
            // Para el flujo solicitado, guardaremos el changelog cuando detectamos la actualización.
            return sharedPrefs.getString("pending_changelog", null)
        }
        return null
    }

    fun markUpdateAsSeen() {
        sharedPrefs.edit().putInt("last_installed_version", BuildConfig.VERSION_CODE).apply()
    }
    
    fun savePendingChangelog(changelog: String) {
        sharedPrefs.edit().putString("pending_changelog", changelog).apply()
    }
}