package dev.tommy.tvlauncher

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.min

private const val ENTRANCE_WALLPAPER_SCALE = 1.12f
private const val ENTRANCE_WALLPAPER_MS = 1000L
private const val ENTRANCE_ITEM_MS = 520L
private const val ENTRANCE_BAR_DELAY_MS = 150L
private const val ENTRANCE_SHELF_DELAY_MS = 260L
private const val ENTRANCE_TILE_DELAY_MS = 320L
private const val ENTRANCE_TILE_STAGGER_MS = 55L
private const val ENTRANCE_STAGGERED_TILES = 7
private const val HOME_SECTION_INDEX = 0
private const val APPS_GRID_COLUMNS = 4
private const val SECTION_ANIMATION_MS = 200L
private const val BACKDROP_ANIMATION_MS = 260L
private const val VISIBLE_ALPHA = 1f
private const val HIDDEN_ALPHA = 0f
private const val PACKAGE_URI_SCHEME = "package"
private const val MOVE_BACKWARD = -1
private const val MOVE_FORWARD = 1

private class FocusTarget(val key: String, val row: RecyclerView, val fallbackPosition: Int)

class MainActivity : Activity() {
    private val loaderExecutor = Executors.newSingleThreadExecutor()
    private val homeAdapter = createAdapter(R.layout.item_app, alwaysShowLabel = false)
    private val appsAdapter = createAdapter(R.layout.item_app, alwaysShowLabel = false)
    private val shortcutsAdapter = createAdapter(R.layout.item_setting, alwaysShowLabel = true)
    private val packageChangeReceiver = PackageChangeReceiver(::loadContent)
    private lateinit var customization: Customization
    private lateinit var artRepository: ArtRepository
    private lateinit var appRepository: AppRepository
    private lateinit var blurredBitmap: Bitmap
    private lateinit var sharpWallpaper: ImageView
    private lateinit var blurredWallpaper: ImageView
    private lateinit var homeShelf: View
    private lateinit var tabBar: GlassTabBar
    private lateinit var tileMenu: TileMenu
    private lateinit var homeRow: RecyclerView
    private lateinit var sections: List<View>
    private var selectedSection = HOME_SECTION_INDEX
    private var hiddenTiles: List<Tile> = emptyList()
    private var pendingFocus: FocusTarget? = null
    private var isEntrancePending = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        customization = Customization(this)
        artRepository = ArtRepository(this)
        appRepository = AppRepository(this, customization, artRepository)
        bindWallpaper()
        bindSections()
        bindTabBar()
        bindMenu()
        prepareEntrance()
        playEntrance()
        registerReceiver(packageChangeReceiver, PackageChangeReceiver.intentFilter())
        loadContent()
    }

    override fun onDestroy() {
        unregisterReceiver(packageChangeReceiver)
        loaderExecutor.shutdown()
        super.onDestroy()
    }

    @Deprecated("Home screen has nothing to go back to")
    override fun onBackPressed() {
        if (tileMenu.isShowing) return tileMenu.dismiss()
        if (!tabBar.hasFocus()) tabBar.focusSelectedTab()
    }

    private fun createAdapter(layoutRes: Int, alwaysShowLabel: Boolean) =
        AppAdapter(layoutRes, alwaysShowLabel, ::launchTile, ::openTileMenu)

    private fun prepareEntrance() {
        sharpWallpaper.scaleX = ENTRANCE_WALLPAPER_SCALE
        sharpWallpaper.scaleY = ENTRANCE_WALLPAPER_SCALE
        sharpWallpaper.alpha = HIDDEN_ALPHA
        tabBar.alpha = HIDDEN_ALPHA
        homeShelf.alpha = HIDDEN_ALPHA
        homeRow.alpha = HIDDEN_ALPHA
    }

    private fun playEntrance() {
        sharpWallpaper.animate().scaleX(VISIBLE_ALPHA).scaleY(VISIBLE_ALPHA).alpha(VISIBLE_ALPHA)
            .setDuration(ENTRANCE_WALLPAPER_MS).setInterpolator(appleEaseOut).start()
        revealView(tabBar, R.dimen.entrance_bar_offset, ENTRANCE_BAR_DELAY_MS)
        revealView(homeShelf, R.dimen.entrance_shelf_offset, ENTRANCE_SHELF_DELAY_MS)
    }

    private fun revealView(view: View, offsetRes: Int, delayMs: Long) {
        view.translationY = resources.getDimension(offsetRes)
        view.animate().alpha(VISIBLE_ALPHA).translationY(0f)
            .setStartDelay(delayMs).setDuration(ENTRANCE_ITEM_MS).setInterpolator(appleEaseOut).start()
    }

    private fun cascadeTiles() {
        homeRow.alpha = VISIBLE_ALPHA
        for (index in 0 until homeRow.childCount) {
            val order = min(index, ENTRANCE_STAGGERED_TILES - 1)
            revealView(homeRow.getChildAt(index).apply { alpha = HIDDEN_ALPHA }, R.dimen.entrance_tile_offset, ENTRANCE_TILE_DELAY_MS + order * ENTRANCE_TILE_STAGGER_MS)
        }
    }

    private fun bindWallpaper() {
        sharpWallpaper = findViewById(R.id.wallpaper_sharp)
        homeShelf = findViewById(R.id.home_shelf)
        blurredBitmap = BitmapFactory.decodeResource(resources, R.drawable.wallpaper_blur)
        blurredWallpaper = findViewById(R.id.wallpaper_blur)
        blurredWallpaper.setImageBitmap(blurredBitmap)
    }

    private fun bindSections() {
        homeRow = configureRow(R.id.home_row, homeAdapter, LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false))
        val appsGrid = configureRow(R.id.apps_grid, appsAdapter, GridLayoutManager(this, APPS_GRID_COLUMNS))
        val settingsRow = configureRow(R.id.settings_row, shortcutsAdapter, LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false))
        sections = listOf<View>(findViewById(R.id.section_home), findViewById(R.id.section_apps), settingsRow)
    }

    private fun configureRow(rowId: Int, adapter: AppAdapter, layoutManager: RecyclerView.LayoutManager): RecyclerView {
        val row = findViewById<RecyclerView>(rowId)
        row.layoutManager = layoutManager
        row.adapter = adapter
        row.itemAnimator = null
        return row
    }

    private fun bindTabBar() {
        tabBar = findViewById(R.id.tab_bar)
        tabBar.setBackdrop(blurredBitmap)
        tabBar.setTabs(
            listOf(
                TabSpec(getString(R.string.tab_home), R.drawable.ic_tab_home),
                TabSpec(getString(R.string.tab_apps), R.drawable.ic_tab_apps),
                TabSpec(getString(R.string.tab_settings), R.drawable.ic_settings),
            ),
        )
        tabBar.onTabSelected = ::showSection
    }

    private fun bindMenu() {
        val card = findViewById<GlassFrameLayout>(R.id.menu_card)
        card.setBackdrop(blurredBitmap)
        tileMenu = TileMenu(
            findViewById(R.id.menu_overlay),
            card,
            findViewById(R.id.menu_title),
            findViewById(R.id.menu_input),
            findViewById(R.id.menu_scroll),
            findViewById(R.id.menu_stack),
            findViewById(R.id.menu_actions),
            findViewById(R.id.content_root),
        )
    }

    private fun showSection(index: Int) {
        val outgoing = sections[selectedSection]
        val incoming = sections[index]
        selectedSection = index
        fadeOut(outgoing)
        fadeIn(incoming)
        val backdropAlpha = if (index == HOME_SECTION_INDEX) HIDDEN_ALPHA else VISIBLE_ALPHA
        blurredWallpaper.animate().alpha(backdropAlpha).setDuration(BACKDROP_ANIMATION_MS).setInterpolator(appleEaseOut).start()
    }

    private fun fadeOut(section: View) {
        section.animate().alpha(HIDDEN_ALPHA).setDuration(SECTION_ANIMATION_MS).setInterpolator(appleEaseOut)
            .withLayer().withEndAction { section.visibility = View.INVISIBLE }.start()
    }

    private fun fadeIn(section: View) {
        if (section.visibility != View.VISIBLE) {
            section.alpha = HIDDEN_ALPHA
            section.translationY = resources.getDimension(R.dimen.section_enter_offset)
            section.visibility = View.VISIBLE
        }
        section.animate().alpha(VISIBLE_ALPHA).translationY(0f).setDuration(SECTION_ANIMATION_MS).setInterpolator(appleEaseOut)
            .withLayer().start()
    }

    private fun loadContent() {
        loaderExecutor.execute(::publishContent)
    }

    private fun publishContent() {
        val content = appRepository.loadContent()
        runOnUiThread { showContent(content) }
    }

    private fun showContent(content: HomeContent) {
        hiddenTiles = content.hiddenTiles
        val target = pendingFocus ?: focusTargetOf(currentFocus)
        pendingFocus = null
        homeAdapter.submit(content.homeTiles)
        appsAdapter.submit(content.appTiles)
        shortcutsAdapter.submit(content.shortcutTiles)
        if (isEntrancePending) homeRow.post(::cascadeTiles)
        isEntrancePending = false
        homeRow.post { restoreFocus(target) }
    }

    private fun focusTargetOf(view: View?): FocusTarget? {
        val row = view?.parent as? RecyclerView ?: return null
        val key = view.tag as? String ?: return null
        return FocusTarget(key, row, row.getChildAdapterPosition(view))
    }

    private fun restoreFocus(target: FocusTarget?) {
        if (tileMenu.isShowing) return
        if (target == null) return focusFirstHomeTile()
        val adapter = target.row.adapter as AppAdapter
        val lastPosition = max(adapter.itemCount - 1, 0)
        val position = adapter.indexOfKey(target.key).takeIf { it >= 0 } ?: target.fallbackPosition.coerceIn(0, lastPosition)
        target.row.scrollToPosition(position)
        target.row.post { target.row.findViewHolderForAdapterPosition(position)?.itemView?.requestFocus() }
    }

    private fun focusFirstHomeTile() {
        if (currentFocus == null) homeRow.getChildAt(0)?.requestFocus()
    }

    private fun launchTile(tile: Tile) {
        if (tile.key == HIDDEN_ITEMS_KEY) return showHiddenItems()
        tile.launchIntent?.let(::startSafely)
    }

    private fun startSafely(intent: Intent) {
        try {
            startActivity(intent)
        } catch (error: RuntimeException) {
            Toast.makeText(this, R.string.launch_error, Toast.LENGTH_SHORT).show()
        }
    }

    private fun openTileMenu(tile: Tile) {
        if (tile.kind == TileKind.SHORTCUT) return
        val row = currentFocus?.parent as? RecyclerView
        pendingFocus = focusTargetOf(currentFocus)
        tileMenu.show(tile.label, tileMenuEntries(tile, row))
    }

    private fun tileMenuEntries(tile: Tile, row: RecyclerView?) = listOfNotNull(
        MenuEntry(getString(R.string.menu_open), R.drawable.ic_action_open) { launchTile(tile) },
        MenuEntry(getString(R.string.menu_move_left), R.drawable.ic_action_arrow_back) { moveTile(tile, row, MOVE_BACKWARD) },
        MenuEntry(getString(R.string.menu_move_right), R.drawable.ic_action_arrow_forward) { moveTile(tile, row, MOVE_FORWARD) },
        MenuEntry(getString(R.string.menu_rename), R.drawable.ic_action_rename) { renameTile(tile) },
        MenuEntry(getString(R.string.menu_change_image), R.drawable.ic_action_image) { openArtMenu(tile) },
        MenuEntry(getString(R.string.menu_hide), R.drawable.ic_hidden) { setTileHidden(tile, true) },
        MenuEntry(getString(R.string.menu_info), R.drawable.ic_action_info) { showAppInfo(tile) }.takeIf { tile.kind == TileKind.APP },
        MenuEntry(getString(R.string.menu_uninstall), R.drawable.ic_action_delete, isDestructive = true) { uninstall(tile) }
            .takeIf { tile.canUninstall },
    )

    private fun moveTile(tile: Tile, row: RecyclerView?, direction: Int) {
        val adapter = row?.adapter as? AppAdapter ?: return
        val keys = adapter.keys().toMutableList()
        val from = keys.indexOf(tile.key)
        if (from < 0) return
        keys.add((from + direction).coerceIn(0, keys.lastIndex), keys.removeAt(from))
        customization.saveOrder(keys + customization.order().filterNot { it in keys })
        loadContent()
    }

    private fun renameTile(tile: Tile) {
        tileMenu.promptText(getString(R.string.rename_title), tile.label) { name ->
            customization.setLabel(tile.key, name)
            loadContent()
        }
    }

    private fun setTileHidden(tile: Tile, hidden: Boolean) {
        customization.setHidden(tile.key, hidden)
        loadContent()
    }

    private fun openArtMenu(tile: Tile) {
        val defaultEntry = MenuEntry(getString(R.string.art_default), R.drawable.ic_action_restore) { applyArt(tile, null) }
        val optionEntries = artRepository.options().map { option ->
            MenuEntry(option.label, R.drawable.ic_action_image) { applyArt(tile, option.id) }
        }
        tileMenu.show(getString(R.string.art_title, tile.label), listOf(defaultEntry) + optionEntries)
    }

    private fun applyArt(tile: Tile, artId: String?) {
        customization.setArt(tile.key, artId)
        loadContent()
    }

    private fun showHiddenItems() {
        val entries = hiddenTiles.map { tile -> MenuEntry(tile.label, R.drawable.ic_action_visible) { setTileHidden(tile, false) } }
        val shownEntries = entries.ifEmpty { listOf(MenuEntry(getString(R.string.hidden_empty)) { }) }
        tileMenu.show(getString(R.string.hidden_title), shownEntries)
    }

    private fun showAppInfo(tile: Tile) {
        startSafely(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUriOf(tile)))
    }

    private fun uninstall(tile: Tile) {
        startSafely(Intent(Intent.ACTION_DELETE, packageUriOf(tile)))
    }

    private fun packageUriOf(tile: Tile) = Uri.fromParts(PACKAGE_URI_SCHEME, tile.packageName, null)
}
