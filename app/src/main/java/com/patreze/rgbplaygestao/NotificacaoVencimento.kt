package com.patreze.rgbplaygestao

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificacaoVencimento {

    private const val CANAL_ID = "vencimentos"
    private const val CANAL_NOME = "Vencimentos"
    private const val NOTIFICACAO_ID = 1001

    fun criarCanal(context: Context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val canal = NotificationChannel(
                CANAL_ID,
                CANAL_NOME,
                NotificationManager.IMPORTANCE_DEFAULT
            )

            canal.description =
                "Avisos sobre clientes próximos do vencimento"

            val manager =
                context.getSystemService(
                    Context.NOTIFICATION_SERVICE
                ) as NotificationManager

            manager.createNotificationChannel(canal)
        }
    }

    fun mostrar(
        context: Context,
        mensagem: String
    ) {

        criarCanal(context)

        val notification =
            NotificationCompat.Builder(
                context,
                CANAL_ID
            )
                .setSmallIcon(
                    android.R.drawable.ic_dialog_info
                )
                .setContentTitle(
                    "RGB Play Gestão"
                )
                .setContentText(
                    mensagem
                )
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(mensagem)
                )
                .setPriority(
                    NotificationCompat.PRIORITY_DEFAULT
                )
                .setAutoCancel(true)
                .build()

        val manager =
            context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        manager.notify(
            NOTIFICACAO_ID,
            notification
        )
    }
}
