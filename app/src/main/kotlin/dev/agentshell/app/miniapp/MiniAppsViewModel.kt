package dev.agentshell.app.miniapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MiniAppsViewModel @Inject constructor(
    private val dao: MiniAppDao
) : ViewModel() {

    val miniApps: StateFlow<List<MiniAppEntity>> = dao.getAll()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun deleteMiniApp(id: String) {
        viewModelScope.launch {
            dao.deleteById(id)
        }
    }
}
