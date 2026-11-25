package com.amon.timer

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager

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
        return prefs.getBoolean(KEY_BLOCKING_ENABLED, false)
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

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        return try {
            val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
                ?: return false
            val enabledServices = am.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_GENERIC
            )
            val expectedComponentPrefix = "${context.packageName}/"
            enabledServices.any { serviceInfo ->
                val id = serviceInfo.id.orEmpty()
                id.startsWith(expectedComponentPrefix) || id.contains(context.packageName)
            }
        } catch (_: Exception) {
            false
        }
    }

    fun openAccessibilitySettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
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
