package dev.tommy.tvlauncher

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import android.media.tv.TvContract
import android.provider.Settings

private const val TV_SETTINGS_PACKAGE = "com.android.tv.settings"
private const val PHILIPS_EUM_PACKAGE = "org.droidtv.eum"
private const val PROJECTIVY_INPUT_MARKER = ".guidedActions.activities.input."
private const val PORT_KEY_PREFIX = "port:"
private const val SHORTCUT_KEY_PREFIX = "shortcut:"
private val HIDDEN_PACKAGES = setOf(TV_SETTINGS_PACKAGE, PHILIPS_EUM_PACKAGE)

class AppRepository(
    private val context: Context,
    private val customization: Customization,
    private val artRepository: ArtRepository,
) {
    private val packageManager = context.packageManager
    private val portRepository = PortRepository(context)

    fun loadContent(): HomeContent {
        val leanbackActivities = queryVisibleActivities(Intent.CATEGORY_LEANBACK_LAUNCHER)
        val leanbackKeys = leanbackActivities.map(::keyOf).toSet()
        val leanbackPackages = leanbackActivities.map { it.activityInfo.packageName }.toSet()
        val phoneOnlyActivities = queryVisibleActivities(Intent.CATEGORY_LAUNCHER)
            .filterNot { it.activityInfo.packageName in leanbackPackages }
        val appTiles = (leanbackActivities + phoneOnlyActivities).map(::toAppTile)
        val homeCandidates = appTiles.filter { it.key in leanbackKeys } + loadPortTiles()
        val hiddenKeys = customization.hiddenKeys()
        return HomeContent(
            homeTiles = ordered(homeCandidates.filterNot { it.key in hiddenKeys }),
            appTiles = ordered(appTiles.filterNot { it.key in hiddenKeys }),
            shortcutTiles = shortcutTiles(),
            hiddenTiles = (homeCandidates + appTiles).distinctBy { it.key }.filter { it.key in hiddenKeys },
        )
    }

    private fun keyOf(resolveInfo: ResolveInfo) = "${resolveInfo.activityInfo.packageName}/${resolveInfo.activityInfo.name}"

    private fun isHidden(resolveInfo: ResolveInfo): Boolean {
        val activityInfo = resolveInfo.activityInfo
        val isOwnApp = activityInfo.packageName == context.packageName
        val isProjectivyInput = activityInfo.name.contains(PROJECTIVY_INPUT_MARKER)
        return isOwnApp || isProjectivyInput || activityInfo.packageName in HIDDEN_PACKAGES
    }

    @Suppress("DEPRECATION")
    private fun queryVisibleActivities(category: String): List<ResolveInfo> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(category)
        return packageManager.queryIntentActivities(intent, 0).filterNot(::isHidden)
    }

    private fun ordered(tiles: List<Tile>): List<Tile> {
        val positions = customization.order().withIndex().associate { it.value to it.index }
        return tiles.sortedWith(
            compareBy<Tile> { positions[it.key] ?: Int.MAX_VALUE }
                .thenBy { it.kind.ordinal }
                .thenBy { it.label.lowercase() },
        )
    }

    private fun toAppTile(resolveInfo: ResolveInfo): Tile {
        val activityInfo = resolveInfo.activityInfo
        val key = keyOf(resolveInfo)
        val artId = customization.artFor(key) ?: artRepository.defaultArtIdFor(activityInfo.packageName)
        val image = artId?.let(artRepository::load) ?: activityInfo.loadBanner(packageManager)
        val launchIntent = Intent(Intent.ACTION_MAIN)
            .setClassName(activityInfo.packageName, activityInfo.name)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return Tile(
            key = key,
            kind = TileKind.APP,
            label = customization.labelFor(key) ?: resolveInfo.loadLabel(packageManager).toString(),
            launchIntent = launchIntent,
            tileImage = image ?: resolveInfo.loadIcon(packageManager),
            hasBanner = image != null,
            packageName = activityInfo.packageName,
            canUninstall = isUserInstalled(activityInfo.applicationInfo),
        )
    }

    private fun isUserInstalled(applicationInfo: ApplicationInfo): Boolean {
        val isSystem = applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
        val isUpdatedSystem = applicationInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0
        return !isSystem || isUpdatedSystem
    }

    private fun loadPortTiles() = portRepository.loadPorts().map(::toPortTile)

    private fun toPortTile(port: PortInfo): Tile {
        val key = PORT_KEY_PREFIX + port.inputId
        val label = customization.labelFor(key) ?: context.getString(R.string.port_label, port.number)
        val customArt = customization.artFor(key)?.let(artRepository::load)
        val passthroughUri = TvContract.buildChannelUriForPassthroughInput(port.inputId)
        return Tile(
            key = key,
            kind = TileKind.PORT,
            label = label,
            launchIntent = Intent(Intent.ACTION_VIEW, passthroughUri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            tileImage = customArt ?: PortArtFactory.create(context, label),
            hasBanner = true,
        )
    }

    private fun shortcutTiles() = listOf(
        shortcutTile(R.string.shortcut_network, Intent(Settings.ACTION_WIFI_SETTINGS), R.drawable.ic_card_network),
        shortcutTile(R.string.shortcut_picture, Intent(Settings.ACTION_DISPLAY_SETTINGS), R.drawable.ic_card_picture),
        shortcutTile(R.string.shortcut_startup, Intent(Settings.ACTION_HOME_SETTINGS), R.drawable.ic_card_startup),
        shortcutTile(R.string.shortcut_hidden, null, R.drawable.ic_card_hidden, HIDDEN_ITEMS_KEY),
        shortcutTile(R.string.shortcut_all_settings, Intent(Settings.ACTION_SETTINGS), R.drawable.ic_card_settings),
    )

    private fun shortcutTile(labelRes: Int, intent: Intent?, iconRes: Int, key: String? = null) = Tile(
        key = key ?: SHORTCUT_KEY_PREFIX + labelRes,
        kind = TileKind.SHORTCUT,
        label = context.getString(labelRes),
        launchIntent = intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        tileImage = requireNotNull(context.getDrawable(iconRes)),
        hasBanner = false,
    )
}
