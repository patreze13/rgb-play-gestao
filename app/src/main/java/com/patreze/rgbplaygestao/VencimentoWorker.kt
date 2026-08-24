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

            val clientes =
                JSONArray(texto)

            val hoje =
                Calendar.getInstance()

            for (i in 0 until clientes.length()) {

                val cliente =
                    clientes.getJSONObject(i)

                val nome =
                    cliente.optString("nome")

                val dia =
                    cliente.optInt("dia")

                if (
                    nome.isNotBlank() &&
                    estaNosProximosTresDias(
                        dia,
                        hoje
                    )
                ) {

                    val vencimento =
                        proximoVencimento(
                            dia,
                            hoje
                        )

                    val dias =
                        diferencaEmDias(
                            hoje,
                            vencimento
                        )

                    val mensagem =
                        when (dias) {

                            0 ->
                                "O cliente $nome vence hoje."

                            1 ->
                                "O cliente $nome vence amanhã."

                            else ->
                                "O cliente $nome vence em $dias dias."
                        }

                    NotificacaoVencimento.mostrar(
                        applicationContext,
                        mensagem
                    )
                }
            }

            Result.success()

        } catch (_: Exception) {

            Result.failure()
        }
    }

    private fun estaNosProximosTresDias(
        dia: Int,
        hoje: Calendar
    ): Boolean {

        val vencimento =
            proximoVencimento(
                dia,
                hoje
            )

        val dias =
            diferencaEmDias(
                hoje,
                vencimento
            )

        return dias in 0..2
    }

    private fun proximoVencimento(
        dia: Int,
        referencia: Calendar
    ): Calendar {

        var ano =
            referencia.get(
                Calendar.YEAR
            )

        var mes =
            referencia.get(
                Calendar.MONTH
            )

        while (true) {

            val tentativa =
                Calendar.getInstance().apply {

                    set(
                        ano,
                        mes,
                        1,
                        0,
                        0,
                        0
                    )

                    set(
                        Calendar.MILLISECOND,
                        0
                    )
                }

            val ultimoDia =
                tentativa.getActualMaximum(
                    Calendar.DAY_OF_MONTH
                )

            tentativa.set(
                ano,
                mes,
                minOf(
                    dia,
                    ultimoDia
                )
            )

            if (
                !tentativa.before(
                    referencia.inicioDoDia()
                )
            ) {
                return tentativa
            }

            mes++

            if (
                mes >
                Calendar.DECEMBER
            ) {

                mes =
                    Calendar.JANUARY

                ano++
            }
        }
    }

    private fun diferencaEmDias(
        inicio: Calendar,
        fim: Calendar
    ): Int {

        val inicioDia =
            inicio.inicioDoDia()

        val fimDia =
            fim.inicioDoDia()

        return (
            fimDia.timeInMillis -
                inicioDia.timeInMillis
        ).div(
            24L * 60L * 60L * 1000L
        ).toInt()
    }

    private fun Calendar.inicioDoDia():
        Calendar {

        return (
            clone() as Calendar
        ).apply {

            set(
                Calendar.HOUR_OF_DAY,
                0
            )

            set(
                Calendar.MINUTE,
                0
            )

            set(
                Calendar.SECOND,
                0
            )

            set(
                Calendar.MILLISECOND,
                0
            )
        }
    }
}
