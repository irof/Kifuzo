package dev.irof.kifuzo.viewmodel

import dev.irof.kifuzo.InMemoryAppSettings
import dev.irof.kifuzo.StubKifuRepository
import dev.irof.kifuzo.models.BoardSnapshot
import dev.irof.kifuzo.models.FileViewMode
import dev.irof.kifuzo.models.KifuSession
import dev.irof.kifuzo.models.Move
import java.nio.file.Paths
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KifuzoViewModelTest {

    private lateinit var viewModel: KifuzoViewModel
    private val stubRepository = StubKifuRepository()
    private val inMemorySettings = InMemoryAppSettings()

    @BeforeTest
    fun setup() {
        viewModel = KifuzoViewModel(stubRepository, inMemorySettings)
    }

    @Test
    fun ルートディレクトリを設定すると現在のディレクトリが更新されること() {
        val root = Paths.get(".")
        viewModel.dispatch(KifuzoAction.SetRootDirectory(root))
        assertEquals(root, viewModel.currentRootDirectory)
    }

    @Test
    fun ファイルを選択すると棋譜が読み込まれセッション情報が更新されること() {
        val path = Paths.get("test.kifu")
        stubRepository.parseAction = { state ->
            state.updateSession(
                KifuSession(
                    initialSnapshot = BoardSnapshot(List(9) { List(9) { null } }),
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
        val snapshot = BoardSnapshot(List(9) { List(9) { null } })
        viewModel.boardState.updateSession(
            KifuSession(
                initialSnapshot = snapshot,
                moves = List(3) { i -> Move(i + 1, "${i + 1} test", snapshot) },
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
        viewModel.dispatch(KifuzoAction.SaveSettings("MyName", "{Sente}"))
        assertEquals("MyName", viewModel.uiState.myNameRegex)
        assertFalse(viewModel.uiState.showSettings)
    }

    @Test
    fun 設定された自分の名前によって盤面が自動的に反転されること() {
        // 自分の名前を "irof" に設定
        viewModel.dispatch(KifuzoAction.SaveSettings("irof", "{Sente}"))

        // 後手が "irof" の棋譜を読み込む
        val path = Paths.get("test.kifu")
        stubRepository.parseAction = { state ->
            state.updateSession(
                KifuSession(
                    initialSnapshot = BoardSnapshot(List(9) { List(9) { null } }),
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
                    initialSnapshot = BoardSnapshot(List(9) { List(9) { null } }),
                    senteName = "irof",
                    goteName = "相手",
                ),
            )
        }
        viewModel.dispatch(KifuzoAction.SelectFile(path))

        // 先手が自分なので、反転されていないはず
        assertFalse(viewModel.uiState.isFlipped)
    }

    @Test
    fun サイドバーの表示状態を切り替えられること() {
        assertTrue(viewModel.uiState.isSidebarVisible)

        viewModel.dispatch(KifuzoAction.ToggleSidebar)
        assertFalse(viewModel.uiState.isSidebarVisible)

        viewModel.dispatch(KifuzoAction.ToggleSidebar)
        assertTrue(viewModel.uiState.isSidebarVisible)
    }

    @Test
    fun 手順パネルの表示状態を切り替えられること() {
        assertTrue(viewModel.uiState.isMoveListVisible)

        viewModel.dispatch(KifuzoAction.ToggleMoveList)
        assertFalse(viewModel.uiState.isMoveListVisible)

        viewModel.dispatch(KifuzoAction.ToggleMoveList)
        assertTrue(viewModel.uiState.isMoveListVisible)
    }

    @Test
    fun ビューモードを切り替えると更新されること() {
        assertEquals(FileViewMode.HIERARCHY, viewModel.uiState.viewMode)

        viewModel.dispatch(KifuzoAction.SetViewMode(FileViewMode.FLAT))
        assertEquals(FileViewMode.FLAT, viewModel.uiState.viewMode)

        viewModel.dispatch(KifuzoAction.SetViewMode(FileViewMode.HIERARCHY))
        assertEquals(FileViewMode.HIERARCHY, viewModel.uiState.viewMode)
    }

    @Test
    fun エラーメッセージと通知情報をクリアできること() {
        // 内部的にセットする方法がないので、Action経由で発生させる必要があるが、
        // ここでは直接 UiState が更新されるアクション(ClearErrorAndInfo)の挙動のみ確認
        viewModel.dispatch(KifuzoAction.ClearErrorAndInfo)
        assertEquals(null, viewModel.uiState.errorMessage)
        assertEquals(null, viewModel.uiState.infoMessage)
    }

    @Test
    fun インポートダイアログの表示状態を切り替えられること() {
        assertFalse(viewModel.uiState.showImportDialog)
        viewModel.dispatch(KifuzoAction.ShowImportDialog(true))
        assertTrue(viewModel.uiState.showImportDialog)
    }

    @Test
    fun リネームダイアログの表示状態と対象パスを制御できること() {
        val path = Paths.get("test.kifu")
        assertEquals(null, viewModel.uiState.renameTarget)

        viewModel.dispatch(KifuzoAction.ShowRenameDialog(path))
        assertEquals(path, viewModel.uiState.renameTarget)

        viewModel.dispatch(KifuzoAction.HideRenameDialog)
        assertEquals(null, viewModel.uiState.renameTarget)
    }

    @Test
    fun メタデータ編集ダイアログの表示状態を制御できること() {
        val path = Paths.get("test.kifu")
        assertEquals(null, viewModel.uiState.editMetadataTarget)

        viewModel.dispatch(KifuzoAction.ShowEditMetadataDialog(path))
        assertEquals(path, viewModel.uiState.editMetadataTarget)

        viewModel.dispatch(KifuzoAction.HideEditMetadataDialog)
        assertEquals(null, viewModel.uiState.editMetadataTarget)
    }

    @Test
    fun ソート順の設定を更新できること() {
        val option = dev.irof.kifuzo.models.FileSortOption.NAME
        viewModel.dispatch(KifuzoAction.SetFileSortOption(option))
        assertEquals(option, viewModel.uiState.fileSortOption)
    }

    @Test
    fun ファイルフィルタを切り替えられること() {
        val filter = dev.irof.kifuzo.models.FileFilter.RECENT
        // 初期状態では含まれていないはず
        assertFalse(viewModel.uiState.fileFilters.contains(filter))

        viewModel.dispatch(KifuzoAction.ToggleFileFilter(filter))
        assertTrue(viewModel.uiState.fileFilters.contains(filter))

        viewModel.dispatch(KifuzoAction.ToggleFileFilter(filter))
        assertFalse(viewModel.uiState.fileFilters.contains(filter))
    }
}
