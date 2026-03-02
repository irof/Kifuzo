package dev.irof.kifuzo.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AppSettingsTest {
    @Test
    fun すべてのプロパティの読み書きができること() {
        val settings = AppSettings.Default
        assertNotNull(settings)

        // 元の値を保存
        val originalMyNameRegex = settings.myNameRegex
        val originalWindowX = settings.windowX
        val originalWindowY = settings.windowY
        val originalWindowWidth = settings.windowWidth
        val originalWindowHeight = settings.windowHeight
        val originalSidebarWidth = settings.sidebarWidth
        val originalImportSourceDir = settings.importSourceDir
        val originalLastRootDir = settings.lastRootDir
        val originalFilenameTemplate = settings.filenameTemplate
        val originalFileSortOption = settings.fileSortOption

        try {
            settings.myNameRegex = "test_regex"
            assertEquals("test_regex", settings.myNameRegex)

            settings.windowX = 100f
            assertEquals(100f, settings.windowX)
            settings.windowX = null
            assertNull(settings.windowX)

            settings.windowY = 200f
            assertEquals(200f, settings.windowY)
            settings.windowY = null
            assertNull(settings.windowY)

            settings.windowWidth = 1000f
            assertEquals(1000f, settings.windowWidth)

            settings.windowHeight = 800f
            assertEquals(800f, settings.windowHeight)

            settings.sidebarWidth = 300f
            assertEquals(300f, settings.sidebarWidth)

            settings.importSourceDir = "/tmp/import"
            assertEquals("/tmp/import", settings.importSourceDir)

            settings.lastRootDir = "/tmp/root"
            assertEquals("/tmp/root", settings.lastRootDir)

            settings.filenameTemplate = "template"
            assertEquals("template", settings.filenameTemplate)

            settings.fileSortOption = FileSortOption.LAST_MODIFIED
            assertEquals(FileSortOption.LAST_MODIFIED, settings.fileSortOption)

            settings.putSetting("custom_key", "custom_value")
            assertEquals("custom_value", settings.getAllSettings()["custom_key"])
            settings.removeSetting("custom_key")
            assertNull(settings.getAllSettings()["custom_key"])
        } finally {
            // 元の値を復元
            settings.myNameRegex = originalMyNameRegex
            settings.windowX = originalWindowX
            settings.windowY = originalWindowY
            settings.windowWidth = originalWindowWidth
            settings.windowHeight = originalWindowHeight
            settings.sidebarWidth = originalSidebarWidth
            settings.importSourceDir = originalImportSourceDir
            settings.lastRootDir = originalLastRootDir
            settings.filenameTemplate = originalFilenameTemplate
            settings.fileSortOption = originalFileSortOption
        }
    }
}
