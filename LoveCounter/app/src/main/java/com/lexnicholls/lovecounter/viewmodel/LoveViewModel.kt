package com.lexnicholls.lovecounter.viewmodel

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.lexnicholls.lovecounter.data.repository.QuestionsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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

    private val _syncStatus = mutableStateOf<String?>(null)
    val syncStatus: State<String?> = _syncStatus

    private val privateUserIds = setOf(
        "CX4z9DcQYxTJeaIdyNgzpDQqw6U2", // Alexander
        "pW562p0UqNfEicrVd0q3oRRE9373"  // Laura
    )

    init {
        // Cargar preguntas desde Firebase
        viewModelScope.launch {
            QuestionsRepository.loadQuestions(db)
        }

        // Hilo que actualiza el tiempo cada segundo
        viewModelScope.launch {
            while (true) {
                _currentTime.value = LocalDateTime.now()
                delay(1000)
            }
        }
    }

    /**
     * Sincroniza las preguntas locales a Firebase Firestore.
     * Solo funciona para Alexander o Laura.
     */
    fun syncQuestionsToFirebase() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId !in privateUserIds) {
            Log.e("Sync", "Acceso denegado: Solo administradores pueden sincronizar.")
            return
        }

        viewModelScope.launch {
            try {
                _isRefreshing.value = true
                _syncStatus.value = "Sincronizando..."
                val batch = db.batch()
                
                // 1. Sincronizar Set Privado
                val privateQs = QuestionsRepository.getAllPrivateQuestions()
                privateQs.forEachIndexed { index, text ->
                    val docRef = db.collection("questions_admin").document("private_set").collection("items").document("q_$index")
                    batch.set(docRef, mapOf("text" to text, "order" to index))
                }

                // 2. Sincronizar Set Público
                val publicQs = QuestionsRepository.getAllPublicQuestions()
                publicQs.forEachIndexed { index, text ->
                    val docRef = db.collection("questions_admin").document("public_set").collection("items").document("q_$index")
                    batch.set(docRef, mapOf("text" to text, "order" to index))
                }

                batch.commit().await()
                Log.d("Sync", "Sincronización exitosa: ${privateQs.size} privadas y ${publicQs.size} públicas.")
                _syncStatus.value = "Sincronización exitosa"
            } catch (e: Exception) {
                Log.e("Sync", "Error sincronizando preguntas", e)
                _syncStatus.value = "Error: ${e.message}"
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun clearSyncStatus() {
        _syncStatus.value = null
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
