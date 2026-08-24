package com.patreze.rgbplaygestao

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object AgendamentoVencimentos {

    private const val REQUEST_CODE = 9001

    fun iniciar(context: Context) {

        val alarmManager =
            context.getSystemService(
                Context.ALARM_SERVICE
            ) as AlarmManager

        val intent =
            Intent(
                context,
                AlarmeVencimentosReceiver::class.java
            )

        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
            )

        val agora =
            Calendar.getInstance()

        val proximaExecucao =
            Calendar.getInstance().apply {

                set(
                    Calendar.HOUR_OF_DAY,
                    9
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

                if (!after(agora)) {
                    add(
                        Calendar.DAY_OF_YEAR,
                        1
                    )
                }
            }

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            proximaExecucao.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }
}
