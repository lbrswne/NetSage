package com.netsage.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.netsage.app.api.ApiFactory
import com.netsage.app.repo.DiagnoseRepository

class ViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repo = DiagnoseRepository(ApiFactory.createApi())
        return DiagnoseViewModel(repo) as T
    }
}
