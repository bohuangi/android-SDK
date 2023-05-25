package com.vdp.demo;

import android.media.AudioFormat;
import android.util.Log;

import com.score.rahasak.utils.OpusDecoder;

import java.io.IOException;
import java.io.OutputStream;

public class VDPRecord {
    private Socket_Client client_s;

    private int sampleRate;//采样率
    private int channelCount;//声道数
    private int samplebytes;//采样位数
    private int FRAME_SIZE;//每帧每声道采样数
    private int frame_bytes;//每帧字节数


    private OpusDecoder opusDecoder;//编码器


    public VDPRecord(){}

    public void open(String ipv4,AudioFormat audioFormat){


        sampleRate = 16000;               //audioFormat.getSampleRate(); // 采样率
        channelCount = 2;                 //audioFormat.getChannelCount();   // 声道数
        samplebytes = 2;       //采样位数
        FRAME_SIZE = (int) (sampleRate / 1000 * 20);   //每帧每声道采样数
        frame_bytes = (int) (FRAME_SIZE * channelCount * samplebytes); //每帧字节数

        client_s = new Socket_Client(ipv4);
        client_s.connect();

    }
    public void configure(){

        // init opus decoder
        opusDecoder = new OpusDecoder();
        opusDecoder.init(sampleRate, channelCount);
        Log.e("opusDecoder.init", "opusDecoder.init success");


    }


    public void close(){
        opusDecoder.close();
        client_s.disconnect();
    }

    public int record(OutputStream outputStream){


        long time_start1, time_end1;
        long time_start, time_end;

        String string="ok";
        byte[] tx_ok=string.getBytes();
        time_start1 = System.currentTimeMillis();
        try {
            client_s.sendMessage(tx_ok);
        } catch (IOException e) {
            e.printStackTrace();
        }
        time_end1 = System.currentTimeMillis();
        System.out.println("    sendMessage time =" + (time_end1 - time_start1) + " ms");

        time_start1 = System.currentTimeMillis();
        byte[] rx_buffer = new byte[200];
        int rx_len = client_s.receiveMessage(rx_buffer);
        if (rx_len < 0) {
            Log.e("client_s.receiveMessage", "没有接收到esp发来的确认收到消息");
        }
        time_end1 = System.currentTimeMillis();
        System.out.println("    receiveMessage time =" + (time_end1 - time_start1) + " ms");

        byte[] rx_buffer2=new byte[rx_len];
        System.arraycopy(rx_buffer, 0, rx_buffer2, 0, rx_len);


//        byte[] encBuf2 = new byte[500];
//        int[] data_len = new int[1];
//
//        Log.e("Socket_Thread","数据从缓冲区读出");
//        System.out.println("读出的数据大小为"+data_len[0]);
//        byte[] encBuf = new byte[data_len[0]];
//        System.arraycopy(encBuf2, 0, encBuf, 0, data_len[0]);


        byte[] outBuf = new byte[2048];
        int decoded = opusDecoder.decode(rx_buffer2, outBuf, FRAME_SIZE);
        Log.e("opus_decoded", "decoded " + decoded + " bytes");

        byte[] outBuf2=new byte[decoded* channelCount * samplebytes];
        System.arraycopy(outBuf, 0, outBuf2, 0, outBuf2.length);
        Log.e("arraycopy", "arraycopy after");


        try {
            outputStream.write(outBuf2);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    return  decoded;

    }




}
