package com.patreze.rgbplaygestao

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.json.JSONArray
import java.util.Calendar

class VencimentoWorker(
    private val contexto: Context,
    parametros: WorkerParameters
) : Worker(contexto, parametros) {

    override fun doWork(): Result {
        val prefs = contexto.getSharedPreferences("rgb_play_gestao", Context.MODE_PRIVATE)
        val texto = prefs.getString("clientes", "[]") ?: "[]"
        val hoje = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        try {
            val array = JSONArray(texto)
            val clientesNotificar = mutableListOf<String>()

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val nome = obj.optString("nome")
                val dia = obj.optInt("dia")
                val ultimoMesPago = obj.optString("ultimoMesPago", "")

                val pendente = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                if (ultimoMesPago.isNotEmpty()) {
                    val partes = ultimoMesPago.split("-")
                    if (partes.size == 2) {
                        val ano = partes[0].toIntOrNull()
                        val mes = partes[1].toIntOrNull()
                        if (ano != null && mes != null) {
                            pendente.set(ano, mes - 1, 1)
                            pendente.add(Calendar.MONTH, 1)
                            val ultDia = pendente.getActualMaximum(Calendar.DAY_OF_MONTH)
                            pendente.set(Calendar.DAY_OF_MONTH, minOf(dia, ultDia))
                        }
                    }
                } else {
                    val ultDia = pendente.getActualMaximum(Calendar.DAY_OF_MONTH)
                    pendente.set(Calendar.DAY_OF_MONTH, minOf(dia, ultDia))
                }

                val dias = (pendente.timeInMillis - hoje.timeInMillis) / (1000L * 60L * 60L * 24L)

                if (dias <= 3L) {
                    clientesNotificar.add(nome)
                }
            }

            if (clientesNotificar.isNotEmpty()) {
                NotificacaoVencimento.mostrar(contexto, clientesNotificar)
            }
        } catch (_: Exception) {
            return Result.failure()
        }

        return Result.success()
    }
}
