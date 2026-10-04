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
    var amount by mutableStateOf("0,00"); private set
    var amountIsConversion by mutableStateOf(false); private set

    fun editAmount(value: String) {
        amount = value
        amountIsConversion = false
    }
    var favorites by mutableStateOf(repository.favorites()); private set
    var homeFavorites by mutableStateOf(repository.homeFavorites()); private set
    var theme by mutableStateOf(repository.theme()); private set
    var uiScale by mutableStateOf(repository.uiScale()); private set
    var sourceCurrency by mutableStateOf(repository.sourceCurrency().takeIf { it in homeFavorites }
        ?: homeFavorites.first()); private set

    init {
        repository.setFavorites(favorites)
        repository.setSourceCurrency(sourceCurrency)
        viewModelScope.launch { publishWidgetState(getApplication()) }
        val cachedAt = snapshot?.fetchedAt ?: 0L
        if (cachedAt == 0L || System.currentTimeMillis() - cachedAt >= 6L.hoursMillis ||
            snapshot?.rates?.keys != com.walterhblack.dovizwidget.data.currencyNames.keys) refresh()
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
        if (code !in com.walterhblack.dovizwidget.data.currencyNames) return
        if (code !in favorites && favorites.size >= 4) return
        favorites = if (code in favorites) favorites - code else favorites + code
        repository.setFavorites(favorites)
        viewModelScope.launch { publishWidgetState(getApplication()) }
    }

    fun toggleHomeFavorite(code: String) {
        if (code !in com.walterhblack.dovizwidget.data.currencyNames) return
        if (code in homeFavorites && homeFavorites.size == 1) return
        updateHomeFavorites(if (code in homeFavorites) homeFavorites - code else homeFavorites + code)
    }

    fun replaceHomeCurrency(oldCode: String, newCode: String) {
        if (newCode !in com.walterhblack.dovizwidget.data.currencyNames) return
        val index = homeFavorites.indexOf(oldCode)
        if (index < 0) return
        val previousIndex = homeFavorites.indexOf(newCode)
        val updated = homeFavorites.toMutableList().apply {
            this[index] = newCode
            if (previousIndex >= 0) this[previousIndex] = oldCode
        }
        if (sourceCurrency == oldCode && !setSelectedSourceCurrency(newCode)) return
        updateHomeFavorites(updated)
    }

    fun moveHomeCurrency(code: String, targetCode: String) {
        val from = homeFavorites.indexOf(code)
        val to = homeFavorites.indexOf(targetCode)
        if (from < 0 || to < 0 || from == to) return
        updateHomeFavorites(homeFavorites.toMutableList().apply { add(to, removeAt(from)) })
    }

    private fun updateHomeFavorites(codes: List<String>) {
        if (sourceCurrency !in codes && !setSelectedSourceCurrency(codes.first())) return
        homeFavorites = codes
        repository.setHomeFavorites(codes)
    }

    fun setAppearance(value: String) { theme = value; repository.setTheme(value) }
    fun setInterfaceScale(value: String) { uiScale = value; repository.setUiScale(value) }
    fun setSelectedSourceCurrency(value: String): Boolean {
        if (value in com.walterhblack.dovizwidget.data.currencyNames) {
            if (value == sourceCurrency) return true
            val converted = runCatching {
                com.walterhblack.dovizwidget.data.WidgetCalculator.changeCurrency(amount, sourceCurrency, value, snapshot)
            }
            if (converted.isFailure) {
                error = converted.exceptionOrNull()?.message ?: "Para birimi değiştirilemedi."
                return false
            }
            amount = converted.getOrThrow()
            amountIsConversion = true
            sourceCurrency = value
            repository.setSourceCurrency(value)
            error = null
            return true
        }
        return false
    }
}

private val Long.hoursMillis: Long get() = this * 60L * 60L * 1000L
