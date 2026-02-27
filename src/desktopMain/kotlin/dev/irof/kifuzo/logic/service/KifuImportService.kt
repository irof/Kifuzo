package dev.irof.kifuzo.logic.service
import dev.irof.kifuzo.logic.io.*
import dev.irof.kifuzo.logic.parser.*
import java.nio.file.Path

interface KifuImportService {
    fun importQuestFiles(sourceDir: Path, targetDir: Path): Int
}

class KifuImportServiceImpl : KifuImportService {
    override fun importQuestFiles(sourceDir: Path, targetDir: Path): Int = importShogiQuestFiles(sourceDir, targetDir)
}
