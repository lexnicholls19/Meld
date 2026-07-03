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
    val relationId: String? = null,
    val relationIds: List<String> = emptyList(),
    val profilePicUrl: String? = null
)

@HiltViewModel
class LoveViewModel @Inject constructor(
    private val db: FirebaseFirestore
) : ViewModel() {

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

    private val _relationIds = mutableStateOf<List<String>>(emptyList())
    val relationIds: State<List<String>> = _relationIds

    private val _sharedId = mutableStateOf<String?>(FirebaseAuth.getInstance().currentUser?.uid)
    val sharedId: State<String?> = _sharedId

    private val _linkingCode = mutableStateOf<String?>(null)
    val linkingCode: State<String?> = _linkingCode

    private val _isLinking = mutableStateOf(false)
    val isLinking: State<Boolean> = _isLinking

    private val _members = mutableStateOf<List<User>>(emptyList())
    val members: State<List<User>> = _members

    private val _currentUserProfile = mutableStateOf<User?>(null)
    val currentUserProfile: State<User?> = _currentUserProfile

    private val _availableRelations = mutableStateOf<Map<String, String>>(emptyMap())
    val availableRelations: State<Map<String, String>> = _availableRelations

    private val _incomingMessage = mutableStateOf<Pair<String, String>?>(null)
    val incomingMessage: State<Pair<String, String>?> = _incomingMessage

    private var membersListener: ListenerRegistration? = null
    private var relationsListener: ListenerRegistration? = null
    private var currentRelationListener: ListenerRegistration? = null
    private val messagesListeners = mutableMapOf<String, ListenerRegistration>()

    private val privateUserIds = setOf(
        "66817fc6-c66c-49bb-b0fe-089cbbe70b2c", // Alexander (Relation ID as Fallback)
        "pW562p0UqNfEicrVd0q3oRRE9373"  // Laura
    )

    private val ADMIN_RELATION_ID = "66817fc6-c66c-49bb-b0fe-089cbbe70b2c"

    init {
        // Cargar preguntas públicas inicialmente
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
        val currentRelationId = _relationId.value
        
        if (currentUserId !in privateUserIds && currentRelationId != ADMIN_RELATION_ID) {
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
                    val user = snapshot.toObject(User::class.java)?.copy(uid = snapshot.id)
                    _currentUserProfile.value = user

                    val pId = snapshot.getString("partnerId")
                    val rId = snapshot.getString("relationId")
                    val rIds = snapshot.get("relationIds") as? List<String> ?: emptyList()
                    
                    if (rId != null && !rIds.contains(rId)) {
                        db.collection("users").document(currentUserId)
                            .update("relationIds", com.google.firebase.firestore.FieldValue.arrayUnion(rId))
                    }

                    // Prioridad al relationId para grupos. Si no existe, usamos el partnerId legacy.
                    // Si no hay ninguno, usamos el UID propio.
                    val sId = rId ?: if (pId != null) {
                        listOf(currentUserId, pId).sorted().joinToString("_")
                    } else {
                        currentUserId
                    }
                    _sharedId.value = sId

                    _relationId.value = rId
                    
                    // Construir lista de todos los IDs de relación a monitorear
                    val allMonitoredIds = rIds.toMutableList()
                    if (rId != null && !allMonitoredIds.contains(rId)) allMonitoredIds.add(rId)
                    if (sId != currentUserId && !allMonitoredIds.contains(sId)) allMonitoredIds.add(sId)
                    
                    _relationIds.value = allMonitoredIds
                    
                    // Escuchar detalles de la relación actual (incluyendo linkingCode por perfil)
                    observeCurrentRelation(rId, snapshot.getString("linkingCode"))

                    // Escuchar mensajes de TODAS las relaciones
                    listenToAllQuickMessages(allMonitoredIds)

                    // Cargar nombres de relaciones
                    observeRelations(allMonitoredIds)

                    // Cargar preguntas específicas si es admin
                    if (rId == ADMIN_RELATION_ID) {
                        viewModelScope.launch {
                            QuestionsRepository.loadQuestions(db, rId)
                        }
                    }
                    
                    if (rId != null) {
                        observeMembers(rId)
                    } else if (pId != null) {
                        // Soporte para legado (pId)
                        observeMembers(sId)
                    } else {
                        _members.value = emptyList()
                        membersListener?.remove()
                    }
                }
            }
    }

    private fun observeRelations(rIds: List<String>) {
        if (rIds.isEmpty()) {
            _availableRelations.value = emptyMap()
            relationsListener?.remove()
            return
        }

        relationsListener?.remove()
        relationsListener = db.collection("relations")
            .whereIn(com.google.firebase.firestore.FieldPath.documentId(), rIds.take(10))
            .addSnapshotListener { snapshot, _ ->
                val namesMap = mutableMapOf<String, String>()
                rIds.forEach { id ->
                    namesMap[id] = "Relación ${id.take(4)}"
                }
                snapshot?.documents?.forEach { doc ->
                    namesMap[doc.id] = doc.getString("name") ?: "Relación ${doc.id.take(4)}"
                }
                _availableRelations.value = namesMap
            }
    }

    private fun observeCurrentRelation(rId: String?, userLinkingCode: String?) {
        currentRelationListener?.remove()
        if (rId == null) {
            _linkingCode.value = userLinkingCode
            return
        }
        currentRelationListener = db.collection("relations").document(rId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    // Si el documento de relación tiene un código, ese es el que mostramos.
                    // Si no tiene, mostramos null (o el del usuario como fallback legacy si aplica)
                    _linkingCode.value = snapshot.getString("linkingCode")
                } else {
                    _linkingCode.value = userLinkingCode
                }
            }
    }

    private fun listenToAllQuickMessages(rIds: List<String>) {
        // Remover listeners de relaciones que ya no están
        val toRemove = messagesListeners.keys.filter { !rIds.contains(it) }
        toRemove.forEach { 
            messagesListeners[it]?.remove()
            messagesListeners.remove(it)
        }

        // Usamos un margen de 1 minuto hacia atrás para evitar perder mensajes por desfase de reloj
        val startTime = Timestamp(System.currentTimeMillis() / 1000 - 60, 0)
        
        rIds.forEach { id ->
            if (!messagesListeners.containsKey(id)) {
                Log.d("LoveVM", "Iniciando listener de mensajes para relación: $id")
                val listener = db.collection("relations").document(id)
                    .collection("quick_messages")
                    .whereGreaterThan("timestamp", startTime)
                    .addSnapshotListener { snapshot, e ->
                        if (e != null) {
                            Log.e("LoveVM", "Error en listener de mensajes ($id): ${e.message}")
                            return@addSnapshotListener
                        }
                        
                        snapshot?.documentChanges?.forEach { change ->
                            if (change.type == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                                val doc = change.document
                                val senderUid = doc.getString("senderUid")
                                val timestamp = doc.getTimestamp("timestamp")
                                
                                // Solo procesar si no lo enviamos nosotros y es realmente nuevo (posterior al inicio del listener real)
                                if (senderUid != FirebaseAuth.getInstance().currentUser?.uid && 
                                    timestamp != null && timestamp.seconds >= startTime.seconds) {
                                    val title = doc.getString("name") ?: ""
                                    val value = doc.getString("value") ?: ""
                                    Log.d("LoveVM", "Nuevo mensaje recibido de $id: $title - $value")
                                    _incomingMessage.value = title to value
                                }
                            }
                        }
                    }
                messagesListeners[id] = listener
            }
        }
    }

    fun clearIncomingMessage() {
        _incomingMessage.value = null
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
        relationsListener?.remove()
        currentRelationListener?.remove()
        messagesListeners.values.forEach { it.remove() }
        messagesListeners.clear()
    }

    fun generateLinkingCode() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId == null) {
            Log.e("Partner", "User not authenticated")
            _syncStatus.value = "Error: Usuario no autenticado"
            return
        }
        
        val rId = _relationId.value

        viewModelScope.launch {
            try {
                val batch = db.batch()
                
                // Si ya existe un código activo, lo eliminamos de la colección global
                _linkingCode.value?.let { oldCode ->
                    batch.delete(db.collection("linking_codes").document(oldCode))
                }
                
                val newCode = (1..16).map { (0..9).random() }.joinToString("")
                val targetRelationId = rId ?: currentUserId

                // Actualizar según corresponda: en la relación o en el usuario (legacy)
                if (rId != null) {
                    batch.set(db.collection("relations").document(rId), mapOf("linkingCode" to newCode), SetOptions.merge())
                } else {
                    batch.set(db.collection("users").document(currentUserId), mapOf("linkingCode" to newCode), SetOptions.merge())
                }
                
                // Guardar en colección global para búsqueda
                batch.set(db.collection("linking_codes").document(newCode), mapOf(
                    "userId" to currentUserId,
                    "relationId" to targetRelationId
                ))
                
                batch.commit().await()
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

        Log.d("Partner", "Intentando vincular con código: $code")
        viewModelScope.launch {
            try {
                _isLinking.value = true
                val result = db.collection("linking_codes").document(code).get().await()
                
                if (!result.exists()) {
                    Log.e("Partner", "El código $code no existe en la colección linking_codes")
                    _syncStatus.value = "Código inválido o expirado"
                    return@launch
                }

                val creatorUid = result.getString("userId")
                val targetRelationId = result.getString("relationId")
                
                Log.d("Partner", "Código encontrado. Creador: $creatorUid, Relación Objetivo: $targetRelationId")

                if (creatorUid == currentUserId) {
                    Log.e("Partner", "Error: El usuario intenta vincularse con su propio código")
                    _syncStatus.value = "No puedes vincularte con tu propio código"
                    return@launch
                }

                if (creatorUid != null && targetRelationId != null) {
                    val batch = db.batch()
                    
                    // 1. Me uno a la relación
                    batch.set(db.collection("users").document(currentUserId), mapOf(
                        "relationId" to targetRelationId,
                        "relationIds" to com.google.firebase.firestore.FieldValue.arrayUnion(targetRelationId)
                    ), SetOptions.merge())
                    
                    // 2. Si el creador no tenía relationId aún, se lo asignamos
                    batch.set(db.collection("users").document(creatorUid), mapOf(
                        "relationId" to targetRelationId,
                        "relationIds" to com.google.firebase.firestore.FieldValue.arrayUnion(targetRelationId)
                    ), SetOptions.merge())

                    // Nota: No borramos el código para permitir que más personas se unan 
                    // (útil para grupos de amigos o familia). El creador puede regenerarlo si desea.
                    
                    batch.commit().await()
                    Log.d("Partner", "Vinculación exitosa con relación $targetRelationId")
                    _syncStatus.value = "¡Enlazado con éxito!"
                } else {
                    Log.e("Partner", "Error: Datos del código incompletos en Firebase")
                    _syncStatus.value = "Código inválido (datos incompletos)"
                }
            } catch (e: Exception) {
                Log.e("Partner", "Error linking with partner", e)
                _syncStatus.value = "Error: ${e.message}"
            } finally {
                _isLinking.value = false
            }
        }
    }

    fun switchProfile(newRelationId: String) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                db.collection("users").document(currentUserId)
                    .update("relationId", newRelationId).await()
                _syncStatus.value = "Perfil cambiado"
            } catch (e: Exception) {
                Log.e("Profile", "Error switching profile", e)
                _syncStatus.value = "Error al cambiar perfil"
            }
        }
    }

    fun createProfile(name: String) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val newRelationId = java.util.UUID.randomUUID().toString()
                val batch = db.batch()
                
                // Crear documento de relación
                batch.set(db.collection("relations").document(newRelationId), mapOf("name" to name))
                
                // Actualizar usuario
                batch.set(db.collection("users").document(currentUserId), mapOf(
                    "relationId" to newRelationId,
                    "relationIds" to com.google.firebase.firestore.FieldValue.arrayUnion(newRelationId)
                ), SetOptions.merge())
                
                batch.commit().await()
                _syncStatus.value = "Perfil creado: $name"
            } catch (e: Exception) {
                Log.e("Profile", "Error creating profile", e)
                _syncStatus.value = "Error al crear perfil"
            }
        }
    }

    fun renameRelation(relationId: String, newName: String) {
        viewModelScope.launch {
            try {
                db.collection("relations").document(relationId)
                    .set(mapOf("name" to newName), SetOptions.merge()).await()
                _syncStatus.value = "Relación renombrada"
            } catch (e: Exception) {
                Log.e("Profile", "Error renaming relation", e)
                _syncStatus.value = "Error al renombrar"
            }
        }
    }

    fun unlinkPartner() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId == null) {
            _syncStatus.value = "Error: Usuario no autenticado"
            return
        }
        
        val rId = _relationId.value

        viewModelScope.launch {
            try {
                val batch = db.batch()
                batch.set(db.collection("users").document(currentUserId), mapOf(
                    "relationId" to null,
                    "relationIds" to if (rId != null) com.google.firebase.firestore.FieldValue.arrayRemove(rId) else emptyList<String>(),
                    "partnerId" to null
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

    fun updateProfile(name: String, profilePicUrl: String? = null) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val updates = mutableMapOf<String, Any>("name" to name)
        profilePicUrl?.let { updates["profilePicUrl"] = it }
        db.collection("users").document(uid).update(updates)
    }

    fun refresh() {
        _isRefreshing.value = true
        db.collection("partner_status").get().addOnCompleteListener {
            _isRefreshing.value = false
        }
    }
}
