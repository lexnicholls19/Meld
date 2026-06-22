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
    private const val LOCAL_PROFILE_FILE = "profile_pic.jpg"

    /**
     * Saves the image to internal app storage for offline availability.
     */
    fun saveToInternalStorage(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val file = File(context.filesDir, LOCAL_PROFILE_FILE)
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
    fun getLocalProfileFile(context: Context): File? {
        val file = File(context.filesDir, LOCAL_PROFILE_FILE)
        return if (file.exists()) file else null
    }

    /**
     * Uploads the image to Firebase Storage and updates Firestore.
     */
    suspend fun uploadToCloud(context: Context, uri: Uri): String? {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return null
        val storageRef = FirebaseStorage.getInstance().reference.child("profile_pics/$uid.jpg")
        
        return try {
            storageRef.putFile(uri).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            
            // Update Firestore
            FirebaseFirestore.getInstance().collection("users").document(uid)
                .update("profilePicUrl", downloadUrl).await()
            
            downloadUrl
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading to Firebase Storage", e)
            null
        }
    }

    /**
     * Resolves which URI to use: Local file (offline priority) or Cloud URL.
     */
    fun getBestProfileUri(context: Context, cloudUrl: String?): Any? {
        val localFile = getLocalProfileFile(context)
        return if (localFile != null) {
            localFile
        } else {
            cloudUrl
        }
    }
}
