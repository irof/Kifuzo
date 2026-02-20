package dev.irof.kfv.viewmodel

import dev.irof.kfv.logic.*
import dev.irof.kfv.models.*
import java.nio.file.Path
import kotlin.test.*

class KifuManagerViewModelTest {

    private lateinit var viewModel: KifuManagerViewModel
    private var parseResultAction: (ShogiBoardState) -> Unit = {}

    @BeforeTest
    fun setup() {
        val stubRepository = object : KifuRepository {
            override fun scanDirectory(directory: Path): List<Path> = emptyList()
            override fun getKifuInfos(files: List<Path>): Map<Path, KifuInfo> = emptyMap()
            override fun parse(path: Path, state: ShogiBoardState) {
                parseResultAction(state)
            }

            override fun convertCsa(path: Path): Path = path
            override fun updateSenkei(path: Path, senkei: String) {}
            override fun importQuestFiles(sourceDir: Path, targetDir: Path): Int = 0
        }
        viewModel = KifuManagerViewModel(stubRepository)
    }

    @Test
    fun testSelectFile() {
        val path = java.nio.file.Paths.get("test.kifu")
        parseResultAction = { state ->
            state.updateSession(
                KifuSession(
                    history = listOf(BoardSnapshot(List(9) { List(9) { null } })),
                    senteName = "先手",
                    goteName = "後手",
                ),
            )
        }

        viewModel.dispatch(KifuManagerAction.SelectFile(path))

        assertEquals(path, viewModel.uiState.selectedFile)
        assertEquals("先手", viewModel.boardState.session.senteName)
    }

    @Test
    fun testStepNavigation() {
        // 3手ある棋譜をセット
        viewModel.boardState.updateSession(
            KifuSession(
                history = List(4) { BoardSnapshot(List(9) { List(9) { null } }) }, // 0, 1, 2, 3手
            ),
        )
        viewModel.boardState.currentStep = 0

        viewModel.dispatch(KifuManagerAction.NextStep)
        assertEquals(1, viewModel.boardState.currentStep)

        viewModel.dispatch(KifuManagerAction.NextStep)
        assertEquals(2, viewModel.boardState.currentStep)

        viewModel.dispatch(KifuManagerAction.PrevStep)
        assertEquals(1, viewModel.boardState.currentStep)

        viewModel.dispatch(KifuManagerAction.ChangeStep(3))
        assertEquals(3, viewModel.boardState.currentStep)
    }

    @Test
    fun testToggleSettings() {
        assertFalse(viewModel.uiState.showSettings)

        viewModel.dispatch(KifuManagerAction.ShowSettings(true))
        assertTrue(viewModel.uiState.showSettings)

        viewModel.dispatch(KifuManagerAction.ShowSettings(false))
        assertFalse(viewModel.uiState.showSettings)
    }

    @Test
    fun testSaveSettings() {
        viewModel.dispatch(KifuManagerAction.SaveSettings("MyName"))
        assertEquals("MyName", viewModel.uiState.myNameRegex)
        assertFalse(viewModel.uiState.showSettings)
    }
}
