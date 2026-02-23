package dev.irof.kifuzo.logic

import dev.irof.kifuzo.StubKifuRepository
import dev.irof.kifuzo.models.ShogiBoardState
import java.nio.file.Paths
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FileActionHandlerTest {
    private lateinit var repository: StubKifuRepository
    private lateinit var boardState: ShogiBoardState
    private lateinit var handler: FileActionHandler
    private var errorMsg: String? = null
    private var errorDetail: String? = null
    private var infoMsg: String? = null
    private var renamedPath: java.nio.file.Path? = null
    private var filesChangedCalled = false
    private var autoFlipCalled = false

    @BeforeTest
    fun setup() {
        repository = StubKifuRepository()
        boardState = ShogiBoardState()
        handler = FileActionHandler(
            repository = repository,
            boardState = boardState,
            onError = { msg, detail ->
                errorMsg = msg
                errorDetail = detail
            },
            onInfo = { infoMsg = it },
            onFileRenamed = { renamedPath = it },
            onFilesChanged = { filesChangedCalled = true },
            onAutoFlip = { autoFlipCalled = true },
        )
        errorMsg = null
        errorDetail = null
        infoMsg = null
        renamedPath = null
        filesChangedCalled = false
        autoFlipCalled = false
    }

    @Test
    fun 棋譜ファイルを選択した時にパースが実行されること() {
        val path = Paths.get("test.kifu")
        handler.selectFile(path)
        assertEquals(path, repository.lastParsedPath)
        assertEquals(true, autoFlipCalled)
    }

    @Test
    fun パースエラー時にエラーメッセージがセットされること() {
        val path = Paths.get("error.kifu")
        repository.parseAction = { throw KifuParseException("parse error") }

        handler.selectFile(path)

        assertEquals(true, errorMsg?.contains("棋譜パースエラー"))
    }

    @Test
    fun リネームが成功した時に通知されること() {
        val oldPath = Paths.get("old.kifu")
        val newPath = Paths.get("new.kifu")
        repository.renameResult = newPath

        handler.performRename(oldPath, "new.kifu")

        assertEquals(newPath, renamedPath)
        assertEquals(true, filesChangedCalled)
    }
}
