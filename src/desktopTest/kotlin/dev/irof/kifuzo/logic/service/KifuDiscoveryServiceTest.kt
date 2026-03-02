package dev.irof.kifuzo.logic.service

import dev.irof.kifuzo.StubKifuRepository
import dev.irof.kifuzo.models.FileFilter
import dev.irof.kifuzo.models.FileSortOption
import dev.irof.kifuzo.models.FileTreeNode
import dev.irof.kifuzo.models.FileViewMode
import java.nio.file.Paths
import kotlin.io.path.createFile
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KifuDiscoveryServiceTest {

    private val repository = StubKifuRepository()
    private val fileTreeManager = FileTreeManager(repository)
    private val service = KifuDiscoveryService(repository, fileTreeManager)

    @Test
    fun buildFileListがHIERARCHYモードでTreeを構築すること() {
        val root = Paths.get("/root")
        val result = service.buildFileList(
            root = root,
            mode = FileViewMode.HIERARCHY,
            currentNodes = emptyList(),
            filters = emptySet(),
            sortOption = FileSortOption.NAME,
        )
        // 実体は fileTreeManager.buildTree が呼ばれていることの確認
        assertTrue(result.isEmpty())
    }

    @Test
    fun buildFileListがFLATモードでFlatListを構築すること() {
        // FLATモードの場合は Files.walkFileTree を呼ぶため、実在するディレクトリが必要
        val root = createTempDirectory("kifuzo-discovery-test")
        try {
            root.resolve("test.kifu").createFile()
            val result = service.buildFileList(
                root = root,
                mode = FileViewMode.FLAT,
                currentNodes = emptyList(),
                filters = emptySet(),
                sortOption = FileSortOption.NAME,
            )
            assertEquals(1, result.size)
            assertEquals("test.kifu", result[0].name)
        } finally {
            root.toFile().deleteRecursively()
        }
    }

    @Test
    fun isKifuFileが隠しファイルを無視すること() {
        val root = createTempDirectory("kifuzo-discovery-hidden")
        try {
            root.resolve(".hidden.kifu").createFile()
            root.resolve("normal.kifu").createFile()
            val result = service.buildFileList(
                root = root,
                mode = FileViewMode.FLAT,
                currentNodes = emptyList(),
                filters = emptySet(),
                sortOption = FileSortOption.NAME,
            )
            assertEquals(1, result.size)
            assertEquals("normal.kifu", result[0].name)
        } finally {
            root.toFile().deleteRecursively()
        }
    }
}
