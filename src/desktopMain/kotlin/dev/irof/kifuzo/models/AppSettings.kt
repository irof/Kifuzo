package dev.irof.kifuzo.models

object AppSettings {
    private val prefs = java.util.prefs.Preferences.userRoot().node("dev/irof/kifuzo")
    private const val KEY_MY_NAME_REGEX = "my_name_regex"
    private const val KEY_WINDOW_X = "window_x"
    private const val KEY_WINDOW_Y = "window_y"
    private const val KEY_WINDOW_WIDTH = "window_width"
    private const val KEY_WINDOW_HEIGHT = "window_height"

    var myNameRegex: String
        get() = prefs.get(KEY_MY_NAME_REGEX, "")
        set(value) {
            prefs.put(KEY_MY_NAME_REGEX, value)
            prefs.flush()
        }

    var windowX: Float?
        get() = if (prefs.get(KEY_WINDOW_X, null) == null) null else prefs.getFloat(KEY_WINDOW_X, 0f)
        set(value) {
            if (value == null) prefs.remove(KEY_WINDOW_X) else prefs.putFloat(KEY_WINDOW_X, value)
            prefs.flush()
        }

    var windowY: Float?
        get() = if (prefs.get(KEY_WINDOW_Y, null) == null) null else prefs.getFloat(KEY_WINDOW_Y, 0f)
        set(value) {
            if (value == null) prefs.remove(KEY_WINDOW_Y) else prefs.putFloat(KEY_WINDOW_Y, value)
            prefs.flush()
        }

    var windowWidth: Float
        get() = prefs.getFloat(KEY_WINDOW_WIDTH, AppConfig.DEFAULT_WINDOW_WIDTH)
        set(value) {
            prefs.putFloat(KEY_WINDOW_WIDTH, value)
            prefs.flush()
        }

    var windowHeight: Float
        get() = prefs.getFloat(KEY_WINDOW_HEIGHT, AppConfig.DEFAULT_WINDOW_HEIGHT)
        set(value) {
            prefs.putFloat(KEY_WINDOW_HEIGHT, value)
            prefs.flush()
        }

    private const val KEY_SIDEBAR_WIDTH = "sidebar_width"
    var sidebarWidth: Float
        get() = prefs.getFloat(KEY_SIDEBAR_WIDTH, AppConfig.DEFAULT_SIDEBAR_WIDTH)
        set(value) {
            prefs.putFloat(KEY_SIDEBAR_WIDTH, value)
            prefs.flush()
        }

    private const val KEY_IMPORT_SOURCE_DIR = "import_source_dir"
    var importSourceDir: String
        get() = prefs.get(KEY_IMPORT_SOURCE_DIR, "")
        set(value) {
            prefs.put(KEY_IMPORT_SOURCE_DIR, value)
            prefs.flush()
        }

    private const val KEY_LAST_ROOT_DIR = "last_root_dir"
    var lastRootDir: String
        get() = prefs.get(KEY_LAST_ROOT_DIR, "")
        set(value) {
            prefs.put(KEY_LAST_ROOT_DIR, value)
            prefs.flush()
        }

    private const val KEY_FILENAME_TEMPLATE = "kifu_filename_template"
    var filenameTemplate: String
        get() = prefs.get(KEY_FILENAME_TEMPLATE, "{YYYYMMDD}-{Sente}-{Gote}")
        set(value) {
            prefs.put(KEY_FILENAME_TEMPLATE, value)
            prefs.flush()
        }

    private const val KEY_FILE_SORT_OPTION = "file_sort_option"
    var fileSortOption: FileSortOption
        get() = try {
            FileSortOption.valueOf(prefs.get(KEY_FILE_SORT_OPTION, FileSortOption.NAME.name))
        } catch (e: Exception) {
            FileSortOption.NAME
        }
        set(value) {
            prefs.put(KEY_FILE_SORT_OPTION, value.name)
            prefs.flush()
        }

    fun getAllSettings(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        prefs.keys().forEach { key -> map[key] = prefs.get(key, "") }
        return map
    }
    fun removeSetting(key: String) {
        prefs.remove(key)
        prefs.flush()
    }
    fun putSetting(key: String, value: String) {
        prefs.put(key, value)
        prefs.flush()
    }
}
