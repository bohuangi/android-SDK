/***********************************************************
 ** Copyright (C), 2020-2021, OPPO Mobile Comm Corp., Ltd.
 ** VENDOR_EDIT
 ** File: - ProxyAudioVDPService.java
 ** Description: proxy vdp service for demo
 ** Version: 1.0
 ** Date : 2020/06/04
 ** Author: Guangming.Chen@NO.NEUTRON.VDM
 **
 ** ----------------------Revision History: --------------------
 **  <author>            <date>         <version >    <desc>
 **  chenguangming       2021/06/04     1.0           first add
 ****************************************************************/
package com.vdp.demo

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioFormat
import android.util.Log
import com.oplus.vd.base.VirtualDeviceHolderType
import com.oplus.vd.base.VirtualDeviceSet
import com.oplus.vd.base.VirtualDeviceType
import com.oplus.vd.base.ipc.bean.DeviceInfoReply
import com.oplus.vd.base.ipc.server.VirtualAudioDeviceProviderImpl
import com.oplus.vdp.proxy.ProxyVirtualAudioDeviceProvider
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class ProxyAudioVDPServiceLocal : ProxyVirtualAudioDeviceProvider() {
    companion object {
        const val TAG = "ProxyAudioVDPService"
        const val NOTIFICATION_ID = "ProxyAudioVDPService"
        const val NOTIFICATION_NAME = "ProxyAudioVDPService"
    }

    private fun startForegroundWithNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(NOTIFICATION_ID, NOTIFICATION_NAME, NotificationManager.IMPORTANCE_HIGH)
        notificationManager.createNotificationChannel(channel)
        val builder = Notification.Builder(this, NOTIFICATION_ID)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle("Proxy Audio VDP Service")
                .setContentText("服务中...")
        startForeground(1, builder.build())
        Log.d(TAG, "startForegroundWithNotification")
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundWithNotification()
    }

    var audioTracks = HashMap<Long, OutputStream>()
    var audioRecords = HashMap<Long, InputStream>()

    override fun getServiceImpl(): VirtualAudioDeviceProviderImpl {
        return object : VirtualAudioDeviceProviderImpl() {
            override fun createAudioRecord(deviceId: Long, audioFormat: AudioFormat?): Int {
                Log.d(TAG, "createAudioRecord deviceId $deviceId ")
                Log.d(TAG, "createAudioRecord sampleRate ${audioFormat!!.sampleRate} ")
                Log.d(TAG, "createAudioRecord channelMask ${audioFormat!!.channelMask} ")
                Log.d(TAG, "createAudioRecord encoding ${audioFormat!!.encoding} ")

                val audioRecord = assets.open("test.pcm")
                audioRecords[deviceId] = audioRecord

                return 0
            }

            override fun closeAudioRecord(deviceId: Long): Int {
                val audioRecord = audioRecords[deviceId]
                audioRecord?.close()
                audioRecords.remove(deviceId)
                return 0
            }

            override fun getRemoteDevices(): DeviceInfoReply {
                Log.d(TAG, "getRemoteDevices")
                // VirtualDeviceSet.Builder(applicationContext, "proxy", VirtualDeviceHolderType.CAR) 当前需固定写法
                // 返回需要被手机使用的虚拟设备，addDevice支持三个参数：名称、类型、序号（默认0）

                return DeviceInfoReply(
                        VirtualDeviceSet.Builder(applicationContext, "proxy", VirtualDeviceHolderType.CAR)
                                .addDevice("SPK", VirtualDeviceType.SPEAKER)
                                .addDevice("MIC", VirtualDeviceType.MIC)
                                .build())
            }

            override fun write(deviceId: Long, data: ByteArray?): Int {

                App.getInstance()?.receivedData?.postValue(data)

                if (!App.getInstance()?.needPlay!!) {
                    return data?.size!!
                }

                audioTracks[deviceId]?.write(data!!)
                return data?.size!!
            }

            private fun fakeWaitData(bytes: Int?) {
                bytes?:return

                val ms: Int = 1000 * bytes / (48000 * 4)
                try {
                    Thread.sleep(ms.toLong())
                } catch (ignored: InterruptedException) {
                }
            }

            override fun read(deviceId: Long, data: ByteArray?): Int {
                if (audioRecords[deviceId]?.available()!! <= 0) {
                    audioRecords[deviceId]?.reset()
                }

                val len = audioRecords[deviceId]?.read(data!!, 0, data.size)

                fakeWaitData(len)

                App.getInstance()?.sendData?.postValue(data)

                return len!!
            }

            override fun closeAudioPlayer(deviceId: Long): Int {
                Log.d(TAG, "closeAudioPlayer deviceId $deviceId ")

                val audioTrack = audioTracks[deviceId]
                audioTrack?.close()
                audioTracks.remove(deviceId)

                return 0
            }

            override fun createAudioPlayer(deviceId: Long, audioFormat: AudioFormat?): Int {
                Log.d(TAG, "createAudioPlayer sampleRate ${audioFormat!!.sampleRate} ")
                Log.d(TAG, "createAudioPlayer channelMask ${audioFormat!!.channelMask} ")
                Log.d(TAG, "createAudioPlayer encoding ${audioFormat!!.encoding} ")

                val file = applicationContext.getExternalFilesDir("record")
                try {
                    val audioTrack = FileOutputStream(file.toString() + deviceId.toString() + ".pcm")
                    audioTracks[deviceId] = audioTrack
                } catch (e: IOException) {
                    Log.e(TAG, "createAudioPlayer " + e.message)
                }

                return 0
            }

        }
    }
}
