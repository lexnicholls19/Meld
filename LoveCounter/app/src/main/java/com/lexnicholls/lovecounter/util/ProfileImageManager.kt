package com.lexnicholls.lovecounter.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream

object ProfileImageManager {
    private const val TAG = "ProfileImageManager"
    private const val DEFAULT_PROFILE_FILE = "profile_pic.jpg"

    private fun getFileName(relationId: String?): String {
        return if (relationId == null) DEFAULT_PROFILE_FILE else "profile_pic_${relationId}.jpg"
    }

    /**
     * Saves the image to internal app storage for offline availability.
     */
    fun saveToInternalStorage(context: Context, uri: Uri, relationId: String? = null): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val file = File(context.filesDir, getFileName(relationId))
            val outputStream = FileOutputStream(file)
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            file
        } catch (e: Exception) {
            Log.e(TAG, "Error saving image to internal storage", e)
            null
        }
    }

    /**
     * Gets the local profile picture file if it exists.
     */
    fun getLocalProfileFile(context: Context, relationId: String? = null): File? {
        val file = File(context.filesDir, getFileName(relationId))
        return if (file.exists()) file else null
    }

    /**
     * Uploads the image to Firebase Storage and updates Firestore.
     */
    suspend fun uploadToCloud(context: Context, uri: Uri, relationId: String? = null): String? {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return null
        val fileName = if (relationId != null) "${uid}_${relationId}.jpg" else "${uid}.jpg"
        val storageRef = FirebaseStorage.getInstance().reference.child("profile_pics/$fileName")
        
        return try {
            storageRef.putFile(uri).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            
            // Update Firestore
            // Note: If we want per-relation profile pics in Firestore, 
            // we should store them in the relation metadata or the user's specific profile within that relation.
            // For now, let's just update the main one if no relationId, 
            // but the request is specifically for "per profile" (meaning relation in this app's context).
            
            if (relationId == null) {
                FirebaseFirestore.getInstance().collection("users").document(uid)
                    .update("profilePicUrl", downloadUrl).await()
            }
            // If relationId is present, we might need a different place to store it in Firestore 
            // if we want it to sync across devices for that specific profile.
            // However, the user request focuses on selecting a different one per profile.
            
            downloadUrl
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading to Firebase Storage", e)
            null
        }
    }

    /**
     * Resolves which URI to use: Local file (offline priority) or Cloud URL.
     */
    fun getBestProfileUri(context: Context, cloudUrl: String?, relationId: String? = null): Any? {
        val localFile = getLocalProfileFile(context, relationId)
        return if (localFile != null) {
            localFile
        } else {
            cloudUrl
        }
    }
}
