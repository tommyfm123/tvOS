package dev.tommy.tvlauncher

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.roundToInt

private const val SELECTOR_ANIMATION_MS = 260L
private const val EMPHASIS_ANIMATION_MS = 160L
private const val FOCUSED_SELECTOR_ALPHA = 1f
private const val IDLE_SELECTOR_ALPHA = 0.55f
private const val SELECTED_TAB_ALPHA = 1f
private const val UNSELECTED_TAB_ALPHA = 0.7f
private const val TAB_FONT_FAMILY = "sans-serif-medium"
private const val FULL_FRACTION = 1f

data class TabSpec(val title: String, val iconRes: Int)

class GlassTabBar @JvmOverloads constructor(
    context: Context,
    attributes: AttributeSet? = null,
) : GlassFrameLayout(context, attributes), View.OnFocusChangeListener {
    var onTabSelected: ((Int) -> Unit)? = null

    private val selector = View(context)
    private val tabRow = LinearLayout(context)
    private val selectorAnimator = ValueAnimator.ofFloat(0f, FULL_FRACTION)
    private var selectorFromLeft = 0f
    private var selectorFromWidth = 0f
    private var selectorToLeft = 0f
    private var selectorToWidth = 0f
    private var selectedIndex = 0

    init {
        val barPadding = resources.getDimensionPixelSize(R.dimen.tab_bar_padding)
        setPadding(barPadding, barPadding, barPadding, barPadding)
        clipToPadding = false
        clipChildren = false
        selector.setBackgroundResource(R.drawable.tab_selector)
        selector.alpha = IDLE_SELECTOR_ALPHA
        addView(selector, LayoutParams(0, 0))
        tabRow.orientation = LinearLayout.HORIZONTAL
        addView(tabRow, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        selectorAnimator.duration = SELECTOR_ANIMATION_MS
        selectorAnimator.interpolator = appleEaseOut
        selectorAnimator.addUpdateListener(::onSelectorFrame)
    }

    fun setTabs(tabs: List<TabSpec>) {
        tabs.forEach { tab -> tabRow.addView(createTab(tab)) }
        trapHorizontalFocus()
        highlightSelectedTab()
    }

    override fun addFocusables(views: ArrayList<View>, direction: Int, focusableMode: Int) {
        if (direction != FOCUS_UP) return super.addFocusables(views, direction, focusableMode)
        tabRow.getChildAt(selectedIndex)?.addFocusables(views, direction, focusableMode)
    }

    fun focusSelectedTab() {
        tabRow.getChildAt(selectedIndex)?.requestFocus()
    }

    override fun onFocusChange(view: View, hasFocus: Boolean) {
        val alpha = if (hasFocus) FOCUSED_SELECTOR_ALPHA else IDLE_SELECTOR_ALPHA
        selector.animate().alpha(alpha).setDuration(EMPHASIS_ANIMATION_MS).start()
        if (hasFocus) selectTab(tabRow.indexOfChild(view))
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (!selectorAnimator.isRunning) moveSelector(animate = false)
    }

    private fun createTab(tab: TabSpec) = TextView(context).apply {
        id = View.generateViewId()
        text = tab.title
        gravity = Gravity.CENTER
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.tab_text_size))
        setTextColor(context.getColor(R.color.text_primary))
        typeface = Typeface.create(TAB_FONT_FAMILY, Typeface.NORMAL)
        val iconSize = resources.getDimensionPixelSize(R.dimen.tab_icon_size)
        val icon = requireNotNull(context.getDrawable(tab.iconRes))
        icon.setBounds(0, 0, iconSize, iconSize)
        setCompoundDrawablesRelative(icon, null, null, null)
        compoundDrawablePadding = resources.getDimensionPixelSize(R.dimen.tab_icon_padding)
        setPadding(
            resources.getDimensionPixelSize(R.dimen.tab_horizontal_padding),
            resources.getDimensionPixelSize(R.dimen.tab_vertical_padding),
            resources.getDimensionPixelSize(R.dimen.tab_horizontal_padding),
            resources.getDimensionPixelSize(R.dimen.tab_vertical_padding),
        )
        isFocusable = true
        onFocusChangeListener = this@GlassTabBar
    }

    private fun trapHorizontalFocus() {
        val firstTab = tabRow.getChildAt(0) ?: return
        val lastTab = tabRow.getChildAt(tabRow.childCount - 1)
        firstTab.nextFocusLeftId = firstTab.id
        lastTab.nextFocusRightId = lastTab.id
    }

    private fun selectTab(index: Int) {
        if (index == selectedIndex) return
        selectedIndex = index
        highlightSelectedTab()
        moveSelector(animate = true)
        onTabSelected?.invoke(index)
    }

    private fun highlightSelectedTab() {
        for (index in 0 until tabRow.childCount) {
            val alpha = if (index == selectedIndex) SELECTED_TAB_ALPHA else UNSELECTED_TAB_ALPHA
            tabRow.getChildAt(index).animate().alpha(alpha).setDuration(EMPHASIS_ANIMATION_MS).start()
        }
    }

    private fun moveSelector(animate: Boolean) {
        val tab = tabRow.getChildAt(selectedIndex) ?: return
        selectorFromLeft = selector.left.toFloat()
        selectorFromWidth = selector.width.toFloat()
        selectorToLeft = (tabRow.left + tab.left).toFloat()
        selectorToWidth = tab.width.toFloat()
        selectorAnimator.cancel()
        if (animate) selectorAnimator.start() else applySelector(FULL_FRACTION)
    }

    private fun onSelectorFrame(animator: ValueAnimator) = applySelector(animator.animatedFraction)

    private fun applySelector(fraction: Float) {
        val left = (selectorFromLeft + (selectorToLeft - selectorFromLeft) * fraction).roundToInt()
        val width = (selectorFromWidth + (selectorToWidth - selectorFromWidth) * fraction).roundToInt()
        val params = selector.layoutParams
        params.width = width
        params.height = tabRow.height
        selector.layout(left, tabRow.top, left + width, tabRow.bottom)
    }
}
