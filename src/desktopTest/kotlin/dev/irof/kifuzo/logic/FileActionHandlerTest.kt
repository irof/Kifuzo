package dev.irof.kifuzo.logic

import dev.irof.kifuzo.models.BoardSnapshot
import dev.irof.kifuzo.models.KifuInfo
import dev.irof.kifuzo.models.KifuSession
import dev.irof.kifuzo.models.ShogiBoardState
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FileActionHandlerTest {
    private lateinit var repository: StubKifuRepository
    private lateinit var boardState: ShogiBoardState
    private lateinit var handler: FileActionHandler
    private var errorMsg: String? = null
    private var infoMsg: String? = null
    private var renamedPath: Path? = null
    private var filesChangedCalled = false
    private var autoFlipCalled = false

    class StubKifuRepository : KifuRepository {
        var parseCalled = false
        var parseError = false
        var renameResult: Path? = null

        override fun scanDirectory(directory: Path): List<Path> = emptyList()
        override fun getKifuInfos(files: List<Path>): Map<Path, KifuInfo> = emptyMap()
        override fun parse(path: Path, state: ShogiBoardState) {
            parseCalled = true
            if (parseError) throw KifuParseException("parse error")
        }
        override fun convertCsa(path: Path): Path = path
        override fun updateSenkei(path: Path, senkei: String) {}
        override fun updateResult(path: Path, result: String) {}
        override fun renameKifuFile(path: Path, template: String): Path? = renameResult
        override fun importQuestFiles(sourceDir: Path, targetDir: Path): Int = 0
    }

    @BeforeTest
    fun setup() {
        repository = StubKifuRepository()
        boardState = ShogiBoardState()
        handler = FileActionHandler(
            repository = repository,
            boardState = boardState,
            onError = { errorMsg = it },
            onInfo = { infoMsg = it },
            onFileRenamed = { renamedPath = it },
            onFilesChanged = { filesChangedCalled = true },
            onAutoFlip = { autoFlipCalled = true }
        )
        errorMsg = null
        infoMsg = null
        renamedPath = null
        filesChangedCalled = false
        autoFlipCalled = false
    }

    @Test
    fun 棋譜ファイルを選択した時にパースが実行されること() {
        val path = Paths.get("test.kifu")
        handler.selectFile(path)
        assertEquals(true, repository.parseCalled)
        assertEquals(true, autoFlipCalled)
    }

    @Test
    fun パースエラー時にエラーメッセージがセットされること() {
        val path = Paths.get("error.kifu")
        repository.parseError = true
        
        handler.selectFile(path)
        
        assertEquals(true, errorMsg?.contains("棋譜パースエラー"))
    }

    @Test
    fun リネームが成功した時に通知されること() {
        val oldPath = Paths.get("old.kifu")
        val newPath = Paths.get("new.kifu")
        repository.renameResult = newPath
        
        handler.renameFile(oldPath, "{Sente}")
        
        assertEquals(newPath, renamedPath)
        assertEquals(true, filesChangedCalled)
    }
}
