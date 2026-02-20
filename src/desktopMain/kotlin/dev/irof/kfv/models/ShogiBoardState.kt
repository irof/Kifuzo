package dev.irof.kfv.models

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class ShogiBoardState {
    // 対局データ全体を一括管理
    var session by mutableStateOf(KifuSession())
        private set

    // 現在の手数（常に session の範囲内に収まることを保証）
    private var _currentStep by mutableStateOf(0)
    var currentStep: Int
        get() = _currentStep
        set(value) {
            _currentStep = session.coerceStep(value)
        }

    val currentBoard: BoardSnapshot?
        get() = session.history.getOrNull(currentStep)

    /**
     * 新しい対局データをセットします。
     * 手数やプレイヤー名なども含めてアトミックに更新されます。
     */
    fun updateSession(newSession: KifuSession) {
        session = newSession
        currentStep = newSession.initialStep
    }

    /**
     * すべての状態を初期化します。
     */
    fun clear() {
        updateSession(KifuSession())
    }
}
