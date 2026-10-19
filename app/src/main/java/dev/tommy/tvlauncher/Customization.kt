package dev.tommy.tvlauncher

import android.content.Context

private const val PREFERENCES_NAME = "customization"
private const val HIDDEN_KEYS_PREFERENCE = "hidden_keys"
private const val ORDER_PREFERENCE = "order"
private const val ART_PREFERENCE_PREFIX = "art:"
private const val LABEL_PREFERENCE_PREFIX = "label:"
private const val ORDER_SEPARATOR = "\n"

class Customization(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun hiddenKeys(): Set<String> = preferences.getStringSet(HIDDEN_KEYS_PREFERENCE, emptySet()).orEmpty().toSet()

    fun setHidden(key: String, hidden: Boolean) {
        val updatedKeys = if (hidden) hiddenKeys() + key else hiddenKeys() - key
        preferences.edit().putStringSet(HIDDEN_KEYS_PREFERENCE, updatedKeys).apply()
    }

    fun order(): List<String> = preferences.getString(ORDER_PREFERENCE, "").orEmpty()
        .split(ORDER_SEPARATOR)
        .filter { it.isNotEmpty() }

    fun saveOrder(keys: List<String>) {
        preferences.edit().putString(ORDER_PREFERENCE, keys.joinToString(ORDER_SEPARATOR)).apply()
    }

    fun artFor(key: String): String? = preferences.getString(ART_PREFERENCE_PREFIX + key, null)

    fun setArt(key: String, artId: String?) = setOrRemove(ART_PREFERENCE_PREFIX + key, artId)

    fun labelFor(key: String): String? = preferences.getString(LABEL_PREFERENCE_PREFIX + key, null)

    fun setLabel(key: String, label: String?) = setOrRemove(LABEL_PREFERENCE_PREFIX + key, label?.trim()?.ifEmpty { null })

    private fun setOrRemove(preferenceKey: String, value: String?) {
        val editor = preferences.edit()
        if (value == null) editor.remove(preferenceKey) else editor.putString(preferenceKey, value)
        editor.apply()
    }
}
