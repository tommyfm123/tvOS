package dev.tommy.tvlauncher

import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

private const val MENU_ANIMATION_MS = 260L
private const val SELECTOR_ANIMATION_MS = 200L
private const val MENU_SOURCE_SCALE = 0.4f
private const val MENU_FONT_FAMILY = "sans-serif-medium"
private const val VISIBLE_ALPHA = 1f
private const val HIDDEN_ALPHA = 0f
private const val FULL_SCALE = 1f
const val NO_ICON = 0

data class MenuEntry(
    val label: String,
    val iconRes: Int = NO_ICON,
    val isDestructive: Boolean = false,
    val onSelected: () -> Unit,
)

class TileMenu(
    private val overlay: View,
    private val card: GlassFrameLayout,
    private val titleView: TextView,
    private val inputField: EditText,
    private val scroll: ScrollView,
    private val stack: FrameLayout,
    private val actions: LinearLayout,
    private val blockedContent: ViewGroup,
) : View.OnFocusChangeListener, TextView.OnEditorActionListener {
    var isShowing = false
        private set

    private val resources = overlay.resources
    private val inputMethodManager = overlay.context.getSystemService(InputMethodManager::class.java)
    private val rowSelector = View(overlay.context)
    private val screenLocation = IntArray(2)
    private var returnFocus: View? = null
    private var sourceOffsetX = 0f
    private var sourceOffsetY = 0f
    private var onTextSubmitted: ((String) -> Unit)? = null

    init {
        card.cornerRadius = resources.getDimension(R.dimen.menu_corner)
        rowSelector.setBackgroundResource(R.drawable.menu_selector)
        stack.addView(rowSelector, 0, FrameLayout.LayoutParams(0, 0))
        inputField.setOnEditorActionListener(this)
    }

    fun show(title: String, entries: List<MenuEntry>) {
        present(title)
        inputField.visibility = View.GONE
        scroll.visibility = View.VISIBLE
        actions.removeAllViews()
        entries.forEach { entry -> actions.addView(createRow(entry)) }
        limitHeight(entries.size)
        actions.getChildAt(0)?.requestFocus()
        stack.post { alignSelector(actions.getChildAt(0), animate = false) }
    }

    fun promptText(title: String, initialText: String, onSubmit: (String) -> Unit) {
        present(title)
        onTextSubmitted = onSubmit
        scroll.visibility = View.GONE
        inputField.visibility = View.VISIBLE
        inputField.setText(initialText)
        inputField.setSelection(initialText.length)
        inputField.requestFocus()
        inputField.post { inputMethodManager.showSoftInput(inputField, InputMethodManager.SHOW_IMPLICIT) }
    }

    fun dismiss() {
        isShowing = false
        inputMethodManager.hideSoftInputFromWindow(overlay.windowToken, 0)
        blockedContent.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        overlay.animate().alpha(HIDDEN_ALPHA).setDuration(MENU_ANIMATION_MS).setInterpolator(appleEaseOut)
            .withEndAction { overlay.visibility = View.GONE }.start()
        card.animate().alpha(HIDDEN_ALPHA).translationX(sourceOffsetX).translationY(sourceOffsetY)
            .scaleX(MENU_SOURCE_SCALE).scaleY(MENU_SOURCE_SCALE)
            .setDuration(MENU_ANIMATION_MS).setInterpolator(appleEaseOut).start()
        returnFocus?.takeIf { it.isAttachedToWindow }?.requestFocus()
    }

    override fun onFocusChange(view: View, hasFocus: Boolean) {
        if (hasFocus) alignSelector(view, animate = true)
    }

    override fun onEditorAction(view: TextView, actionId: Int, event: android.view.KeyEvent?): Boolean {
        if (actionId != EditorInfo.IME_ACTION_DONE) return false
        val text = view.text.toString()
        val submit = onTextSubmitted
        dismiss()
        submit?.invoke(text)
        return true
    }

    private fun present(title: String) {
        returnFocus = overlay.rootView.findFocus()
        isShowing = true
        titleView.text = title
        blockedContent.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
        measureSourceOffset()
        overlay.alpha = HIDDEN_ALPHA
        card.alpha = HIDDEN_ALPHA
        card.translationX = sourceOffsetX
        card.translationY = sourceOffsetY
        card.scaleX = MENU_SOURCE_SCALE
        card.scaleY = MENU_SOURCE_SCALE
        overlay.visibility = View.VISIBLE
        overlay.animate().alpha(VISIBLE_ALPHA).setDuration(MENU_ANIMATION_MS).setInterpolator(appleEaseOut).start()
        card.animate().alpha(VISIBLE_ALPHA).translationX(0f).translationY(0f).scaleX(FULL_SCALE).scaleY(FULL_SCALE)
            .setDuration(MENU_ANIMATION_MS).setInterpolator(appleEaseOut).start()
    }

    private fun measureSourceOffset() {
        val source = returnFocus?.takeIf { it.isAttachedToWindow }
        if (source == null) {
            sourceOffsetX = 0f
            sourceOffsetY = 0f
            return
        }
        source.getLocationOnScreen(screenLocation)
        sourceOffsetX = screenLocation[0] + source.width / 2f - overlay.width / 2f
        sourceOffsetY = screenLocation[1] + source.height / 2f - overlay.height / 2f
    }

    private fun alignSelector(row: View?, animate: Boolean) {
        row ?: return
        val params = rowSelector.layoutParams
        if (params.height != row.height || params.width != row.width) {
            params.height = row.height
            params.width = row.width
            rowSelector.layoutParams = params
        }
        rowSelector.animate().cancel()
        if (!animate) {
            rowSelector.translationY = row.top.toFloat()
            return
        }
        rowSelector.animate().translationY(row.top.toFloat())
            .setDuration(SELECTOR_ANIMATION_MS).setInterpolator(appleEaseOut).start()
    }

    private fun limitHeight(rowCount: Int) {
        val maxRows = resources.getInteger(R.integer.menu_max_rows)
        val rowHeight = resources.getDimensionPixelSize(R.dimen.menu_row_height)
        scroll.layoutParams.height = if (rowCount > maxRows) rowHeight * maxRows else ViewGroup.LayoutParams.WRAP_CONTENT
        scroll.scrollTo(0, 0)
    }

    private fun createRow(entry: MenuEntry) = TextView(overlay.context).apply {
        val contentColor = context.getColor(if (entry.isDestructive) R.color.menu_destructive else R.color.text_primary)
        text = entry.label
        gravity = Gravity.CENTER_VERTICAL
        minHeight = resources.getDimensionPixelSize(R.dimen.menu_row_height)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.menu_text_size))
        setTextColor(contentColor)
        typeface = Typeface.create(MENU_FONT_FAMILY, Typeface.NORMAL)
        maxLines = 1
        setPadding(
            resources.getDimensionPixelSize(R.dimen.menu_row_padding),
            0,
            resources.getDimensionPixelSize(R.dimen.menu_row_padding),
            0,
        )
        showIcon(this, entry.iconRes, contentColor)
        isFocusable = true
        isClickable = true
        onFocusChangeListener = this@TileMenu
        setOnClickListener { selectEntry(entry) }
    }

    private fun showIcon(row: TextView, iconRes: Int, color: Int) {
        val iconSize = resources.getDimensionPixelSize(R.dimen.menu_icon_size)
        val icon = (if (iconRes == NO_ICON) null else row.context.getDrawable(iconRes))?.mutate()
        icon?.setBounds(0, 0, iconSize, iconSize)
        icon?.setTint(color)
        row.setCompoundDrawablesRelative(icon, null, null, null)
        row.compoundDrawablePadding = resources.getDimensionPixelSize(R.dimen.menu_icon_padding)
    }

    private fun selectEntry(entry: MenuEntry) {
        dismiss()
        entry.onSelected()
    }
}
