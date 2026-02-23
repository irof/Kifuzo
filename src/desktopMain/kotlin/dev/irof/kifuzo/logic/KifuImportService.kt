package dev.irof.kifuzo.logic

import java.nio.file.Path

interface KifuImportService {
    fun importQuestFiles(sourceDir: Path, targetDir: Path): Int
}

class KifuImportServiceImpl : KifuImportService {
    override fun importQuestFiles(sourceDir: Path, targetDir: Path): Int = importShogiQuestFiles(sourceDir, targetDir)
}
