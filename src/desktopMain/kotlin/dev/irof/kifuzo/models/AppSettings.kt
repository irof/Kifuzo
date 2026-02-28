package dev.irof.kifuzo.models

import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * アプリケーションの設定を保持するインターフェース。
 */
interface AppSettings {
    var myNameRegex: String
    var windowX: Float?
    var windowY: Float?
    var windowWidth: Float
    var windowHeight: Float
    var sidebarWidth: Float
    var importSourceDir: String
    var lastRootDir: String
    var filenameTemplate: String
    var fileSortOption: FileSortOption

    fun saveWindowState(windowState: WindowState)
    fun getAllSettings(): Map<String, String>
    fun removeSetting(key: String)
    fun putSetting(key: String, value: String)

    companion object {
        /**
         * デフォルトの実装。実際の永続化（Preferences）を行います。
         */
        val Default: AppSettings = StandardAppSettings()
    }
}

private const val KEY_MY_NAME_REGEX = "my_name_regex"
private const val KEY_WINDOW_X = "window_x"
private const val KEY_WINDOW_Y = "window_y"
private const val KEY_WINDOW_WIDTH = "window_width"
private const val KEY_WINDOW_HEIGHT = "window_height"
private const val KEY_SIDEBAR_WIDTH = "sidebar_width"
private const val KEY_IMPORT_SOURCE_DIR = "import_source_dir"
private const val KEY_LAST_ROOT_DIR = "last_root_dir"
private const val KEY_FILENAME_TEMPLATE = "kifu_filename_template_v2"
private const val KEY_FILE_SORT_OPTION = "file_sort_option"

private class StandardAppSettings : AppSettings {
    private val logger = KotlinLogging.logger {}
    private val prefs = java.util.prefs.Preferences.userRoot().node("dev/irof/kifuzo")

    override fun saveWindowState(windowState: WindowState) {
        windowX = windowState.position.let { if (it is WindowPosition.Absolute) it.x.value else null }
        windowY = windowState.position.let { if (it is WindowPosition.Absolute) it.y.value else null }
        windowWidth = windowState.size.width.value
        windowHeight = windowState.size.height.value
    }

    override var myNameRegex: String
        get() = prefs.get(KEY_MY_NAME_REGEX, "")
        set(value) {
            prefs.put(KEY_MY_NAME_REGEX, value)
            prefs.flush()
        }

    override var windowX: Float?
        get() = if (prefs.get(KEY_WINDOW_X, null) == null) null else prefs.getFloat(KEY_WINDOW_X, 0f)
        set(value) {
            if (value == null) prefs.remove(KEY_WINDOW_X) else prefs.putFloat(KEY_WINDOW_X, value)
            prefs.flush()
        }

    override var windowY: Float?
        get() = if (prefs.get(KEY_WINDOW_Y, null) == null) null else prefs.getFloat(KEY_WINDOW_Y, 0f)
        set(value) {
            if (value == null) prefs.remove(KEY_WINDOW_Y) else prefs.putFloat(KEY_WINDOW_Y, value)
            prefs.flush()
        }

    override var windowWidth: Float
        get() = prefs.getFloat(KEY_WINDOW_WIDTH, AppConfig.DEFAULT_WINDOW_WIDTH)
        set(value) {
            prefs.putFloat(KEY_WINDOW_WIDTH, value)
            prefs.flush()
        }

    override var windowHeight: Float
        get() = prefs.getFloat(KEY_WINDOW_HEIGHT, AppConfig.DEFAULT_WINDOW_HEIGHT)
        set(value) {
            prefs.putFloat(KEY_WINDOW_HEIGHT, value)
            prefs.flush()
        }

    override var sidebarWidth: Float
        get() = prefs.getFloat(KEY_SIDEBAR_WIDTH, AppConfig.DEFAULT_SIDEBAR_WIDTH)
        set(value) {
            prefs.putFloat(KEY_SIDEBAR_WIDTH, value)
            prefs.flush()
        }

    override var importSourceDir: String
        get() = prefs.get(KEY_IMPORT_SOURCE_DIR, "")
        set(value) {
            prefs.put(KEY_IMPORT_SOURCE_DIR, value)
            prefs.flush()
        }

    override var lastRootDir: String
        get() = prefs.get(KEY_LAST_ROOT_DIR, System.getProperty("user.home"))
        set(value) {
            prefs.put(KEY_LAST_ROOT_DIR, value)
            prefs.flush()
        }

    override var filenameTemplate: String
        get() = prefs.get(KEY_FILENAME_TEMPLATE, "{開始日の年月日}_{開始日の時分秒}_{棋戦名}_{先手}_{後手}")
        set(value) {
            prefs.put(KEY_FILENAME_TEMPLATE, value)
            prefs.flush()
        }

    override var fileSortOption: FileSortOption
        get() = try {
            FileSortOption.valueOf(prefs.get(KEY_FILE_SORT_OPTION, FileSortOption.NAME.name))
        } catch (e: IllegalArgumentException) {
            logger.warn(e) { "Failed to parse file sort option, defaulting to NAME" }
            FileSortOption.NAME
        }
        set(value) {
            prefs.put(KEY_FILE_SORT_OPTION, value.name)
            prefs.flush()
        }

    override fun getAllSettings(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        prefs.keys().forEach { key -> map[key] = prefs.get(key, "") }
        return map
    }

    override fun removeSetting(key: String) {
        prefs.remove(key)
        prefs.flush()
    }

    override fun putSetting(key: String, value: String) {
        prefs.put(key, value)
        prefs.flush()
    }
}
