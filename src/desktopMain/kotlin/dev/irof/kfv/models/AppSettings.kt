package dev.irof.kfv.models

object AppSettings {
    private val prefs = java.util.prefs.Preferences.userNodeForPackage(AppSettings::class.java)
    private const val KEY_MY_NAME_REGEX = "my_name_regex"
    private const val KEY_WINDOW_X = "window_x"
    private const val KEY_WINDOW_Y = "window_y"
    private const val KEY_WINDOW_WIDTH = "window_width"
    private const val KEY_WINDOW_HEIGHT = "window_height"

    var myNameRegex: String
        get() = prefs.get(KEY_MY_NAME_REGEX, "")
        set(value) = prefs.put(KEY_MY_NAME_REGEX, value)

    var windowX: Float?
        get() = if (prefs.get(KEY_WINDOW_X, null) == null) null else prefs.getFloat(KEY_WINDOW_X, 0f)
        set(value) = if (value == null) prefs.remove(KEY_WINDOW_X) else prefs.putFloat(KEY_WINDOW_X, value)

    var windowY: Float?
        get() = if (prefs.get(KEY_WINDOW_Y, null) == null) null else prefs.getFloat(KEY_WINDOW_Y, 0f)
        set(value) = if (value == null) prefs.remove(KEY_WINDOW_Y) else prefs.putFloat(KEY_WINDOW_Y, value)

    var windowWidth: Float
        get() = prefs.getFloat(KEY_WINDOW_WIDTH, 800f)
        set(value) = prefs.putFloat(KEY_WINDOW_WIDTH, value)

    var windowHeight: Float
        get() = prefs.getFloat(KEY_WINDOW_HEIGHT, 750f)
        set(value) = prefs.putFloat(KEY_WINDOW_HEIGHT, value)

    fun getAllSettings(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        prefs.keys().forEach { key -> map[key] = prefs.get(key, "") }
        return map
    }
    fun removeSetting(key: String) { prefs.remove(key) }
    fun putSetting(key: String, value: String) { prefs.put(key, value) }
}
