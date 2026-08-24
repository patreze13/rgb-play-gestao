package com.patreze.rgbplaygestao

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class AlarmeVencimentosReceiver :
    BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent?
    ) {

        val trabalho =
            OneTimeWorkRequestBuilder<
                VencimentoWorker
            >()
                .build()

        WorkManager
            .getInstance(context)
            .enqueue(trabalho)
    }
}
