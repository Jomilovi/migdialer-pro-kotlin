package com.migdialer.pro.ui.recents

import android.content.Context
import androidx.lifecycle.*
import com.migdialer.pro.data.model.CallEntry
import com.migdialer.pro.data.model.CallType
import com.migdialer.pro.data.repository.CallLogRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class RecentsViewModel(private val repo: CallLogRepository) : ViewModel() {

    private val _items = MutableLiveData<List<CallListItem>>(emptyList())
    val items: LiveData<List<CallListItem>> = _items

    fun load() {
        viewModelScope.launch {
            val calls = repo.getCallLog()
            _items.value = groupByDate(calls)
        }
    }

    /**
     * Groups calls by date and inserts date headers.
     * Format: "Hoy", "Ayer", "Lun 11/05/2026", etc.
     */
    private fun groupByDate(calls: List<CallEntry>): List<CallListItem> {
        if (calls.isEmpty()) return emptyList()

        val result = mutableListOf<CallListItem>()
        var lastDateLabel = ""

        val today     = startOfDay(System.currentTimeMillis())
        val yesterday = today - 86_400_000L
        val thisWeek  = today - 7 * 86_400_000L

        val dayFmt  = SimpleDateFormat("EEEE dd/MM/yyyy", Locale("es", "ES"))
        val shortFmt = SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES"))

        calls.forEach { call ->
            val callDay = startOfDay(call.date)
            val label = when {
                callDay >= today     -> "Hoy"
                callDay >= yesterday -> "Ayer"
                callDay >= thisWeek  -> {
                    // Day of week + date
                    dayFmt.format(Date(call.date)).replaceFirstChar { it.uppercase() }
                }
                else -> shortFmt.format(Date(call.date))
            }

            if (label != lastDateLabel) {
                result.add(CallListItem.Header(label))
                lastDateLabel = label
            }
            result.add(CallListItem.Entry(call))
        }

        return result
    }

    private fun startOfDay(millis: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}

class RecentsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return RecentsViewModel(CallLogRepository(context)) as T
    }
}
