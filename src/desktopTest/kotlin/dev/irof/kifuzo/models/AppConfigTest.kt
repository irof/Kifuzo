package dev.irof.kifuzo.models

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AppConfigTest {
    @Test
    fun AppConfigの定数が正しく取得できること() {
        assertNotNull(AppConfig.USER_HOME)
        assertNotNull(AppConfig.USER_HOME_PATH)
        assertTrue(AppConfig.DEFAULT_SIDEBAR_WIDTH > 0)
        assertTrue(AppConfig.MIN_SIDEBAR_WIDTH > 0)
        assertTrue(AppConfig.MAX_SIDEBAR_WIDTH > AppConfig.MIN_SIDEBAR_WIDTH)
        assertTrue(AppConfig.DEFAULT_WINDOW_WIDTH > 0)
        assertTrue(AppConfig.DEFAULT_WINDOW_HEIGHT > 0)
        assertTrue(AppConfig.MAX_PERMISSION_ERRORS > 0)
    }
}
