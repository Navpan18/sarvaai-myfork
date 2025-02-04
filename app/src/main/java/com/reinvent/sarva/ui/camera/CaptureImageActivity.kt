package com.reinvent.sarva.ui.camera

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.reinvent.sarva.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CaptureImageActivity : AppCompatActivity()
{

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState : Bundle?)
    {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        setContentView(R.layout.activity_capture_image)
        if (savedInstanceState == null)
        {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container , CaptureImageFragment.newInstance())
                .commitNow()
        }

    }
}