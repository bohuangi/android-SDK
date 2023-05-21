package com.vdp.demo;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;

import android.media.AudioFormat;
import android.os.IBinder;
import android.util.Log;
import com.oplus.vd.base.VirtualDeviceHolderType;
import com.oplus.vd.base.VirtualDeviceSet;
import com.oplus.vd.base.VirtualDeviceType;
import com.oplus.vd.base.ipc.bean.DeviceInfoReply;
import com.oplus.vd.base.ipc.server.VirtualAudioDeviceProviderImpl;
import com.oplus.vdp.proxy.ProxyVirtualAudioDeviceProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class ProxyAudioVDPService extends ProxyVirtualAudioDeviceProvider {


    String TAG = "ProxyAudioVDPService";
    String NOTIFICATION_ID = "ProxyAudioVDPService";
    String NOTIFICATION_NAME = "ProxyAudioVDPService";






    private void startForegroundWithNotification() {
        NotificationManager notificationManager= (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel= new NotificationChannel(NOTIFICATION_ID, NOTIFICATION_NAME, NotificationManager.IMPORTANCE_HIGH);
        notificationManager.createNotificationChannel(channel);
        Notification.Builder builder = new Notification.Builder(this, NOTIFICATION_ID)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle("Proxy Audio VDP Service")
                .setContentText("服务中...666666666");
        startForeground(2,builder.build());
        Log.d(TAG, "startForegroundWithNotification");
    }





    @Override
    public void onCreate() {
        super.onCreate();
        startForegroundWithNotification();

    }




    Map<Long, java.io.OutputStream> audioTracks = new HashMap<Long, OutputStream>();
    Map<Long, InputStream> audioRecords = new HashMap<Long, InputStream>();

    @Override
    public VirtualAudioDeviceProviderImpl getServiceImpl() {
        //获取服务对象
        System.out.println("call get VirtualAudioDeviceProviderImpl");

        return new VirtualAudioDeviceProviderImpl() {

            private IBinder mRemote;



            private VDPPlayer player;

            private VDPRecord Record;

            /////////////////////////////////////////////////////////////////////////////////////////////////////////////
            public DeviceInfoReply getRemoteDevices() {
                // 实现获取虚拟设备列表方法
                Log.d(TAG, "getRemoteDevices");
                VirtualDeviceSet.Builder deviceSet = new VirtualDeviceSet.Builder(getApplicationContext(), "proxy", VirtualDeviceHolderType.CAR);
                // VirtualDeviceSet.Builder(applicationContext, "proxy", VirtualDeviceHolderType.CAR) 当前需固定写法
                // 返回需要被手机使用的虚拟设备，addDevice支持三个参数：名称、类型、序号（默认0）
                deviceSet.addDevice("SPK", VirtualDeviceType.SPEAKER);
                deviceSet.addDevice("MIC", VirtualDeviceType.MIC);

                return new DeviceInfoReply(deviceSet.build());
            }


            /////////////////////////////////////////////////////////////////////////////////////////////////////////////
            @Override
            public int createAudioRecord(long deviceId, AudioFormat audioFormat) {
                // 实现创建音频录制器方法,即.pcm数据当作手机麦克风，发给esp播放
                Log.d(TAG, "createAudioRecord deviceId " + deviceId);
                Log.d(TAG, "createAudioRecord sampleRate " + audioFormat.getSampleRate());
                Log.d(TAG, "createAudioRecord channelMask " + audioFormat.getChannelMask());
                Log.d(TAG, "createAudioRecord encoding " + audioFormat.getEncoding());
                Log.d(TAG, "createAudioRecord ChannelCount " + audioFormat.getChannelCount());



                // 读取测试数据
                InputStream audioRecord = null;
                try {
                    audioRecord = getAssets().open("test.pcm");
                    //发送？？？？
                    audioRecords.put(deviceId, audioRecord);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                String ipv4="192.168.252.46";

                player=new VDPPlayer();
                player.open(ipv4,audioFormat);
                player.configure();

                return 0;
            }

            /////////////////////////////////////////////////////////////////////////////////////////////////////////////
            public int closeAudioRecord(long deviceId) {
                // 实现关闭音频录制器方法
                Log.e("closeAudioRecord", "creat closeAudioRecord");

                player.close();



                InputStream audioRecord = audioRecords.get(deviceId);
                if (audioRecord != null) {
                    try {
                        audioRecord.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                audioRecords.remove(deviceId);
                return 0;


            }

            /////////////////////////////////////////////////////////////////////////////////////////////////////////////
            public int createAudioPlayer(long deviceId, AudioFormat audioFormat) {
                // 实现创建音频播放器方法，即接收esp麦克风发来的数据，手机进行播放（保存到文件中）
                Log.d(TAG, "createAudioPlayer deviceId " + deviceId);
                Log.d(TAG, "createAudioPlayer sampleRate " + audioFormat.getSampleRate());
                Log.d(TAG, "createAudioPlayer channelMask " + audioFormat.getChannelMask());
                Log.d(TAG, "createAudioPlayer encoding " + audioFormat.getEncoding());
                Log.d(TAG, "createAudioPlayer ChannelCount " + audioFormat.getChannelCount());
                // 创建存储音频数据的文件
                File file = getApplicationContext().getExternalFilesDir("record");
                System.out.println("file.toString()=========================="+file.toString());
                //这行代码创建一个名为"record"的文件夹，用来保存音频数据。
                try {
                    OutputStream audioTrack = new FileOutputStream(file.toString() + deviceId + ".pcm");
                    //这里创建一个存储音频数据的输出流，将其与设备ID一起存储在一个哈希表中，以便将来可以使用该ID识别特定的音频播放器。如果创建输出流时发生了异常，则记录错误消息到日志中。
                    audioTracks.put(deviceId, audioTrack);
                } catch (IOException e) {
                    Log.e(TAG, "createAudioPlayer " + e.getMessage());
                }

                String ipv4="192.168.252.14";

                Record=new VDPRecord();
                Record.open(ipv4,audioFormat);
                Record.configure();

                return 0;
            }


            /////////////////////////////////////////////////////////////////////////////////////////////////////////////
            @Override
            public int closeAudioPlayer(long deviceId) {
                // 实现关闭音频播放器方法
                Log.d(TAG, "closeAudioPlayer deviceId " + deviceId);



                Record.close();


                OutputStream audioTrack = audioTracks.get(deviceId);
                if (audioTrack != null) {
                    try {
                        audioTrack.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                audioTracks.remove(deviceId);

                return 0;
            }


            /////////////////////////////////////////////////////////////////////////////////////////////////////////////
            public int write(long deviceId, byte[] data) {
                Log.d("write", "creat write");
                // 实现写入音频数据方法
                //App.getInstance().receivedData.postValue(data);//将写入的音频数据通过LiveData实例发送，并更新UI界面。

//                if (!App.getInstance().needPlay) {
//                    //检查当前应用程序是否需要播放音频数据。如果不需要播放，则立即返回写入的数据的字节数。
//                    return (data == null) ? 0 : data.length;
//                }


                OutputStream audioTrack = audioTracks.get(deviceId);//这里与播放前建立连接

                int decoded=0;
                if (audioTrack != null ) {
                    decoded=Record.record(audioTrack);
                    return decoded;
                }

            return 0;

            }

            /////////////////////////////////////////////////////////////////////////////////////////////////////////////
            public int read(long deviceId, byte[] data) {

// 实现读取音频数据方法
                InputStream audioRecord = audioRecords.get(deviceId);
                //这里检查给定设备ID的音频记录是否可用，如果未找到，将返回0来指示没有数据读取。
                if (audioRecord == null) {
                    return 0;
                }

                int datalen = 0;
                try {
                    // 判断音频数据是否已读取完，如果是，则从头开始读取
                    if (audioRecord.available() <= 0) {
                        datalen = audioRecord.read(data, 0, data.length);//尝试从音频记录中读取音频数据，并将读取到的字节数赋值给len
                        audioRecord.reset();
                    }
                    player.play(audioRecord);
                } catch (IOException e) {
                    datalen = 0;
                    e.printStackTrace();
                }



                //App.getInstance().sendData.postValue(data);
                return datalen;//返回已读取的字节数。

                /////////////////////////////////////////////////////////////////////////////////////////////////////////////

            }

        };

    }

}
