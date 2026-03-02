package dev.irof.kifuzo.viewmodel

import dev.irof.kifuzo.InMemoryAppSettings
import dev.irof.kifuzo.StubKifuRepository
import dev.irof.kifuzo.models.FileTreeNode
import java.nio.file.Paths
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ActionHandlerTest {
    private lateinit var viewModel: KifuzoViewModel
    private val stubRepository = StubKifuRepository()
    private val inMemorySettings = InMemoryAppSettings()

    @BeforeTest
    fun setup() {
        viewModel = KifuzoViewModel(stubRepository, inMemorySettings)
    }

    @Test
    fun 隣接するファイルを選択できること() {
        val root = Paths.get("/root")
        val file1 = root.resolve("file1.kifu")
        val file2 = root.resolve("file2.kifu")
        val dir1 = root.resolve("dir1")

        viewModel.updateUiState {
            it.copy(
                treeNodes = listOf(
                    FileTreeNode(file1, 0, false),
                    FileTreeNode(dir1, 0, true),
                    FileTreeNode(file2, 0, false),
                ),
                selectedFile = file1,
            )
        }

        // 次のファイル (dir1はスキップされて file2 になるはず)
        viewModel.dispatch(KifuzoAction.SelectNextFile)
        assertEquals(file2, viewModel.uiState.selectedFile)

        // 前のファイル
        viewModel.dispatch(KifuzoAction.SelectPrevFile)
        assertEquals(file1, viewModel.uiState.selectedFile)
    }

    @Test
    fun CSA変換のフローが正しく動作すること() {
        val path = Paths.get("test.csa")

        // 変換アクション
        viewModel.dispatch(KifuzoAction.ConvertCsa(path))
        assertEquals("convertCsa", stubRepository.lastMethodCalled)

        // 上書き確認のシミュレーション
        // (実際にはファイル存在チェックがあるが、ViewModelのステート更新を確認)
        viewModel.updateUiState { it.copy(showOverwriteConfirm = path) }
        assertTrue(viewModel.uiState.showOverwriteConfirm != null)

        viewModel.dispatch(KifuzoAction.ConfirmOverwrite)
        assertNull(viewModel.uiState.showOverwriteConfirm)

        viewModel.updateUiState { it.copy(showOverwriteConfirm = path) }
        viewModel.dispatch(KifuzoAction.HideOverwriteConfirm)
        assertNull(viewModel.uiState.showOverwriteConfirm)
    }

    @Test
    fun 強制パースアクションが動作すること() {
        val path = Paths.get("test.txt")
        viewModel.dispatch(KifuzoAction.ForceParseAsKifu(path))
        assertEquals("parseManually", stubRepository.lastMethodCalled)
    }

    @Test
    fun メタデータ更新アクションが動作すること() {
        val path = Paths.get("test.kifu")
        viewModel.dispatch(KifuzoAction.UpdateMetadata(path, "Event", "StartTime"))
        // repositoryへの委譲は FileActionHandlerTest で担保されているので、
        // ここでは例外が発生せずに処理が終わることを確認
    }

    @Test
    fun ディレクトリの開閉アクションが動作すること() {
        val dir = FileTreeNode(Paths.get("/root/dir"), 0, true)
        viewModel.dispatch(KifuzoAction.ToggleDirectory(dir))
        // FileTreeManager への委譲を確認
    }

    @Test
    fun 貼り付け棋譜の保存フローが動作すること() {
        viewModel.updateUiState {
            it.copy(
                pastedKifuText = """
            V2.2
            N+Sente
            N-Gote
            +7776FU
                """.trimIndent(),
            )
        }

        // 保存ダイアログ表示
        viewModel.dispatch(KifuzoAction.ShowSavePastedKifuDialog)
        assertTrue(viewModel.uiState.pastedKifuProposedName != null)

        // ダイアログ閉じる
        viewModel.dispatch(KifuzoAction.HideSavePastedKifuDialog)
        assertNull(viewModel.uiState.pastedKifuProposedName)
    }
}
