package com.vdp.demo;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class Socket_Client {


    private Socket socket;
    private InputStream input;
    private OutputStream output;
    private String host;

    Socket_Client(String s) {
        this.host = s;
    }

    public void connect() {
        try {
            Log.e("client_s.connect", "client_s.connect before");
            socket = new Socket(this.host, 1028);
            Log.e("Client", "socket连接成功");
            System.out.println("Connect to server " + this.host + ":" + 1028 + " success!"); // 连接成功提示
            output = socket.getOutputStream();
            input = socket.getInputStream();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(byte[] out) throws IOException {
        output.write(out);       //发送
        Log.e("Client", "发送成功");
    }

    public int receiveMessage(byte[] buffer) {
        try {

            int length = input.read(buffer); // 将读取的字节存储到缓冲区中，并返回读取的字节数量
            System.out.println("接收成功，receive length= " + length);
            return length;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void disconnect() {
        try {
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}