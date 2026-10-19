package dev.tommy.tvlauncher

import android.content.Context
import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager

data class PortInfo(val inputId: String, val number: Int)

class PortRepository(private val context: Context) {
    fun loadPorts(): List<PortInfo> {
        val inputManager = context.getSystemService(TvInputManager::class.java) ?: return emptyList()
        return inputManager.tvInputList
            .filter { it.type == TvInputInfo.TYPE_HDMI && it.isPassthroughInput && it.parentId == null }
            .sortedBy { it.id }
            .mapIndexed { index, info -> PortInfo(info.id, index + 1) }
    }
}
