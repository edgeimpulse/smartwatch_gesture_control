package com.example.wizbulb.presentation

import android.content.Context

object BulbSettings {
    private const val PREFS    = "bulb_settings"
    private const val KEY_IP   = "ip"
    private const val KEY_PORT = "port"

    const val DEFAULT_IP   = "192.168.1.103"
    const val DEFAULT_PORT = 38899

    fun load(context: Context): Pair<String, Int> {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return Pair(
            p.getString(KEY_IP, DEFAULT_IP) ?: DEFAULT_IP,
            p.getInt(KEY_PORT, DEFAULT_PORT)
        )
    }

    fun save(context: Context, ip: String, port: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_IP, ip)
            .putInt(KEY_PORT, port)
            .apply()
    }
}
