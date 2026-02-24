package dev.irof.kifuzo.logic

import dev.irof.kifuzo.StubKifuRepository
import dev.irof.kifuzo.models.ShogiBoardState
import java.nio.file.Paths
import kotlin.io.path.extension
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
    fun サポート外の拡張子のファイルを選択した際に盤面がクリアされること() {
        val path = Paths.get("test.txt")
        // 予め何かデータが入っている状態にする
        boardState.updateSession(dev.irof.kifuzo.models.KifuSession(history = listOf(dev.irof.kifuzo.models.BoardSnapshot(dev.irof.kifuzo.models.BoardSnapshot.getInitialCells()))))

        handler.selectFile(path)

        assertEquals(0, boardState.session.history.size)
        assertEquals(null, repository.lastParsedPath)
    }

    @Test
    fun forceParseが拡張子に関わらずパースを実行すること() {
        val path = Paths.get("test.txt")
        handler.forceParse(path)
        assertEquals(path, repository.lastParsedPath)
    }

    @Test
    fun パースエラー時にエラーメッセージがセットされ盤面がクリアされること() {
        val path = Paths.get("error.kifu")
        repository.parseAction = { throw KifuParseException("parse error") }
        // 予め何かデータが入っている状態にする
        boardState.updateSession(dev.irof.kifuzo.models.KifuSession(history = listOf(dev.irof.kifuzo.models.BoardSnapshot(dev.irof.kifuzo.models.BoardSnapshot.getInitialCells()))))

        handler.selectFile(path)

        assertEquals(true, errorMsg?.contains("棋譜パースエラー"))
        assertEquals(0, boardState.session.history.size)
    }

    @Test
    fun 一般的な例外発生時にもエラーメッセージがセットされ盤面がクリアされること() {
        val path = Paths.get("io-error.kifu")
        repository.parseAction = { throw java.io.IOException("io error") }
        boardState.updateSession(dev.irof.kifuzo.models.KifuSession(history = listOf(dev.irof.kifuzo.models.BoardSnapshot(dev.irof.kifuzo.models.BoardSnapshot.getInitialCells()))))

        handler.selectFile(path)

        assertEquals(true, errorMsg?.contains("ファイルの読み込みに失敗しました"))
        assertEquals(0, boardState.session.history.size)
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
