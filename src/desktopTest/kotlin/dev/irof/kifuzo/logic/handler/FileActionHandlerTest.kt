package dev.irof.kifuzo.logic.handler

import dev.irof.kifuzo.StubKifuRepository
import dev.irof.kifuzo.logic.parser.KifuParseException
import dev.irof.kifuzo.models.ShogiBoardState
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.readText
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
        assertEquals("parse", repository.lastMethodCalled)
        assertEquals(true, autoFlipCalled)
    }

    @Test
    fun サポート外の拡張子のファイルを選択した際に盤面がクリアされること() {
        val path = Paths.get("test.txt")
        // 予め何かデータが入っている状態にする
        boardState.updateSession(dev.irof.kifuzo.models.KifuSession())

        handler.selectFile(path)

        assertEquals(0, boardState.session.moves.size)
        assertEquals(null, repository.lastParsedPath)
    }

    @Test
    fun forceParseが拡張子に関わらずparseManuallyを実行すること() {
        val path = Paths.get("test.txt")
        handler.forceParse(path)
        assertEquals(path, repository.lastParsedPath)
        assertEquals("parseManually", repository.lastMethodCalled)
    }

    @Test
    fun パースエラー時にエラーメッセージがセットされ盤面がクリアされること() {
        val path = Paths.get("error.kifu")
        repository.parseAction = { throw KifuParseException("parse error") }
        // 予め何かデータが入っている状態にする
        boardState.updateSession(dev.irof.kifuzo.models.KifuSession())

        handler.selectFile(path)

        assertEquals(true, errorMsg?.contains("棋譜パースエラー"))
        assertEquals(0, boardState.session.moves.size)
    }

    @Test
    fun 一般的な例外発生時にもエラーメッセージがセットされ盤面がクリアされること() {
        val path = Paths.get("io-error.kifu")
        repository.parseAction = { throw java.io.IOException("io error") }
        boardState.updateSession(dev.irof.kifuzo.models.KifuSession())

        handler.selectFile(path)

        assertEquals(true, errorMsg?.contains("ファイルの読み込みに失敗しました"))
        assertEquals(0, boardState.session.moves.size)
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

    @Test
    fun CSA形式からKIF形式への変換に失敗した時にエラー通知されること() {
        val path = Paths.get("error.csa")
        val errorMsgDetail = "Conversion error"
        repository.convertCsaAction = { throw KifuParseException(errorMsgDetail) }

        handler.performCsaConversion(path)

        assertEquals("変換エラー", errorMsg)
        assertEquals(errorMsgDetail, errorDetail)
    }

    @Test
    fun writeGameResultが成功した時に通知されること() {
        val path = Paths.get("test.kifu")
        handler.writeGameResult(path, "投了")
        assertEquals(true, filesChangedCalled)
        assertEquals(true, infoMsg?.contains("対局結果"))
    }

    @Test
    fun updateMetadataが成功した時に通知されること() {
        val path = Paths.get("test.kifu")
        handler.updateMetadata(path, "Event", "2026/03/01")
        assertEquals(true, filesChangedCalled)
        assertEquals(true, infoMsg?.contains("棋譜情報"))
    }

    @Test
    fun savePastedKifuがファイルを保存し通知すること() {
        val root = kotlin.io.path.createTempDirectory("kifuzo-save-test")
        try {
            val filename = "pasted.kifu"
            val text = "test kifu content"

            handler.savePastedKifu(root, filename, text)

            val savedFile = root.resolve(filename)
            assertTrue(savedFile.exists())
            assertEquals(text, savedFile.readText())
            assertEquals(savedFile, renamedPath)
            assertTrue(filesChangedCalled)
            assertTrue(infoMsg?.contains("保存しました") == true)
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun openInExternalAppが例外を投げずに実行されること() {
        // 実際の外部アプリ起動はモックしづらいが、パスが渡された時にエラーにならないことを確認
        val path = Paths.get("test.kifu")
        handler.openInExternalApp(path)
        // ShogiHomeがない環境ではエラー通知されるはず
        assertTrue(errorMsg?.contains("起動エラー") == true || infoMsg?.contains("開いています") == true)
    }
}

private fun assertTrue(actual: Boolean) {
    kotlin.test.assertTrue(actual)
}
