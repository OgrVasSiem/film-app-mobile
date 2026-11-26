package com.example.film_app_mobile.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.film_app_mobile.core.dataStore.ContentTypeDataStore
import com.example.film_app_mobile.core.models.ContentType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
internal class ContentSelectionViewModel
@Inject
constructor(private val store: ContentTypeDataStore) : ViewModel() {
    val selection = store.selection

    fun select(type: ContentType) {
        viewModelScope.launch { store.select(type) }
    }
}
