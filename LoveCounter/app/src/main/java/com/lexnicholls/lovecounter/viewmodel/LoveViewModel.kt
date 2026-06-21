package com.lexnicholls.lovecounter.viewmodel

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.lexnicholls.lovecounter.data.repository.QuestionsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDateTime
import javax.inject.Inject

data class User(
    val uid: String = "",
    val name: String = "",
    val deviceId: String = "",
    val relationId: String? = null
)

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

    private val _relationId = mutableStateOf<String?>(null)
    val relationId: State<String?> = _relationId

    private val _sharedId = mutableStateOf<String?>(FirebaseAuth.getInstance().currentUser?.uid)
    val sharedId: State<String?> = _sharedId

    private val _linkingCode = mutableStateOf<String?>(null)
    val linkingCode: State<String?> = _linkingCode

    private val _isLinking = mutableStateOf(false)
    val isLinking: State<Boolean> = _isLinking

    private val _members = mutableStateOf<List<User>>(emptyList())
    val members: State<List<User>> = _members

    private var membersListener: ListenerRegistration? = null

    private val privateUserIds = setOf(
        "CX4z9DcQYxTJeaIdyNgzpDQqw6U2", // Alexander
        "pW562p0UqNfEicrVd0q3oRRE9373"  // Laura
    )

    init {
        // Cargar preguntas desde Firebase
        viewModelScope.launch {
            QuestionsRepository.loadQuestions(db)
            observePartnerId()
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

    private fun observePartnerId() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.collection("users").document(currentUserId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val pId = snapshot.getString("partnerId")
                    val rId = snapshot.getString("relationId")
                    _relationId.value = rId
                    _linkingCode.value = snapshot.getString("linkingCode")
                    
                    if (rId != null) {
                        observeMembers(rId)
                    } else {
                        _members.value = emptyList()
                        membersListener?.remove()
                    }
                    
                    // Prioridad al relationId para grupos. Si no existe, usamos el partnerId legacy.
                    // Si no hay ninguno, usamos el UID propio.
                    _sharedId.value = rId ?: if (pId != null) {
                        listOf(currentUserId, pId).sorted().joinToString("_")
                    } else {
                        currentUserId
                    }
                }
            }
    }

    private fun observeMembers(rId: String) {
        membersListener?.remove()
        membersListener = db.collection("users")
            .whereEqualTo("relationId", rId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val userList = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(User::class.java)?.copy(uid = doc.id)
                    }
                    _members.value = userList
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        membersListener?.remove()
    }

    fun generateLinkingCode() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId == null) {
            Log.e("Partner", "User not authenticated")
            _syncStatus.value = "Error: Usuario no autenticado"
            return
        }
        
        viewModelScope.launch {
            try {
                val batch = db.batch()
                
                // Si ya existe un código, lo eliminamos de la colección global
                _linkingCode.value?.let { oldCode ->
                    batch.delete(db.collection("linking_codes").document(oldCode))
                }
                
                val newCode = (1..16).map { (0..9).random() }.joinToString("")
                
                // Determinamos qué ID compartir: 
                // Si ya estoy en una relación (relationId), comparto ese ID.
                // Si no, comparto mi propio UID para crear una nueva relación conmigo.
                val targetRelationId = _relationId.value ?: currentUserId

                // Actualizar usuario con el nuevo código
                batch.set(db.collection("users").document(currentUserId), mapOf("linkingCode" to newCode), SetOptions.merge())
                
                // Guardar en colección global para búsqueda, incluyendo el relationId meta
                batch.set(db.collection("linking_codes").document(newCode), mapOf(
                    "userId" to currentUserId,
                    "relationId" to targetRelationId
                ))
                
                batch.commit().await()
                _linkingCode.value = newCode
                Log.d("Partner", "Linking code generated successfully: $newCode")
            } catch (e: Exception) {
                Log.e("Partner", "Error generating code", e)
                _syncStatus.value = "Error: ${e.message}"
            }
        }
    }

    fun linkWithPartner(code: String) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId == null) {
            _syncStatus.value = "Error: Usuario no autenticado"
            return
        }

        viewModelScope.launch {
            try {
                _isLinking.value = true
                val result = db.collection("linking_codes").document(code).get().await()
                
                if (!result.exists()) {
                    _syncStatus.value = "Código inválido o expirado"
                    return@launch
                }

                val creatorUid = result.getString("userId")
                val targetRelationId = result.getString("relationId")

                if (creatorUid != null && creatorUid != currentUserId && targetRelationId != null) {
                    val batch = db.batch()
                    
                    // 1. Me uno a la relación (ya sea un grupo existente o el UID del creador)
                    batch.set(db.collection("users").document(currentUserId), mapOf("relationId" to targetRelationId), SetOptions.merge())
                    
                    // 2. Si el creador no tenía relationId aún (era una pareja nueva), se lo asignamos también
                    batch.set(db.collection("users").document(creatorUid), mapOf("relationId" to targetRelationId), SetOptions.merge())

                    // Limpiar código usado (Solo si es el creador quien lo hace o si queremos que sea de un solo uso)
                    // Para permitir múltiples dispositivos con el mismo código, NO lo borramos aquí.
                    // Opcionalmente, podemos dejar que expire por tiempo o que el creador lo regenere.
                    // batch.delete(db.collection("linking_codes").document(code))
                    
                    // Solo borramos el linkingCode del creador si queremos que sea de un solo uso.
                    // batch.set(db.collection("users").document(creatorUid), mapOf("linkingCode" to null), SetOptions.merge())
                    
                    // Limpiar mis códigos viejos si tenía
                    _linkingCode.value?.let { myOldCode ->
                        batch.delete(db.collection("linking_codes").document(myOldCode))
                    }
                    batch.set(db.collection("users").document(currentUserId), mapOf("linkingCode" to null), SetOptions.merge())
                    
                    batch.commit().await()
                    _syncStatus.value = "¡Enlazado con éxito!"
                } else {
                    _syncStatus.value = "Código inválido"
                }
            } catch (e: Exception) {
                Log.e("Partner", "Error linking with partner", e)
                _syncStatus.value = "Error: ${e.message}"
            } finally {
                _isLinking.value = false
            }
        }
    }

    fun unlinkPartner() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId == null) {
            _syncStatus.value = "Error: Usuario no autenticado"
            return
        }
        
        viewModelScope.launch {
            try {
                // Al salir de una relación de grupo, simplemente borramos nuestro relationId.
                // Nota: Esto no borra la relación para los demás, solo para ti.
                val batch = db.batch()
                batch.set(db.collection("users").document(currentUserId), mapOf(
                    "relationId" to null,
                    "partnerId" to null // Limpiamos también el legacy por si acaso
                ), SetOptions.merge())
                
                batch.commit().await()
                _syncStatus.value = "Has salido de la relación"
            } catch (e: Exception) {
                Log.e("Partner", "Error unlinking partner", e)
                _syncStatus.value = "Error al salir: ${e.message}"
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
