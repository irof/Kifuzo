package dev.irof.kifuzo.logic

import dev.irof.kifuzo.models.FileSortOption
import dev.irof.kifuzo.models.KifuInfo
import dev.irof.kifuzo.models.ShogiBoardState
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ImportHandlerTest {
    private lateinit var repository: StubKifuRepository
    private lateinit var handler: ImportHandler
    private var infoMsg: String? = null
    private var importedCalled = false

    class StubKifuRepository : KifuRepository {
        var importResult = 0
        override fun scanDirectory(directory: Path, sortOption: FileSortOption): List<Path> = emptyList()
        override fun getKifuInfos(files: List<Path>): Map<Path, KifuInfo> = emptyMap()
        override fun parse(path: Path, state: ShogiBoardState) {}
        override fun convertCsa(path: Path): Path = path
        override fun updateResult(path: Path, result: String) {}
        override fun renameKifuFile(path: Path, template: String): Path? = path
        override fun importQuestFiles(sourceDir: Path, targetDir: Path): Int = importResult
    }

    @BeforeTest
    fun setup() {
        repository = StubKifuRepository()
        handler = ImportHandler(
            repository = repository,
            onInfo = { infoMsg = it },
            onImported = { importedCalled = true },
        )
        infoMsg = null
        importedCalled = false
    }

    @Test
    fun インポートが成功した時に通知されること() {
        val sourceDir = Paths.get("source")
        val root = Paths.get("root")
        repository.importResult = 5

        handler.importFiles(sourceDir, root)

        assertEquals(true, infoMsg?.contains("5件"))
        assertEquals(true, importedCalled)
    }

    @Test
    fun 該当ファイルがない時に適切なメッセージが表示されること() {
        val sourceDir = Paths.get("source")
        val root = Paths.get("root")
        repository.importResult = 0

        handler.importFiles(sourceDir, root)

        assertEquals(true, infoMsg?.contains("見つかりませんでした"))
        assertEquals(false, importedCalled)
    }
}
