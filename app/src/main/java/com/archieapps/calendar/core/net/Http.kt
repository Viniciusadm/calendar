package com.archieapps.calendar.core.net

import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient

internal object Http {
    val shared: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }
}
