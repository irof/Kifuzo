package dev.irof.kifuzo.logic.service
import dev.irof.kifuzo.logic.io.readLinesWithEncoding
import dev.irof.kifuzo.logic.io.readTextWithEncoding
import dev.irof.kifuzo.logic.parser.HeaderParser
import dev.irof.kifuzo.logic.parser.KifuParseException
import dev.irof.kifuzo.logic.parser.convertCsaToKifu
import dev.irof.kifuzo.logic.parser.csa.parseCsa
import dev.irof.kifuzo.logic.parser.kif.parseKifu
import dev.irof.kifuzo.logic.parser.kif.scanKifuInfo
import dev.irof.kifuzo.logic.parser.parseHeader
import java.nio.file.Path

interface KifuImportService {
    fun importQuestFiles(sourceDir: Path, targetDir: Path): Int
}

class KifuImportServiceImpl : KifuImportService {
    override fun importQuestFiles(sourceDir: Path, targetDir: Path): Int = importShogiQuestFiles(sourceDir, targetDir)
}
