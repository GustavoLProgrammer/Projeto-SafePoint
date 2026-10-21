package com.example.safepoint.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun AdicionarDesastreDialog(
    onDismiss: () -> Unit,
    onAdicionar: (titulo: String, endereco: String, lat: Double, lng: Double) -> Unit
) {
    var titulo by remember { mutableStateOf("") }
    var endereco by remember { mutableStateOf("") }
    var lat by remember { mutableStateOf("-23.5505") }
    var lng by remember { mutableStateOf("-46.6333") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Adicionar Desastre", fontWeight = FontWeight.Bold, color = SafePointBlue)
        },
        text = {
            Column {
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Título (ex: Incêndio)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = endereco,
                    onValueChange = { endereco = it },
                    label = { Text("Endereço") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    OutlinedTextField(
                        value = lat,
                        onValueChange = { lat = it },
                        label = { Text("Latitude") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = lng,
                        onValueChange = { lng = it },
                        label = { Text("Longitude") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (titulo.isNotBlank() && endereco.isNotBlank()) {
                        onAdicionar(
                            titulo,
                            endereco,
                            lat.toDoubleOrNull() ?: -23.5505,
                            lng.toDoubleOrNull() ?: -46.6333
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SafePointGreen)
            ) {
                Text("Adicionar", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.Gray)
            }
        }
    )
}