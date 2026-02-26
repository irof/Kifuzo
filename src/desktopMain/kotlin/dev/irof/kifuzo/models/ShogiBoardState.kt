package dev.irof.kifuzo.models

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class ShogiBoardState {
    // 対局データ全体
    var session by mutableStateOf(KifuSession())
        private set

    // 現在表示中の初期局面と指し手リスト
    var currentInitialSnapshot by mutableStateOf(BoardSnapshot(BoardSnapshot.getInitialCells()))
        private set
    var currentMoves by mutableStateOf(emptyList<Move>())
        private set

    // 現在表示中の局面リスト（0手目を含む、UI互換用）
    val currentHistory: List<BoardSnapshot> get() = listOf(currentInitialSnapshot) + currentMoves.map { it.resultSnapshot }

    // 現在の手数
    private var _currentStep by mutableStateOf(0)
    var currentStep: Int
        get() = _currentStep
        set(value) {
            _currentStep = value.coerceIn(0, currentMoves.size)
        }

    val currentBoard: BoardSnapshot
        get() = if (currentStep == 0) currentInitialSnapshot else currentMoves[currentStep - 1].resultSnapshot

    /**
     * 新しい対局データをセットします。
     */
    fun updateSession(newSession: KifuSession) {
        session = newSession
        currentInitialSnapshot = newSession.initialSnapshot
        currentMoves = newSession.moves
        currentStep = newSession.initialStep
    }

    /**
     * 手順を切り替えます（変化手順の選択など）。
     */
    fun switchHistory(newMoves: List<Move>) {
        if (newMoves.isEmpty()) return
        currentInitialSnapshot = newMoves[0].resultSnapshot // 分岐元
        currentMoves = newMoves.drop(1)
        currentStep = 0
    }

    /**
     * 本譜に戻ります。
     */
    fun resetToMainHistory() {
        currentInitialSnapshot = session.initialSnapshot
        currentMoves = session.moves
        currentStep = session.coerceStep(currentStep)
    }

    /**
     * すべての状態を初期化します。
     */
    fun clear() {
        updateSession(KifuSession())
    }
}
