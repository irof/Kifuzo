package models

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

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
    val lastMoveText: String = ""
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
