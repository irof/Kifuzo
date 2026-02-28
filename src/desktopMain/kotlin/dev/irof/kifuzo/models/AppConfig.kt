package dev.irof.kifuzo.models

import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.div

object AppConfig {
    val USER_HOME: String = System.getProperty("user.home")
    val USER_HOME_PATH: Path = Path(USER_HOME)

    // サイドバー設定
    const val DEFAULT_SIDEBAR_WIDTH = 250f
    const val MIN_SIDEBAR_WIDTH = 150f
    const val MAX_SIDEBAR_WIDTH = 600f

    // ウィンドウ設定
    const val DEFAULT_WINDOW_WIDTH = 800f
    const val DEFAULT_WINDOW_HEIGHT = 750f

    // 走査エラー設定
    const val MAX_PERMISSION_ERRORS = 3
}
