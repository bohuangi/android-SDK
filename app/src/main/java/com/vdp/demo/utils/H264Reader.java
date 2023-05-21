/***********************************************************
 ** Copyright (C), 2008-2016, OPPO Mobile Comm Corp., Ltd.
 ** VENDOR_EDIT
 ** File: - H264Reader.java
 ** Description: raw H264 data file Reader, for testing
 ** Version: 1.0
 ** Date : 2021/09/23
 ** Author: FanWei@NO.NEUTRON.VDM
 **
 ** ----------------------Revision History: --------------------
 **  <author>            <date>         <version >    <desc>
 **  fanwei              2021/09/23     1.0           first add
 ****************************************************************/
package com.vdp.demo.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class H264Reader {
    private static final String TAG = "ProxyCameraVDP.H264Reader";
    private static final String TEST_FILE = "test.h264";
    private static final int START_CODE_LEN = 4;
    private static final int DEFAULT_FPS = 30;

    private final AssetManager mAssetManager;

    private int startIndex = 0;
    private byte[] mVideoData = null;

    public H264Reader(Context context) {
        mAssetManager = context.getAssets();
    }

    public void openFile() {
        try {
            mVideoData = loadFromAssets(TEST_FILE);
            startIndex = getStartCode(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void closeFile() {
        mVideoData = null;
    }

    private int getStartCode(int startPos) {
        for (int i = startPos; i < mVideoData.length - START_CODE_LEN; i++) {
            if (mVideoData[i] == 0x00 && mVideoData[i + 1] == 0x00 && mVideoData[i + 2] == 0x00 && mVideoData[i + 3] == 0x01) {
                return i;
            }
        }
        return -1;
    }

    public int getNextFrame(ByteBuffer frameBuffer) {
        int nextFrame = getStartCode(startIndex + START_CODE_LEN);
        if (nextFrame < 0) {
            Log.d(TAG, "reset to head!");
            startIndex = getStartCode(0);
            nextFrame = getStartCode(startIndex + START_CODE_LEN);
        }

        int len = nextFrame - startIndex;
        frameBuffer.put(mVideoData, startIndex, len);
        startIndex = nextFrame;
        Log.d(TAG, "frame len " + len);

        try {
            Thread.sleep(1000/DEFAULT_FPS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return len;
    }

    private byte[] loadFromAssets(String file) throws IOException {
        int len;
        int size = 1024;

        InputStream stream = mAssetManager.open(file);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[size];
        while ((len = stream.read(buf, 0, size)) != -1) {
            bos.write(buf, 0, len);
        }
        buf = bos.toByteArray();

        bos.close();
        stream.close();

        return buf;
    }
}
