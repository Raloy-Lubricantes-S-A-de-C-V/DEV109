package com.example.sgnatureraloy.ui.signature

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sgnatureraloy.data.model.SignatureProcess
import com.example.sgnatureraloy.data.repository.SignatureRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class FilterType { PENDIENTE, TERMINADO, CANCELADO }

class SignatureViewModel(private val repository: SignatureRepository) : ViewModel() {

    private val _signatures = MutableStateFlow<List<SignatureProcess>>(emptyList())
    
    private val _selectedFilter = MutableStateFlow(FilterType.PENDIENTE)
    val selectedFilter: StateFlow<FilterType> = _selectedFilter.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val filteredSignatures: StateFlow<List<SignatureProcess>> = combine(_signatures, _selectedFilter) { signatures, filter ->
        when (filter) {
            FilterType.PENDIENTE -> signatures.filter { it.status.uppercase() in listOf("PROCESSING", "PROCESO") }
            FilterType.TERMINADO -> signatures.filter { it.status.uppercase() in listOf("COMPLETED", "CERRADO") }
            FilterType.CANCELADO -> signatures.filter { it.status.uppercase() in listOf("CANCELLED", "CANCELADO") }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val signatures: StateFlow<List<SignatureProcess>> = _signatures.asStateFlow()

    fun setFilter(filter: FilterType) {
        _selectedFilter.value = filter
    }

    fun loadSignatures(email: String) {
        Log.d("FIRMA", "SignatureViewModel: Iniciando carga de firmas para $email")
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getSignatures(email).collect {
                    Log.d("FIRMA", "SignatureViewModel: Se cargaron ${it.size} firmas")
                    _signatures.value = it
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e("FIRMA", "SignatureViewModel: Error al cargar firmas: ${e.message}", e)
                _isLoading.value = false
            }
        }
    }
}
