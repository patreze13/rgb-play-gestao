package com.patreze.rgbplaygestao

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.json.JSONArray
import java.util.Calendar

class VencimentoWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {

    override fun doWork(): Result {

        val preferencias =
            applicationContext.getSharedPreferences(
                "rgb_play_gestao",
                Context.MODE_PRIVATE
            )

        val texto =
            preferencias.getString(
                "clientes",
                "[]"
            ) ?: "[]"

        return try {

            val clientes = JSONArray(texto)

            val hoje = Calendar.getInstance()
            val diaHoje =
                hoje.get(Calendar.DAY_OF_MONTH)

            for (i in 0 until clientes.length()) {

                val cliente =
                    clientes.getJSONObject(i)

                val nome =
                    cliente.optString("nome")

                val dia =
                    cliente.optInt("dia")

                if (
                    nome.isNotBlank() &&
                    dia == diaHoje
                ) {

                    NotificacaoVencimento.mostrar(
                        applicationContext,
                        "O cliente $nome vence hoje."
                    )
                }
            }

            Result.success()

        } catch (_: Exception) {

            Result.failure()
        }
    }
}
