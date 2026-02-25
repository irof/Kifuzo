package dev.irof.kifuzo.models

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class ShogiBoardState {
    // 対局データ全体
    var session by mutableStateOf(KifuSession())
        private set

    // 現在表示中の履歴（本譜または変化手順）
    var currentHistory by mutableStateOf(emptyList<BoardSnapshot>())
        private set

    // 現在の手数
    private var _currentStep by mutableStateOf(0)
    var currentStep: Int
        get() = _currentStep
        set(value) {
            _currentStep = value.coerceIn(0, maxOf(0, currentHistory.size - 1))
        }

    val currentBoard: BoardSnapshot?
        get() = currentHistory.getOrNull(currentStep)

    /**
     * 新しい対局データをセットします。
     */
    fun updateSession(newSession: KifuSession) {
        session = newSession
        currentHistory = newSession.history
        currentStep = newSession.initialStep
    }

    /**
     * 手順を切り替えます（変化手順の選択など）。
     */
    fun switchHistory(newHistory: List<BoardSnapshot>) {
        currentHistory = newHistory
        currentStep = 0
    }

    /**
     * 本譜に戻ります。
     */
    fun resetToMainHistory() {
        currentHistory = session.history
        currentStep = session.coerceStep(currentStep)
    }

    /**
     * すべての状態を初期化します。
     */
    fun clear() {
        updateSession(KifuSession())
    }
}
