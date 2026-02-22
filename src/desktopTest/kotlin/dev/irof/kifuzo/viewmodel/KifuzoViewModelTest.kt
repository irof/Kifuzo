package dev.irof.kifuzo.viewmodel

import dev.irof.kifuzo.logic.KifuRepository
import dev.irof.kifuzo.models.BoardSnapshot
import dev.irof.kifuzo.models.FileTreeNode
import dev.irof.kifuzo.models.KifuInfo
import dev.irof.kifuzo.models.KifuSession
import dev.irof.kifuzo.models.ShogiBoardState
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Suppress("FunctionName")
class KifuzoViewModelTest {

    private lateinit var viewModel: KifuzoViewModel
    private val stubRepository = StubKifuRepository()

    class StubKifuRepository : KifuRepository {
        var parseAction: (ShogiBoardState) -> Unit = {}

        override fun scanDirectory(directory: Path): List<Path> = emptyList()
        override fun getKifuInfos(files: List<Path>): Map<Path, KifuInfo> = emptyMap()
        override fun parse(path: Path, state: ShogiBoardState) {
            parseAction(state)
        }

        override fun convertCsa(path: Path): Path = path
        override fun updateSenkei(path: Path, senkei: String) {}
        override fun importQuestFiles(sourceDir: Path, targetDir: Path): Int = 0
    }

    @BeforeTest
    fun setup() {
        viewModel = KifuzoViewModel(stubRepository)
    }

    @Test
    fun ファイルを選択すると棋譜が読み込まれセッション情報が更新されること() {
        val path = java.nio.file.Paths.get("test.kifu")
        stubRepository.parseAction = { state ->
            state.updateSession(
                KifuSession(
                    history = listOf(BoardSnapshot(List(9) { List(9) { null } })),
                    senteName = "先手",
                    goteName = "後手",
                ),
            )
        }

        viewModel.dispatch(KifuzoAction.SelectFile(path))

        assertEquals(path, viewModel.uiState.selectedFile)
        assertEquals("先手", viewModel.boardState.session.senteName)
    }

    @Test
    fun 手数移動のアクションで現在のステップが正しく更新されること() {
        // 3手ある棋譜をセット
        viewModel.boardState.updateSession(
            KifuSession(
                history = List(4) { BoardSnapshot(List(9) { List(9) { null } }) }, // 0, 1, 2, 3手
            ),
        )
        viewModel.boardState.currentStep = 0

        viewModel.dispatch(KifuzoAction.NextStep)
        assertEquals(1, viewModel.boardState.currentStep)

        viewModel.dispatch(KifuzoAction.NextStep)
        assertEquals(2, viewModel.boardState.currentStep)

        viewModel.dispatch(KifuzoAction.PrevStep)
        assertEquals(1, viewModel.boardState.currentStep)

        viewModel.dispatch(KifuzoAction.ChangeStep(3))
        assertEquals(3, viewModel.boardState.currentStep)
    }

    @Test
    fun 設定ダイアログの表示状態を切り替えられること() {
        assertFalse(viewModel.uiState.showSettings)

        viewModel.dispatch(KifuzoAction.ShowSettings(true))
        assertTrue(viewModel.uiState.showSettings)

        viewModel.dispatch(KifuzoAction.ShowSettings(false))
        assertFalse(viewModel.uiState.showSettings)
    }

    @Test
    fun 設定を保存すると正規表現が更新されダイアログが閉じること() {
        viewModel.dispatch(KifuzoAction.SaveSettings("MyName"))
        assertEquals("MyName", viewModel.uiState.myNameRegex)
        assertFalse(viewModel.uiState.showSettings)
    }

    @Test
    fun 設定された自分の名前によって盤面が自動的に反転されること() {
        // 自分の名前を "irof" に設定
        viewModel.dispatch(KifuzoAction.SaveSettings("irof"))

        // 後手が "irof" の棋譜を読み込む
        val path = Paths.get("test.kifu")
        stubRepository.parseAction = { state ->
            state.updateSession(
                KifuSession(
                    history = listOf(BoardSnapshot(List(9) { List(9) { null } })),
                    senteName = "相手",
                    goteName = "irof",
                ),
            )
        }

        viewModel.dispatch(KifuzoAction.SelectFile(path))

        // 後手が自分の名前なので、反転されているはず
        assertTrue(viewModel.uiState.isFlipped)

        // 先手が "irof" の棋譜を読み込む
        stubRepository.parseAction = { state ->
            state.updateSession(
                KifuSession(
                    history = listOf(BoardSnapshot(List(9) { List(9) { null } })),
                    senteName = "irof",
                    goteName = "相手",
                ),
            )
        }
        viewModel.dispatch(KifuzoAction.SelectFile(path))

        // 先手が自分なので、反転されていないはず
        assertFalse(viewModel.uiState.isFlipped)
    }
}
