package com.example.firebase_restapi.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.firebase_restapi.model.TempatWisata
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream

fun uploadImageToFirestore(
    firestore: FirebaseFirestore,
    context: Context,
    imageUri: Uri,
    tempatWisata: TempatWisata,
    onSuccess: (TempatWisata) -> Unit,
    onFailure: (Exception) -> Unit
) {
    try {
        // Save image locally
        val inputStream = context.contentResolver.openInputStream(imageUri)
        val file = File(context.filesDir, "img_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(file)

        inputStream?.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }

        val localImageUri = Uri.fromFile(file).toString()

        // Save to Firestore
        val data = hashMapOf(
            "nama" to tempatWisata.nama,
            "deskripsi" to tempatWisata.deskripsi,
            "gambarUriString" to localImageUri
        )

        firestore.collection("tempat_wisata")
            .document(tempatWisata.nama)
            .set(data)
            .addOnSuccessListener {
                onSuccess(tempatWisata.copy(gambarUriString = localImageUri))
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    } catch (e: Exception) {
        onFailure(e)
    }
}

suspend fun fetchTempatWisataFromFirestore(firestore: FirebaseFirestore): List<TempatWisata> {
    return try {
        val snapshot = firestore.collection("tempat_wisata").get().await()
        snapshot.documents.mapNotNull { doc ->
            TempatWisata(
                nama = doc.getString("nama") ?: "",
                deskripsi = doc.getString("deskripsi") ?: "",
                gambarUriString = doc.getString("gambarUriString")
            )
        }
    } catch (e: Exception) {
        Log.e("FirestoreUtils", "Error fetching data", e)
        emptyList()
    }
}
