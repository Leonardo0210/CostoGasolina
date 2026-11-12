
package com.mexiti.costogasolina

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mexiti.costogasolina.ui.theme.CostoGasolinaTheme
import kotlinx.coroutines.launch
import java.text.NumberFormat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CostoGasolinaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CostGasLayout()
                }
            }
        }
    }
}

@Composable
fun CostGasLayout() {
    val context = LocalContext.current
    val dao = remember {
        GasolineDatabase.getDatabase(context).gasolineDao()
    }

    val registros by dao.getAll().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var precio by remember { mutableStateOf("") }
    var litros by remember { mutableStateOf("") }
    var propina by remember { mutableStateOf("") }
    var darPropina by remember { mutableStateOf(false) }
    var idEditando by remember { mutableStateOf<Int?>(null) }
    var mensajeError by remember { mutableStateOf("") }

    val precioNumero = precio.toDoubleOrNull() ?: 0.0
    val litrosNumero = litros.toDoubleOrNull() ?: 0.0
    val propinaNumero = if (darPropina) propina.toDoubleOrNull() ?: 0.0 else 0.0
    val total = precioNumero * litrosNumero + propinaNumero

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Calculadora de Gasolina",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = precio,
                onValueChange = { precio = it },
                label = { Text("Precio por litro") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = litros,
                onValueChange = { litros = it },
                label = { Text("Cantidad de litros") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = propina,
                onValueChange = { propina = it },
                label = { Text("Propina") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Agregar propina")
                Switch(
                    checked = darPropina,
                    onCheckedChange = { darPropina = it }
                )
            }

            Text(
                text = "Total: ${NumberFormat.getCurrencyInstance().format(total)}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            if (mensajeError.isNotEmpty()) {
                Text(
                    text = mensajeError,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = {
                    if (precioNumero <= 0 || litrosNumero <= 0 || propinaNumero < 0) {
                        mensajeError = "Ingresa un precio y una cantidad válidos."
                    } else {
                        val registro = GasolineTransaction(
                            id = idEditando ?: 0,
                            pricePerLiter = precioNumero,
                            liters = litrosNumero,
                            tip = propinaNumero,
                            total = total
                        )

                        scope.launch {
                            if (idEditando == null) {
                                dao.insert(registro)
                            } else {
                                dao.update(registro)
                            }

                            precio = ""
                            litros = ""
                            propina = ""
                            darPropina = false
                            idEditando = null
                            mensajeError = ""
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (idEditando == null) "Guardar registro" else "Actualizar registro")
            }

            if (idEditando != null) {
                TextButton(
                    onClick = {
                        precio = ""
                        litros = ""
                        propina = ""
                        darPropina = false
                        idEditando = null
                        mensajeError = ""
                    }
                ) {
                    Text("Cancelar edición")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Historial de registros",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        if (registros.isEmpty()) {
            item {
                Text("Todavía no hay registros guardados.")
            }
        }

        items(registros, key = { it.id }) { registro ->
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Registro #${registro.id}", fontWeight = FontWeight.Bold)
                    Text("Precio por litro: ${registro.pricePerLiter}")
                    Text("Litros: ${registro.liters}")
                    Text("Propina: ${registro.tip}")
                    Text(
                        "Total: ${NumberFormat.getCurrencyInstance().format(registro.total)}",
                        fontWeight = FontWeight.Bold
                    )

                    Row {
                        TextButton(
                            onClick = {
                                precio = registro.pricePerLiter.toString()
                                litros = registro.liters.toString()
                                propina = registro.tip.toString()
                                darPropina = registro.tip > 0
                                idEditando = registro.id
                                mensajeError = ""
                            }
                        ) {
                            Text("Editar")
                        }

                        TextButton(
                            onClick = {
                                scope.launch {
                                    dao.delete(registro)
                                }
                            }
                        ) {
                            Text("Eliminar")
                        }
                    }
                }
            }
        }
    }
}
