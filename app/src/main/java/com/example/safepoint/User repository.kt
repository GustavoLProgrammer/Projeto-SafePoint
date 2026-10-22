package com.example.safepoint

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun buscarNomeUsuario(): String? {
        val userId = auth.currentUser?.uid ?: return null

        return try {
            val documento = firestore
                .collection("usuarios")
                .document(userId)
                .get()
                .await()

            documento.getString("nome")
        } catch (e: Exception) {
            null
        }
    }
}

