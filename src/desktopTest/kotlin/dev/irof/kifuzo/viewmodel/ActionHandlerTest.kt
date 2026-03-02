package dev.irof.kifuzo.viewmodel

import dev.irof.kifuzo.InMemoryAppSettings
import dev.irof.kifuzo.StubKifuRepository
import dev.irof.kifuzo.models.FileTreeNode
import java.nio.file.Paths
import kotlin.io.path.createTempDirectory
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
        // 実際のファイルシステムを使ったテスト
        val dir = kotlin.io.path.createTempDirectory("kifuzo-convert-test")
        try {
            val csaFile = dir.resolve("test.csa")
            java.nio.file.Files.writeString(csaFile, "V2.2")
            val kifuFile = dir.resolve("test.kifu")

            // 1. KIFUファイルが存在しない場合、即座に変換
            viewModel.dispatch(KifuzoAction.ConvertCsa(csaFile))
            assertEquals("convertCsa", stubRepository.lastMethodCalled)
            assertNull(viewModel.uiState.showOverwriteConfirm)

            // 2. KIFUファイルが存在する場合、上書き確認を表示
            java.nio.file.Files.writeString(kifuFile, "existing")
            viewModel.dispatch(KifuzoAction.ConvertCsa(csaFile))
            assertEquals(csaFile, viewModel.uiState.showOverwriteConfirm)

            // 3. 上書き確認で Confirm した場合、変換実行
            stubRepository.lastMethodCalled = ""
            viewModel.dispatch(KifuzoAction.ConfirmOverwrite)
            assertEquals("convertCsa", stubRepository.lastMethodCalled)
            assertNull(viewModel.uiState.showOverwriteConfirm)
        } finally {
            dir.toFile().deleteRecursively()
        }
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
    }

    @Test
    fun ディレクトリの開閉アクションが動作すること() {
        val dir = FileTreeNode(Paths.get("/root/dir"), 0, true)
        viewModel.dispatch(KifuzoAction.ToggleDirectory(dir))
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

        viewModel.dispatch(KifuzoAction.ShowSavePastedKifuDialog)
        assertTrue(viewModel.uiState.pastedKifuProposedName != null)

        viewModel.dispatch(KifuzoAction.HideSavePastedKifuDialog)
        assertNull(viewModel.uiState.pastedKifuProposedName)
    }

    @Test
    fun エラーメッセージが表示されている時にクリアできること() {
        viewModel.updateUiState { it.copy(errorMessage = "Some error") }
        assertEquals("Some error", viewModel.uiState.errorMessage)

        viewModel.dispatch(KifuzoAction.ClearErrorAndInfo)
        assertNull(viewModel.uiState.errorMessage)
    }

    @Test
    fun UIのトグル系アクションが動作すること() {
        // Sidebar
        val initialSidebar = viewModel.uiState.isSidebarVisible
        viewModel.dispatch(KifuzoAction.ToggleSidebar)
        assertEquals(!initialSidebar, viewModel.uiState.isSidebarVisible)

        // MoveList
        val initialMoveList = viewModel.uiState.isMoveListVisible
        viewModel.dispatch(KifuzoAction.ToggleMoveList)
        assertEquals(!initialMoveList, viewModel.uiState.isMoveListVisible)

        // Flipped
        val initialFlipped = viewModel.uiState.isFlipped
        viewModel.dispatch(KifuzoAction.ToggleFlipped)
        assertEquals(!initialFlipped, viewModel.uiState.isFlipped)
    }

    @Test
    fun 各種設定変更アクションが動作すること() {
        // ViewMode
        viewModel.dispatch(KifuzoAction.SetViewMode(dev.irof.kifuzo.models.FileViewMode.FLAT))
        // Dispatchers.Main の設定がないため refreshFiles が即座に失敗または無視されることを期待
        // uiState 自体は即座に書き換わるはず
        assertEquals(dev.irof.kifuzo.models.FileViewMode.FLAT, viewModel.uiState.viewMode)

        // SortOption
        viewModel.dispatch(KifuzoAction.SetFileSortOption(dev.irof.kifuzo.models.FileSortOption.LAST_MODIFIED))
        assertEquals(dev.irof.kifuzo.models.FileSortOption.LAST_MODIFIED, viewModel.uiState.fileSortOption)

        // SidebarWidth
        viewModel.dispatch(KifuzoAction.UpdateSidebarWidth(50f)) // delta
        assertEquals(300f, viewModel.uiState.sidebarWidth) // 250 + 50
    }

    @Test
    fun 棋譜操作系アクションが動作すること() {
        viewModel.dispatch(KifuzoAction.RefreshFiles)
        viewModel.dispatch(KifuzoAction.ResetToMainHistory)
        viewModel.dispatch(KifuzoAction.SelectVariation(emptyList()))
        viewModel.dispatch(KifuzoAction.ToggleFileFilter(dev.irof.kifuzo.models.FileFilter.RECENT))
    }
}
