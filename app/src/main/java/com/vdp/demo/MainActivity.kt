/***********************************************************
 ** Copyright (C), 2020-2021, OPPO Mobile Comm Corp., Ltd.
 ** VENDOR_EDIT
 ** File: - java
 ** Description: proxy vdp demo
 ** Version: 1.0
 ** Date : 2020/06/04
 ** Author: Guangming.Chen@NO.NEUTRON.VDM
 **
 ** ----------------------Revision History: --------------------
 **  <author>            <date>         <version >    <desc>
 **  chenguangming       2021/06/04     1.0           first add
 ****************************************************************/

package com.vdp.demo

import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Observer
import com.oplus.vd.config.VDConfig
import java.lang.Integer.min

class MainActivity : AppCompatActivity() {
    private var receiveWaveView: ImageView? = null
    private var receiveWaveBitmap: Bitmap? = null
    private var receiveWaveCanvas: Canvas? = null
    private var sendWaveView: ImageView? = null
    private var sendWaveBitmap: Bitmap? = null
    private var sendWaveCanvas: Canvas? = null

    private var paint: Paint? = null
    private var serviceStarted = false

    companion object {
        const val TAG = "ProxyVDPDemo"
        private const val CODE_RQST_NOTIFICATION = 100
    }

    private fun runCmd(cmd: String) {
        val intent = Intent()
        intent.component = ComponentName("com.oplus.vdc", "com.oplus.vdc.FakeController")
        intent.putExtra("vdc_cmd", cmd)
        startService(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        VDConfig.init(applicationContext)
        setContentView(R.layout.activity_main)


        findViewById<Switch>(R.id.spkSwitch).setOnCheckedChangeListener { _, isChecked ->
            runCmd(
                if (isChecked) {
                    "spk-on"
                } else {
                    "spk-off"
                }
            )
        }

        findViewById<Switch>(R.id.micSwitch).setOnCheckedChangeListener { _, isChecked ->
            runCmd(
                if (isChecked) {
                    "mic-on"
                } else {
                    "mic-off"
                }
            )
        }

        findViewById<Switch>(R.id.cameraSwitch).setOnCheckedChangeListener { _, isChecked ->
            runCmd(
                if (isChecked) {
                    "camera-on"
                } else {
                    "camera-off"
                }
            )
        }

        initWaveDrawer()

        App.getInstance()?.receivedData?.observe(this, Observer {
            drawWave(it, receiveWaveCanvas!!, receiveWaveView!!)
        })
        App.getInstance()?.sendData?.observe(this, Observer {
            drawWave(it, sendWaveCanvas!!, sendWaveView!!)
        })

        checkNotificationPermission()
    }

    private fun isGranted(permission: String): Boolean {
        // Verify that all required contact permissions have been granted.
        return ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun goNotificationSettingPage() {
        try {
            val intent = Intent()
            Log.d(TAG, "goNotificationSettingPage")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                intent.action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                intent.putExtra(Settings.EXTRA_CHANNEL_ID, applicationInfo.uid)
                intent.putExtra("app_package", packageName)
                intent.putExtra("app_uid", applicationInfo.uid)
            } else {
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                intent.data = Uri.fromParts("package", packageName, null)
            }
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()

        if (!serviceStarted && isGranted(POST_NOTIFICATIONS)) {
            startProxyVdpService()
        }
    }

    private fun checkNotificationPermission() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                if (!isGranted(POST_NOTIFICATIONS)) {
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this, POST_NOTIFICATIONS)) {
                        goNotificationSettingPage()
                    } else {
                        ActivityCompat.requestPermissions(this,  arrayOf(POST_NOTIFICATIONS), CODE_RQST_NOTIFICATION)
                    }
                } else {
                    Log.d(TAG, "startWebService in checkNotificationPermission T")
                    startProxyVdpService()
                }
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                if (NotificationManagerCompat.from(this).areNotificationsEnabled()) {
                    Log.d(TAG, "startWebService in checkNotificationPermission O")
                    startProxyVdpService()
                } else {
                    goNotificationSettingPage()
                }
            }

            else -> {
                Log.d(TAG, "startWebService in onRequestPermissionsResult < O")
                startProxyVdpService()
            }
        }
    }

    private fun startProxyVdpService() {
        startForegroundService(Intent(this, ProxyAudioVDPService::class.java))
        startForegroundService(Intent(this, ProxyCameraVDPService::class.java))

        Toast.makeText(this, "服务已启动", Toast.LENGTH_SHORT).show()
        serviceStarted = true
    }

    private fun initWaveDrawer() {
        receiveWaveView = findViewById(R.id.receiveWave)
        sendWaveView = findViewById(R.id.sendWave)
        receiveWaveBitmap = Bitmap.createBitmap(1024, 200, Bitmap.Config.ARGB_8888)
        receiveWaveCanvas = Canvas(receiveWaveBitmap!!)
        sendWaveBitmap = Bitmap.createBitmap(1024, 200, Bitmap.Config.ARGB_8888)
        sendWaveCanvas = Canvas(sendWaveBitmap!!)
        paint = Paint()
        paint?.color = Color.CYAN
        receiveWaveView?.setImageBitmap(receiveWaveBitmap)
        sendWaveView?.setImageBitmap(sendWaveBitmap)
    }

    private fun drawWave(data: ByteArray, canvas: Canvas, view: ImageView) {
        var downy: Float
        val waveHeight = 200
        val waveWidth = min(windowManager.defaultDisplay.width, data.size)

        canvas.drawColor(Color.DKGRAY)
        for (i in 0..waveWidth) {
            downy = waveHeight * (0.5f + data[i].toFloat() / 127)
            canvas.drawPoint(i.toFloat(), downy, paint!!)
        }
        view.invalidate()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            CODE_RQST_NOTIFICATION -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "startWebService in onRequestPermissionsResult")
                    startProxyVdpService()
                } else {
                    Toast.makeText(this, R.string.notification_permission_toast, Toast.LENGTH_LONG).show()
                    goNotificationSettingPage()
                }
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}
