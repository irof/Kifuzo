package dev.irof.kifuzo.logic

import dev.irof.kifuzo.StubKifuRepository
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
