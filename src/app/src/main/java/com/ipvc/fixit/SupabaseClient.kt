package com.ipvc.fixit

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.cio.CIO
import android.util.Log

object SupabaseClientInstance {

    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_KEY
        ) {
            Log.d("SupabaseDebug", "URL: ${BuildConfig.SUPABASE_URL}")
            Log.d("SupabaseDebug", "KEY: ${BuildConfig.SUPABASE_KEY.take(10)}...")
            install(Auth)
            install(Postgrest)
            httpEngine = CIO.create()
        }
    }
}
