/***********************************************************
 ** Copyright (C), 2020-2021, OPPO Mobile Comm Corp., Ltd.
 ** VENDOR_EDIT
 ** File: App.kt
 ** Description: local vdp demo
 ** Version: 1.0
 ** Date : 2020/06/04
 ** Author: Guangming.Chen@NO.NEUTRON.VDM
 **
 ** ----------------------Revision History: --------------------
 **  <author>            <date>         <version >    <desc>
 **  chenguangming       2021/06/04     1.0           first add
 ****************************************************************/

package com.vdp.demo

import android.app.Application
import android.content.Intent
import androidx.lifecycle.MutableLiveData;
import com.oplus.vd.config.VDConfig

class App : Application() {
    companion object {
        var app : App? = null

        fun getInstance() : App? {
            return app
        }
    }

    val receivedData = MutableLiveData<ByteArray>()
    val sendData = MutableLiveData<ByteArray>()
    var needPlay = false

    override fun onCreate() {
        super.onCreate()

        app = this
        VDConfig.init(this)
    }
}