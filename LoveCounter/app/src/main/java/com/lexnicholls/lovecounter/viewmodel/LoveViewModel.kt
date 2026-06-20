package com.lexnicholls.lovecounter.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class LoveViewModel @Inject constructor(
    private val db: FirebaseFirestore
) : ViewModel() {
    val startDate: LocalDateTime = LocalDateTime.of(2021, 1, 4, 0, 0)
    
    private val _currentTime = mutableStateOf(LocalDateTime.now())
    val currentTime: State<LocalDateTime> = _currentTime

    private val _partnerStatus = mutableStateOf("❓")
    val partnerStatus: State<String> = _partnerStatus

    private val _myStatus = mutableStateOf("😊")
    val myStatus: State<String> = _myStatus

    private val _isRefreshing = mutableStateOf(false)
    val isRefreshing: State<Boolean> = _isRefreshing

    init {
        // Hilo que actualiza el tiempo cada segundo
        viewModelScope.launch {
            while (true) {
                _currentTime.value = LocalDateTime.now()
                delay(1000)
            }
        }
    }

    fun listenToStatuses(userName: String) {
        if (userName.isBlank()) return
        
        db.collection("partner_status")
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener

                snapshot.documents.forEach { doc ->
                    val status = doc.getString("emoji") ?: "❓"
                    val docId = doc.id
                    
                    if (docId == userName) {
                        _myStatus.value = status
                    } else if (!docId.contains("-") && docId.length < 25) {
                        _partnerStatus.value = status
                    }
                }
            }
    }

    fun updateStatus(userName: String, deviceId: String, emoji: String) {
        val data = hashMapOf(
            "emoji" to emoji,
            "timestamp" to Timestamp.now(),
            "senderName" to userName,
            "userName" to userName,
            "senderId" to deviceId,
            "deviceId" to deviceId
        )
        db.collection("partner_status").document(userName).set(data)
    }

    fun refresh() {
        _isRefreshing.value = true
        db.collection("partner_status").get().addOnCompleteListener {
            _isRefreshing.value = false
        }
    }
}
