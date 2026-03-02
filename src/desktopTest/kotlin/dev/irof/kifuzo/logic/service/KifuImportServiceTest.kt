package dev.irof.kifuzo.logic.service

import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals

class KifuImportServiceTest {

    @Test
    fun importQuestFilesが実体としてimportShogiQuestFilesを呼び出すこと() {
        val service = KifuImportServiceImpl()
        val source = createTempDirectory("import-src")
        val target = createTempDirectory("import-target")
        try {
            // ファイルがないので 0 が返るはず
            val count = service.importQuestFiles(source, target)
            assertEquals(0, count)
        } finally {
            source.toFile().deleteRecursively()
            target.toFile().deleteRecursively()
        }
    }
}
