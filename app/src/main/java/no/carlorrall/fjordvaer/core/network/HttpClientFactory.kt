package no.carlorrall.fjordvaer.core.network

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object HttpClientFactory {
    private const val USER_AGENT = "Fjordvaer/1.0 (https://github.com/carlorrall/fjordvaer)"

    fun create(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                chain.proceed(chain.request().newBuilder().header("User-Agent", USER_AGENT).build())
            }
            .build()
}
