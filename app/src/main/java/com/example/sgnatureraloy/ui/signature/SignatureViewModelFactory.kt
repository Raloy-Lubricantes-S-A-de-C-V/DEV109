package com.example.sgnatureraloy.ui.signature

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.sgnatureraloy.data.repository.SignatureRepository

class SignatureViewModelFactory(private val repository: SignatureRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SignatureViewModel::class.java)) {
            return SignatureViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
