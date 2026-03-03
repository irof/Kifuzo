package dev.irof.kifuzo.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import dev.irof.kifuzo.InMemoryAppSettings
import dev.irof.kifuzo.KifuzoApp
import dev.irof.kifuzo.StubKifuRepository
import dev.irof.kifuzo.utils.AppStrings
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

        composeTestRule.onNodeWithContentDescription(AppStrings.TOGGLE_SIDEBAR).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(AppStrings.TOGGLE_MOVE_LIST).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(AppStrings.IMPORT_KIFU).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(AppStrings.PASTE_KIFU).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(AppStrings.SETTINGS).assertIsDisplayed()
    }

    @Test
    fun サイドバーの表示切り替えボタンで表示非表示が切り替わること() {
        val viewModel = createTestViewModel()

        composeTestRule.setContent {
            KifuzoApp(viewModel)
        }

        // 初期状態では表示されているはず
        composeTestRule.onNodeWithTag(UiId.SIDEBAR).assertIsDisplayed()

        // クリックして非表示にする
        composeTestRule.onNodeWithContentDescription(AppStrings.TOGGLE_SIDEBAR).performClick()
        composeTestRule.onNodeWithTag(UiId.SIDEBAR).assertDoesNotExist()

        // もう一度クリックして表示する
        composeTestRule.onNodeWithContentDescription(AppStrings.TOGGLE_SIDEBAR).performClick()
        composeTestRule.onNodeWithTag(UiId.SIDEBAR).assertIsDisplayed()
    }
}
