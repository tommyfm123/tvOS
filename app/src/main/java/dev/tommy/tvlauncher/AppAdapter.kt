package dev.tommy.tvlauncher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

private const val FOCUSED_SCALE = 1.1f
private const val IDLE_SCALE = 1f
private const val FOCUSED_ALPHA = 1f
private const val IDLE_LABEL_ALPHA = 0.75f
private const val HIDDEN_ALPHA = 0f
private const val FOCUS_ANIMATION_MS = 180L

class AppAdapter(
    private val itemLayout: Int,
    private val alwaysShowLabel: Boolean,
    private val onTileClick: (Tile) -> Unit,
    private val onTileLongClick: (Tile) -> Unit,
) : RecyclerView.Adapter<AppAdapter.TileViewHolder>() {
    private var tiles: List<Tile> = emptyList()

    fun submit(newTiles: List<Tile>) {
        tiles = newTiles
        notifyDataSetChanged()
    }

    fun keys() = tiles.map { it.key }

    fun indexOfKey(key: String) = tiles.indexOfFirst { it.key == key }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TileViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(itemLayout, parent, false)
        return TileViewHolder(view, alwaysShowLabel, onTileClick, onTileLongClick)
    }

    override fun onBindViewHolder(holder: TileViewHolder, position: Int) = holder.bind(tiles[position])

    override fun getItemCount() = tiles.size

    class TileViewHolder(
        itemView: View,
        private val alwaysShowLabel: Boolean,
        private val onTileClick: (Tile) -> Unit,
        private val onTileLongClick: (Tile) -> Unit,
    ) : RecyclerView.ViewHolder(itemView),
        View.OnFocusChangeListener,
        View.OnClickListener,
        View.OnLongClickListener {
        private val tileImage: ImageView = itemView.findViewById(R.id.tile_image)
        private val tileLabel: TextView = itemView.findViewById(R.id.tile_label)
        private val iconPadding = itemView.resources.getDimensionPixelSize(
            if (alwaysShowLabel) R.dimen.setting_icon_padding else R.dimen.tile_icon_padding,
        )
        private val idleLabelAlpha = if (alwaysShowLabel) IDLE_LABEL_ALPHA else HIDDEN_ALPHA
        private var tile: Tile? = null

        init {
            itemView.onFocusChangeListener = this
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
        }

        fun bind(boundTile: Tile) {
            tile = boundTile
            itemView.tag = boundTile.key
            val padding = if (boundTile.hasBanner) 0 else iconPadding
            tileImage.setBackgroundResource(backgroundFor(boundTile))
            tileImage.setPadding(padding, padding, padding, padding)
            tileImage.scaleType = if (boundTile.hasBanner) ImageView.ScaleType.CENTER_CROP else ImageView.ScaleType.FIT_CENTER
            tileImage.setImageDrawable(boundTile.tileImage.constantState?.newDrawable() ?: boundTile.tileImage)
            tileLabel.text = boundTile.label
            tileLabel.alpha = idleLabelAlpha
            itemView.scaleX = IDLE_SCALE
            itemView.scaleY = IDLE_SCALE
            itemView.isSelected = false
            tileLabel.scaleX = IDLE_SCALE
            tileLabel.scaleY = IDLE_SCALE
            tileLabel.translationY = 0f
        }

        private fun backgroundFor(boundTile: Tile) = when {
            alwaysShowLabel -> R.drawable.setting_card_background
            boundTile.hasBanner -> R.drawable.tile_background_banner
            else -> R.drawable.tile_background
        }

        override fun onFocusChange(view: View, hasFocus: Boolean) {
            view.isSelected = hasFocus
            val scale = if (hasFocus) FOCUSED_SCALE else IDLE_SCALE
            val labelAlpha = if (hasFocus) FOCUSED_ALPHA else idleLabelAlpha
            val labelOffset = tileLabel.top + tileLabel.height / 2f - view.height / 2f
            val labelShift = if (hasFocus) -(FOCUSED_SCALE - IDLE_SCALE) * labelOffset else 0f
            val labelScale = IDLE_SCALE / scale
            view.animate().scaleX(scale).scaleY(scale)
                .setDuration(FOCUS_ANIMATION_MS).setInterpolator(appleEaseOut).start()
            tileLabel.animate().alpha(labelAlpha).translationY(labelShift).scaleX(labelScale).scaleY(labelScale)
                .setDuration(FOCUS_ANIMATION_MS).setInterpolator(appleEaseOut).start()
        }

        override fun onClick(view: View) {
            tile?.let(onTileClick)
        }

        override fun onLongClick(view: View): Boolean {
            tile?.let(onTileLongClick)
            return true
        }
    }
}
