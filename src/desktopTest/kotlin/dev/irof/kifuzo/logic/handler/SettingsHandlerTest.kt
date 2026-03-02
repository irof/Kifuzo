package dev.irof.kifuzo.logic.handler

import dev.irof.kifuzo.InMemoryAppSettings
import dev.irof.kifuzo.StubKifuRepository
import dev.irof.kifuzo.models.BoardSnapshot
import dev.irof.kifuzo.models.KifuInfo
import dev.irof.kifuzo.models.KifuSession
import dev.irof.kifuzo.models.ShogiBoardState
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.writeText
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettingsHandlerTest {
    private lateinit var repository: StubKifuRepository
    private lateinit var handler: SettingsHandler
    private var filesChangedCalled = false
    private var flipped: Boolean? = null

    @BeforeTest
    fun setup() {
        repository = object : StubKifuRepository() {
            override fun generateProposedName(path: Path, info: KifuInfo, template: String): String? = "proposed.kifu"
            override fun renameFileTo(path: Path, newName: String): Path? = path.parent.resolve(newName)
            override fun scanKifuInfo(lines: List<String>): KifuInfo = KifuInfo(Paths.get("stub"))
        }
        handler = SettingsHandler(
            repository = repository,
            settings = InMemoryAppSettings(),
            boardState = ShogiBoardState(),
            onFilesChanged = { filesChangedCalled = true },
            onAutoFlip = { flipped = it },
        )
    }

    @Test
    fun renameWithTemplateがリネームを実行し通知すること() {
        val root = kotlin.io.path.createTempDirectory("kifuzo-rename-test")
        try {
            val path = root.resolve("old.kifu")
            path.writeText("test")

            handler.renameWithTemplate(path, "{template}")

            assertTrue(filesChangedCalled)
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun updateAutoFlipが名前設定に基づいて自動反転すること() {
        val state = ShogiBoardState()
        // 先手: A, 後手: B
        state.updateSession(
            KifuSession(
                initialSnapshot = BoardSnapshot(BoardSnapshot.getInitialCells()),
                senteName = "A",
                goteName = "B",
            ),
        )

        val handlerAutoFlip = SettingsHandler(
            repository = repository,
            settings = InMemoryAppSettings(),
            boardState = state,
            onFilesChanged = {},
            onAutoFlip = { flipped = it },
        )

        // 自分が後手(B)の場合、反転する
        handlerAutoFlip.updateAutoFlip("B")
        assertEquals(true, flipped)

        // 自分が先手(A)の場合、反転しない
        handlerAutoFlip.updateAutoFlip("A")
        assertEquals(false, flipped)

        // どちらにもマッチしない場合、何もしない
        flipped = null
        handlerAutoFlip.updateAutoFlip("X")
        assertEquals(null, flipped)
    }
}
