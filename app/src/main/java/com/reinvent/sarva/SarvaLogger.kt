package com.reinvent.sarva

import android.util.Log

object SarvaLogger
{
    fun logWithTag(tag: String = TAG, message: String)
    {
        if(isLoggingEnabled)
        {
            Log.d(tag, message)
        }
    }

    private var isLoggingEnabled = true
    private const val TAG = "Sunnny:"
}