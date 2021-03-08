package com.KGT_AI.cheonlian

import android.Manifest
import android.Manifest.permission.CAMERA
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.content.Context
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.KGT_AI.cheonlian.ui.main.MainFragment
import com.KGT_AI.cheonlian.ui.main.MainFragment.Companion.REQUEST_IMAGE_CAPTURE

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.container, MainFragment.newInstance())
                    .commitNow()
        }

        if (!checkPersmission(this.applicationContext))
            requestPermission()
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(READ_EXTERNAL_STORAGE, CAMERA),
                REQUEST_IMAGE_CAPTURE)
    }


    // 권한 체크
    private fun checkPersmission(context: Context): Boolean {
        return (ContextCompat.checkSelfPermission(context, CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
    }
}