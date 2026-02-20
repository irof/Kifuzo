package dev.irof.kfv.models

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class ShogiBoardState {
    var history by mutableStateOf(listOf<BoardSnapshot>())
    var currentStep by mutableStateOf(0)
    var firstContactStep by mutableStateOf(-1)
    var isStandardStart by mutableStateOf(true)
    var senteName by mutableStateOf("先手")
    var goteName by mutableStateOf("後手")

    val currentBoard: BoardSnapshot?
        get() = if (history.isNotEmpty()) history[currentStep] else null

    fun reset(initialCells: Array<Array<Pair<Piece, PieceColor>?>>) {
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
