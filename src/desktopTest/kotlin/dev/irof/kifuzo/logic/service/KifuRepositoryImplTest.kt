package dev.irof.kifuzo.logic.service

import dev.irof.kifuzo.models.ShogiBoardState
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals

class KifuRepositoryImplTest {

    private class StubFileService : KifuFileService {
        var lastMethod = ""
        override fun scanDirectory(directory: Path, sortOption: dev.irof.kifuzo.models.FileSortOption): List<Path> {
            lastMethod = "scanDirectory"
            return emptyList()
        }
        override fun renameFile(path: Path, newName: String): Path? {
            lastMethod = "renameFile"
            return path
        }
        override fun generateProposedName(path: Path, info: dev.irof.kifuzo.models.KifuInfo, template: String): String? {
            lastMethod = "generateProposedName"
            return "new"
        }
        override fun generateProposedNameForPasted(info: dev.irof.kifuzo.models.KifuInfo, template: String): String? {
            lastMethod = "generateProposedNameForPasted"
            return "pasted"
        }
        override fun updateResult(path: Path, result: String) {
            lastMethod = "updateResult"
        }
        override fun updateHeader(path: Path, event: String, startTime: String) {
            lastMethod = "updateHeader"
        }
    }

    private class StubParseService : KifuParseService {
        var lastMethod = ""
        override fun parse(path: Path, state: ShogiBoardState) {
            lastMethod = "parse"
        }
        override fun parseManually(path: Path, state: ShogiBoardState) {
            lastMethod = "parseManuallyPath"
        }
        override fun parseManually(lines: List<String>, state: ShogiBoardState) {
            lastMethod = "parseManuallyLines"
        }
        override fun scanInfo(path: Path): dev.irof.kifuzo.models.KifuInfo {
            lastMethod = "scanInfoPath"
            return dev.irof.kifuzo.models.KifuInfo(path)
        }
        override fun scanInfo(lines: List<String>, suggestedFormat: dev.irof.kifuzo.logic.parser.KifuFormat?): dev.irof.kifuzo.models.KifuInfo {
            lastMethod = "scanInfoLines"
            return dev.irof.kifuzo.models.KifuInfo(Paths.get("stub"))
        }
        override fun convertCsaToKifu(path: Path): Path {
            lastMethod = "convertCsaToKifu"
            return path
        }
    }

    @Test
    fun KifuRepositoryImplが各サービスに正しく委譲すること() {
        val fileService = StubFileService()
        val parseService = StubParseService()
        val repository = KifuRepositoryImpl(fileService, parseService)

        repository.scanDirectory(Paths.get("."), dev.irof.kifuzo.models.FileSortOption.NAME)
        assertEquals("scanDirectory", fileService.lastMethod)

        repository.parse(Paths.get("test.kifu"), ShogiBoardState())
        assertEquals("parse", parseService.lastMethod)

        repository.updateMetadata(Paths.get("test.kifu"), event = "Event", startTime = "Time")
        assertEquals("updateHeader", fileService.lastMethod)

        repository.updateMetadata(Paths.get("test.kifu"), result = "Result")
        assertEquals("updateResult", fileService.lastMethod)

        repository.renameFileTo(Paths.get("test.kifu"), "new.kifu")
        assertEquals("renameFile", fileService.lastMethod)

        repository.convertCsa(Paths.get("test.csa"))
        assertEquals("convertCsaToKifu", parseService.lastMethod)

        repository.generateProposedNameForPasted(dev.irof.kifuzo.models.KifuInfo(Paths.get("test")), "template")
        assertEquals("generateProposedNameForPasted", fileService.lastMethod)
    }
}
