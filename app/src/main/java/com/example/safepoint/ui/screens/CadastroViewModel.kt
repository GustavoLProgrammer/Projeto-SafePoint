package com.example.safepoint.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class CadastroViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _cadastroSucesso = MutableStateFlow(false)
    val cadastroSucesso: StateFlow<Boolean> = _cadastroSucesso

    fun cadastrarUsuario(
        nome: String,
        email: String,
        telefone: String,
        senha: String,
        dataNascimento: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                // 1. Criar usuário no Firebase Auth
                val result = auth.createUserWithEmailAndPassword(email, senha).await()
                val userId = result.user?.uid

                if (userId != null) {
                    // 2. Salvar os dados extras no Firestore
                    val userMap = hashMapOf(
                        "nome" to nome,
                        "email" to email,
                        "telefone" to telefone,
                        "dataNascimento" to dataNascimento,
                        "uid" to userId
                    )

                    db.collection("usuarios")
                        .document(userId)
                        .set(userMap)
                        .await()

                    _cadastroSucesso.value = true
                } else {
                    _errorMessage.value = "Erro ao criar usuário."
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Erro desconhecido ao cadastrar."
            } finally {
                _isLoading.value = false
            }
        }
    }
}