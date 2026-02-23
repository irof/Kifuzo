package dev.irof.kifuzo.logic

import dev.irof.kifuzo.models.KifuSession
import dev.irof.kifuzo.models.ShogiBoardState
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsHandlerTest {
    private lateinit var boardState: ShogiBoardState
    private lateinit var handler: SettingsHandler
    private var isFlipped: Boolean? = null

    @BeforeTest
    fun setup() {
        boardState = ShogiBoardState()
        handler = SettingsHandler(
            boardState = boardState,
            onAutoFlip = { isFlipped = it },
        )
        isFlipped = null
    }

    @Test
    fun 後手名が正規表現にマッチした時に反転すること() {
        boardState.updateSession(KifuSession(senteName = "Sente", goteName = "MyName"))

        handler.updateAutoFlip("MyName")

        assertEquals(true, isFlipped)
    }

    @Test
    fun 先手名が正規表現にマッチした時に反転しないこと() {
        boardState.updateSession(KifuSession(senteName = "MyName", goteName = "Gote"))

        handler.updateAutoFlip("MyName")

        assertEquals(false, isFlipped)
    }

    @Test
    fun 不正な正規表現が渡された時にログ出力され処理が中断されること() {
        handler.updateAutoFlip("[") // 不正な正規表現
        assertEquals(null, isFlipped)
    }
}
