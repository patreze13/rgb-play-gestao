package com.patreze.rgbplaygestao

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

object AgendamentoVencimentos {

    private const val NOME_TRABALHO =
        "verificacao_diaria_vencimentos"

    fun iniciar(context: Context) {

        val agora = Calendar.getInstance()

        val proximaExecucao =
            Calendar.getInstance().apply {

                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)

                if (!after(agora)) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

        val atraso =
            proximaExecucao.timeInMillis -
                agora.timeInMillis

        val trabalho =
            PeriodicWorkRequestBuilder<VencimentoWorker>(
                24,
                TimeUnit.HOURS
            )
                .setInitialDelay(
                    atraso,
                    TimeUnit.MILLISECONDS
                )
                .build()

        WorkManager
            .getInstance(context)
            .enqueueUniquePeriodicWork(
                NOME_TRABALHO,
                ExistingPeriodicWorkPolicy.UPDATE,
                trabalho
            )
    }
}
