package com.vdp.demo;

import android.media.AudioFormat;
import android.util.Log;

import com.score.rahasak.utils.OpusDecoder;
import com.score.rahasak.utils.OpusEncoder;

import java.io.IOException;
import java.io.InputStream;

public class VDPPlayer {
    private Socket_Client client_s;

    private int sampleRate;//采样率
    private int channelCount;//声道数
    private int samplebytes;//采样位数
    private int FRAME_SIZE;//每帧每声道采样数
    private int frame_bytes;//每帧字节数
    private int complexity;

    private OpusEncoder opusEncoder;//编码器




    public VDPPlayer(){

    }
//////////////////////////////////////////////////////////////////////////////////////////////////////////
    public void open(String ipv4,AudioFormat audioFormat){

        sampleRate = audioFormat.getSampleRate(); // 采样率
        channelCount = audioFormat.getChannelCount();   // 声道数
        samplebytes = 2;       //采样位数
        FRAME_SIZE = (int) (sampleRate / 1000.0 * 20.0);   //每帧每声道采样数
        frame_bytes = (int) (FRAME_SIZE * channelCount * samplebytes); //每帧字节数
        //int bitrate=1536000;
        complexity = 9;




        client_s = new Socket_Client(ipv4);
        client_s.connect();
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////
    public void configure(AudioFormat audioFormat){//重新配置参数并初始化编码


        sampleRate = audioFormat.getSampleRate(); // 采样率
        channelCount = audioFormat.getChannelCount();   // 声道数
        samplebytes = 2;       //采样位数
        FRAME_SIZE = (int) (sampleRate / 1000.0 * 20.0);   //每帧每声道采样数
        frame_bytes = (int) (FRAME_SIZE * channelCount * samplebytes); //每帧字节数
        //int bitrate=1536000;
        complexity = 9;




        opusEncoder = new OpusEncoder();
        opusEncoder.init(sampleRate, channelCount, OpusEncoder.OPUS_APPLICATION_AUDIO);
        Log.e("opusEncoder.init", "opusEncoder.init success");
        //opusEncoder.setBitrate(bitrate);
        opusEncoder.setComplexity(complexity);


    }
    public void configure(){//根据open的参数初始化编码器

        opusEncoder = new OpusEncoder();
        opusEncoder.init(sampleRate, channelCount, OpusEncoder.OPUS_APPLICATION_AUDIO);
        Log.e("opusEncoder.init", "opusEncoder.init success");
        //opusEncoder.setBitrate(bitrate);
        opusEncoder.setComplexity(complexity);

    }
    //////////////////////////////////////////////////////////////////////////////////////////////////////////
    public void close(){

        opusEncoder.close();
        client_s.disconnect();

    }
    //////////////////////////////////////////////////////////////////////////////////////////////////////////
    public void play(InputStream inputStream) {//编码inputStream里的数据并发送到esp播放

        long time_start1, time_end1;
        long time_start, time_end;

        time_start = System.currentTimeMillis();


        int bufferSize = frame_bytes;//要编码的每帧的字节数
        byte[] buffer = new byte[bufferSize];
        int bufferlen = -1;




        try {
            bufferlen = inputStream.read(buffer, 0, bufferSize);//管道的数据写入buffer
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        time_start1 = System.currentTimeMillis();
        byte[] out = new byte[500];//编码到这里
        int opus_len = opusEncoder.encode(buffer, FRAME_SIZE, out);//编码
        time_end1 = System.currentTimeMillis();
        Log.e("Socket_Thread", "opusEncoder time ");
        System.out.println("    opusEncoder time =" + (time_end1 - time_start1) + " ms");
        byte[] out1 = new byte[opus_len];
        System.arraycopy(out, 0, out1, 0, out1.length);


        time_start1 = System.currentTimeMillis();
        try {
            client_s.sendMessage(out1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        time_end1 = System.currentTimeMillis();
        Log.e("Socket_Thread", "sendMessage time ");
        System.out.println("    sendMessage time =" + (time_end1 - time_start1) + " ms");

        time_start1 = System.currentTimeMillis();
        byte[] buffer2 = new byte[20];
        int len1 = client_s.receiveMessage(buffer2);

        if (len1 < 0) {
            Log.e("client_s.receiveMessage", "没有接收到esp发来的确认收到消息");
        }
        time_end1 = System.currentTimeMillis();
        Log.e("Socket_Thread", "receiveMessage time");
        System.out.println("    receiveMessage time =" + (time_end1 - time_start1) + " ms");

        time_end = System.currentTimeMillis();
        Log.e("Socket_Thread", "one time");
        System.out.println("                                Socket_Thread one time =" + (time_end - time_start) + " ms");


    }
    //////////////////////////////////////////////////////////////////////////////////////////////////////////
}
