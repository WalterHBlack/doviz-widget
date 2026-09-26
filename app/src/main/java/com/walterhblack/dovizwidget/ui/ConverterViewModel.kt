package com.walterhblack.dovizwidget.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.walterhblack.dovizwidget.data.RateRepository
import com.walterhblack.dovizwidget.widget.publishWidgetState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

// ViewModel, ekran döndüğünde de veriyi ve yüklenme durumunu korur.
class ConverterViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RateRepository(application)
    var snapshot by mutableStateOf(repository.cached()); private set
    var loading by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    var favorites by mutableStateOf(repository.favorites()); private set
    var theme by mutableStateOf(repository.theme()); private set
    var uiScale by mutableStateOf(repository.uiScale()); private set

    init {
        val cachedAt = snapshot?.fetchedAt ?: 0L
        if (cachedAt == 0L || System.currentTimeMillis() - cachedAt >= 6L.hoursMillis) refresh()
    }

    fun refresh() {
        if (loading) return
        loading = true
        error = null
        viewModelScope.launch {
            try {
                snapshot = repository.refresh()
                publishWidgetState(getApplication())
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                error = if (snapshot == null) "Kurlar alınamadı. İnternet bağlantını kontrol edip yeniden dene."
                    else "Güncellenemedi. Son kaydedilen kurlar gösteriliyor."
            } finally { loading = false }
        }
    }

    fun toggleFavorite(code: String) {
        favorites = if (code in favorites) favorites - code else favorites + code
        repository.setFavorites(favorites)
        viewModelScope.launch { publishWidgetState(getApplication()) }
    }

    fun setAppearance(value: String) { theme = value; repository.setTheme(value) }
    fun setInterfaceScale(value: String) { uiScale = value; repository.setUiScale(value) }
}

private val Long.hoursMillis: Long get() = this * 60L * 60L * 1000L
