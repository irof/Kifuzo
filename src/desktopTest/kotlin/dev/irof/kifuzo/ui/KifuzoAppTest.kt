package dev.irof.kifuzo.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import dev.irof.kifuzo.InMemoryAppSettings
import dev.irof.kifuzo.KifuzoApp
import dev.irof.kifuzo.StubKifuRepository
import dev.irof.kifuzo.viewmodel.KifuzoViewModel
import org.junit.Rule
import org.junit.Test
import java.nio.file.Files

class KifuzoAppTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createTestViewModel(): KifuzoViewModel {
        val tempDir = Files.createTempDirectory("kifuzo-test")
        val settings = InMemoryAppSettings().apply {
            lastRootDir = tempDir.toString()
        }
        return KifuzoViewModel(repository = StubKifuRepository(), settings = settings)
    }

    @Test
    fun アプリ起動時にメニューバーが表示されていること() {
        val viewModel = createTestViewModel()

        composeTestRule.setContent {
            KifuzoApp(viewModel)
        }

        composeTestRule.onNodeWithTag(UiTestTags.MENU_BAR_TOGGLE_SIDEBAR).assertIsDisplayed()
        composeTestRule.onNodeWithTag(UiTestTags.MENU_BAR_TOGGLE_MOVE_LIST).assertIsDisplayed()
        composeTestRule.onNodeWithTag(UiTestTags.MENU_BAR_IMPORT).assertIsDisplayed()
        composeTestRule.onNodeWithTag(UiTestTags.MENU_BAR_PASTE).assertIsDisplayed()
        composeTestRule.onNodeWithTag(UiTestTags.MENU_BAR_SETTINGS).assertIsDisplayed()
    }

    @Test
    fun サイドバーの表示切り替えボタンで表示非表示が切り替わること() {
        val viewModel = createTestViewModel()

        composeTestRule.setContent {
            KifuzoApp(viewModel)
        }

        // 初期状態では表示されているはず
        composeTestRule.onNodeWithTag(UiTestTags.SIDEBAR).assertIsDisplayed()

        // クリックして非表示にする
        composeTestRule.onNodeWithTag(UiTestTags.MENU_BAR_TOGGLE_SIDEBAR).performClick()
        composeTestRule.onNodeWithTag(UiTestTags.SIDEBAR).assertDoesNotExist()

        // もう一度クリックして表示する
        composeTestRule.onNodeWithTag(UiTestTags.MENU_BAR_TOGGLE_SIDEBAR).performClick()
        composeTestRule.onNodeWithTag(UiTestTags.SIDEBAR).assertIsDisplayed()
    }
}
