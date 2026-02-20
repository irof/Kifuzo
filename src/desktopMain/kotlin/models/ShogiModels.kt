package models

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File

object AppConfig {
    val USER_HOME: String = System.getProperty("user.home")
    val KIFU_ROOT = File(USER_HOME, "Kifu")
    val QUEST_CSA_DIR = File(KIFU_ROOT, "quest/csa")
}

object AppSettings {
    private val prefs = java.util.prefs.Preferences.userNodeForPackage(AppSettings::class.java)
    
    private const val KEY_MY_NAME_REGEX = "my_name_regex"
    private const val KEY_WINDOW_X = "window_x"
    private const val KEY_WINDOW_Y = "window_y"
    private const val KEY_WINDOW_WIDTH = "window_width"
    private const val KEY_WINDOW_HEIGHT = "window_height"

    var myNameRegex: String
        get() = prefs.get(KEY_MY_NAME_REGEX, "")
        set(value) = prefs.put(KEY_MY_NAME_REGEX, value)

    var windowX: Float?
        get() = if (prefs.get(KEY_WINDOW_X, null) == null) null else prefs.getFloat(KEY_WINDOW_X, 0f)
        set(value) = if (value == null) prefs.remove(KEY_WINDOW_X) else prefs.putFloat(KEY_WINDOW_X, value)

    var windowY: Float?
        get() = if (prefs.get(KEY_WINDOW_Y, null) == null) null else prefs.getFloat(KEY_WINDOW_Y, 0f)
        set(value) = if (value == null) prefs.remove(KEY_WINDOW_Y) else prefs.putFloat(KEY_WINDOW_Y, value)

    var windowWidth: Float
        get() = prefs.getFloat(KEY_WINDOW_WIDTH, 800f)
        set(value) = prefs.putFloat(KEY_WINDOW_WIDTH, value)

    var windowHeight: Float
        get() = prefs.getFloat(KEY_WINDOW_HEIGHT, 750f)
        set(value) = prefs.putFloat(KEY_WINDOW_HEIGHT, value)

    /**
     * 現在保存されているすべての設定を取得します。
     */
    fun getAllSettings(): Map<String, String> {
        val map = mutableMapOf<String, String>()
        prefs.keys().forEach { key ->
            map[key] = prefs.get(key, "")
        }
        return map
    }

    /**
     * 特定のキーの設定を削除します。
     */
    fun removeSetting(key: String) {
        prefs.remove(key)
    }

    /**
     * 直接値を設定します。
     */
    fun putSetting(key: String, value: String) {
        prefs.put(key, value)
    }
}

enum class Piece(val symbol: String) {
    FU("歩"), KY("香"), KE("桂"), GI("銀"), KI("金"), KA("角"), HI("飛"), OU("玉"),
    TO("と"), NY("杏"), NK("圭"), NG("全"), UM("馬"), RY("龍");

    fun toBase(): Piece = when(this) {
        TO -> FU; NY -> KY; NK -> KE; NG -> GI; UM -> KA; RY -> HI; else -> this
    }
    fun isPromoted(): Boolean = this in listOf(TO, NY, NK, NG, UM, RY)
}

data class BoardSnapshot(
    val cells: Array<Array<Pair<Piece, Boolean>?>>,
    val senteMochigoma: List<Piece> = emptyList(),
    val goteMochigoma: List<Piece> = emptyList(),
    val lastMoveText: String = "",
    val lastFrom: Pair<Int, Int>? = null, // (x, y) 0-8
    val lastTo: Pair<Int, Int>? = null    // (x, y) 0-8
)

class ShogiBoardState {
    var history by mutableStateOf(listOf<BoardSnapshot>())
    var currentStep by mutableStateOf(0)
    var firstContactStep by mutableStateOf(-1)
    var senteName by mutableStateOf("先手")
    var goteName by mutableStateOf("後手")

    val currentBoard: BoardSnapshot?
        get() = if (history.isNotEmpty()) history[currentStep] else null

    fun reset(initialCells: Array<Array<Pair<Piece, Boolean>?>>) {
        history = listOf(BoardSnapshot(initialCells, lastMoveText = "開始局面"))
        currentStep = 0
        firstContactStep = -1
        senteName = "先手"
        goteName = "後手"
    }

    fun addStep(snapshot: BoardSnapshot, isContact: Boolean) {
        history = history + snapshot
        if (isContact && firstContactStep == -1) {
            firstContactStep = history.size - 1
        }
    }
}
