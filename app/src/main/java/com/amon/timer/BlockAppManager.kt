package com.amon.timer

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings

data class InstalledBlockableApp(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val isBlocked: Boolean
)

data class BlockSettingsConfig(
    val blockNotificationsDuringFocus: Boolean = false,
    val blockAppsDuringBreak: Boolean = false
)

object BlockSettingsManager {
    private const val PREFS_NAME = "amon_block_settings_prefs"
    private const val KEY_BLOCK_NOTIFS = "block_notifications_during_focus"
    private const val KEY_BLOCK_DURING_BREAK = "block_apps_during_break"

    fun loadConfig(context: Context): BlockSettingsConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return BlockSettingsConfig(
            blockNotificationsDuringFocus = prefs.getBoolean(KEY_BLOCK_NOTIFS, false),
            blockAppsDuringBreak = prefs.getBoolean(KEY_BLOCK_DURING_BREAK, false)
        )
    }

    fun saveConfig(context: Context, config: BlockSettingsConfig) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_BLOCK_NOTIFS, config.blockNotificationsDuringFocus)
            .putBoolean(KEY_BLOCK_DURING_BREAK, config.blockAppsDuringBreak)
            .apply()
    }
}

object BlockAppManager {
    private const val PREFS_NAME = "amon_block_apps_prefs"
    private const val KEY_BLOCKED_PACKAGES = "blocked_package_names"
    private const val KEY_BLOCKING_ENABLED = "is_app_blocking_enabled"

    fun isBlockingEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_BLOCKING_ENABLED, true)
    }

    fun setBlockingEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_BLOCKING_ENABLED, enabled)
            .apply()
    }

    fun getBlockedPackages(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_BLOCKED_PACKAGES, emptySet())?.toSet() ?: emptySet()
    }

    fun setBlockedPackages(context: Context, packages: Set<String>) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_BLOCKED_PACKAGES, packages.toSet())
            .apply()
    }

    fun toggleAppBlocked(context: Context, packageName: String): Set<String> {
        if (packageName == context.packageName) {
            return getBlockedPackages(context)
        }
        val current = getBlockedPackages(context).toMutableSet()
        if (current.contains(packageName)) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        setBlockedPackages(context, current)
        return current
    }

    fun isPackageBlocked(context: Context, packageName: String): Boolean {
        if (packageName.isBlank() || packageName == context.packageName) return false
        return getBlockedPackages(context).contains(packageName)
    }

    fun hasUsageStatsPermission(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
                ?: return false
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            false
        }
    }

    fun openUsageAccessSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun hasOverlayPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    fun openOverlayPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    fun startBlockerService(context: Context) {
        try {
            val intent = Intent(context, AmonAppBlockerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (_: Exception) {}
    }

    fun getForegroundPackageName(context: Context): String? {
        return try {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return null
            val endTime = System.currentTimeMillis()
            val beginTime = endTime - 10_000L

            val events = usm.queryEvents(beginTime, endTime)
            val event = UsageEvents.Event()
            var lastForegroundPkg: String? = null

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                    event.eventType == UsageEvents.Event.ACTIVITY_RESUMED
                ) {
                    lastForegroundPkg = event.packageName
                }
            }

            if (!lastForegroundPkg.isNullOrBlank()) {
                return lastForegroundPkg
            }

            val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, endTime - 60_000L, endTime)
            stats?.maxByOrNull { it.lastTimeUsed }?.packageName
        } catch (_: Exception) {
            null
        }
    }

    fun getAppIcon(context: Context, packageName: String): Drawable? {
        return try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (_: Exception) {
            null
        }
    }

    fun getAppName(context: Context, packageName: String): String {
        return try {
            val pm = context.packageManager
            val appInfo = getApplicationInfoCompat(pm, packageName)
            appInfo?.loadLabel(pm)?.toString().orEmpty().ifBlank { packageName }
        } catch (_: Exception) {
            packageName
        }
    }

    fun getLaunchableApps(context: Context): List<InstalledBlockableApp> {
        val pm = context.packageManager
        val blockedSet = getBlockedPackages(context)
        val selfPackage = context.packageName

        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(
                    mainIntent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(mainIntent, 0)
            }
        } catch (_: Exception) {
            emptyList()
        }

        val uniqueByPackage = LinkedHashMap<String, InstalledBlockableApp>()

        for (info in resolveInfos) {
            val pkg = info.activityInfo?.packageName.orEmpty()
            if (pkg.isBlank() || pkg == selfPackage) continue
            if (uniqueByPackage.containsKey(pkg)) continue

            val label = try {
                info.loadLabel(pm)?.toString().orEmpty().ifBlank {
                    pkg.substringAfterLast('.')
                }
            } catch (_: Exception) {
                pkg.substringAfterLast('.')
            }

            val icon = try {
                info.loadIcon(pm)
            } catch (_: Exception) {
                null
            }

            uniqueByPackage[pkg] = InstalledBlockableApp(
                packageName = pkg,
                appName = label,
                icon = icon,
                isBlocked = blockedSet.contains(pkg)
            )
        }

        return uniqueByPackage.values.sortedWith(
            compareByDescending<InstalledBlockableApp> { it.isBlocked }
                .thenBy { it.appName.lowercase() }
        )
    }

    private fun getApplicationInfoCompat(pm: PackageManager, packageName: String): ApplicationInfo? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, 0)
            }
        } catch (_: Exception) {
            null
        }
    }
}
