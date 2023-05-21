/***********************************************************
 ** Copyright (C), 2008-2016, OPPO Mobile Comm Corp., Ltd.
 ** VENDOR_EDIT
 ** File: - ProxyCameraVDPService.java
 ** Description: Proxy CameraVDP Service demo
 ** Version: 1.0
 ** Date : 2021/09/23
 ** Author: FanWei@NO.NEUTRON.VDM
 **
 ** ----------------------Revision History: --------------------
 **  <author>            <date>         <version >    <desc>
 **  fanwei              2021/09/23     1.0           first add
 ****************************************************************/
package com.vdp.demo

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.os.DeadObjectException
import android.os.SystemClock
import android.util.Log
import android.view.Surface
import com.oplus.vd.base.VirtualCameraState
import com.oplus.vd.base.VirtualDeviceHolderType
import com.oplus.vd.base.VirtualDeviceSet
import com.oplus.vd.base.VirtualDeviceType
import com.oplus.vd.base.ipc.IProxyCameraStateCallback
import com.oplus.vd.base.ipc.bean.DeviceInfoReply
import com.oplus.vd.base.ipc.bean.VirtualDeviceRange
import com.oplus.vd.base.ipc.server.VirtualCameraDeviceProviderImpl
import com.oplus.vd.utils.CameraUtil
import com.oplus.vdp.proxy.ProxyVirtualCameraDeviceProvider
import com.vdp.demo.utils.H264Reader
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

class ProxyCameraVDPService : ProxyVirtualCameraDeviceProvider() {
    companion object {
        const val TAG = "ProxyCameraVDPService"
        const val NOTIFICATION_ID = "ProxyCameraVDPService"
        const val NOTIFICATION_NAME = "ProxyCameraVDPService"
    }

    private fun startForegroundWithNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(NOTIFICATION_ID, NOTIFICATION_NAME, NotificationManager.IMPORTANCE_HIGH)
        notificationManager.createNotificationChannel(channel)
        val builder = Notification.Builder(this, NOTIFICATION_ID)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle("Proxy Camera VDP Service")
                .setContentText("服务中...")
        startForeground(2, builder.build())
        Log.d(TAG, "startForegroundWithNotification")
    }

    private lateinit var mCameraManager: CameraManager
    private val mH264Readers = HashMap<Long, H264Reader>()
    private var mProxyCameraStateCallback = AtomicReference<IProxyCameraStateCallback>()

    override fun onCreate() {
        super.onCreate()
        startForegroundWithNotification()
        mCameraManager = this.getSystemService(CAMERA_SERVICE) as CameraManager
    }

    override fun getServiceImpl() : VirtualCameraDeviceProviderImpl {
        val mActiveDeviceId = AtomicLong(0L)

        return object : VirtualCameraDeviceProviderImpl() {
            override fun getCameraInfo(): DeviceInfoReply {
                Log.d(TAG, "getDevices")
                return DeviceInfoReply(getVirtualDeviceSet())
            }

            @Synchronized
            override fun open(
                deviceId: Long,
                width: Int,
                height: Int,
                fpsRange: VirtualDeviceRange,
                surface: Surface,
                callback: IProxyCameraStateCallback
            ): Int {
                Log.d(TAG, "open $deviceId, width=$width, height=$height, fpsRange=(${fpsRange.lower}, ${fpsRange.upper})")
                mProxyCameraStateCallback.set(callback)
                val reader = H264Reader(applicationContext).apply {
                    openFile()
                }

                mActiveDeviceId.set(deviceId)

                mH264Readers[deviceId] = reader

                try {
                    mProxyCameraStateCallback.get()?.onCameraStateChanged(deviceId, VirtualCameraState.VIRTUAL_CAMERA_STATE_OPENED)
                } catch (e: DeadObjectException) {
                    Log.w(TAG, "open: " + e.message)
                }

                return super.open(deviceId, width, height, fpsRange, surface, callback)
            }

            @Synchronized
            override fun close(deviceId: Long): Int {
                Log.d(TAG, "close")
                mActiveDeviceId.set(0L)

                mH264Readers[deviceId]?.closeFile()
                mH264Readers.remove(deviceId)

                try {
                    mProxyCameraStateCallback.get()?.onCameraStateChanged(deviceId, VirtualCameraState.VIRTUAL_CAMERA_STATE_CLOSED)
                } catch (e: DeadObjectException) {
                    Log.w(TAG, "close: " + e.message)
                }

                return super.close(deviceId)
            }

            override fun fillInputBuffer(buffer: ByteBuffer?, timeoutMs: Long): Long {
                mH264Readers[mActiveDeviceId.get()]?.getNextFrame(buffer)
                return SystemClock.elapsedRealtime()
            }

            override fun onError() {
                Log.w(TAG, "onError")

                try {
                    val deviceId = mActiveDeviceId.get()
                    if (deviceId != 0L) {
                        mProxyCameraStateCallback.get()
                            ?.onCameraStateChanged(deviceId, VirtualCameraState.VIRTUAL_CAMERA_STATE_ERROR)
                    }
                } catch (e: DeadObjectException) {
                    Log.w(TAG, "onError: " + e.message)
                }
            }
        }
    }

    fun getVirtualDeviceSet(): VirtualDeviceSet? {
        val vdsb: VirtualDeviceSet.Builder = VirtualDeviceSet.Builder(applicationContext, "proxy", VirtualDeviceHolderType.CAR)
        try {
            var cameraIds: Array<String> = mCameraManager.cameraIdList
            // filter
            cameraIds = cameraIds.copyOfRange(0, 1)
            for (id in cameraIds) {
                val characteristics: CameraCharacteristics = mCameraManager.getCameraCharacteristics(id)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (facing != null) {
                    val extras = Bundle().apply {
                        putString("characteristics", CameraUtil.formatCameraCharacteristics(mCameraManager, id))
                    }

                    if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                        vdsb.addDevice("FAKE-BACK", VirtualDeviceType.CAMERA, id.toInt(), extras)
                    } else if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                        vdsb.addDevice("FAKE-FRONT", VirtualDeviceType.CAMERA, id.toInt(), extras)
                    }
                }
            }
        } catch (e: CameraAccessException) {
            Log.e(TAG, e.stackTraceToString())
        }

        try {
            Log.i(TAG, "built dev: $vdsb, device num: ${vdsb.build().devices.size}")
            return vdsb.build()
        } catch (e: RuntimeException) {
            Log.e(TAG, e.stackTraceToString())
        }

        return null
    }
}
