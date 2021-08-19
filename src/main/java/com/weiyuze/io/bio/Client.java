package com.weiyuze.io.bio;

import java.io.IOException;
import java.net.Socket;

public class Client {
    public static void main(String[] args) throws IOException {
        //半双工 读时不能同时写
        Socket s = new Socket("127.0.0.1", 8888);//连接服务器端口，s就是通道
        s.getOutputStream().write("HelloServer".getBytes());
        s.getOutputStream().flush();
        System.out.println("write over, waiting for msg back...");
        byte[] bytes = new byte[1024];//每次最多读满字节数组
        int len = s.getInputStream().read(bytes);//本地处理，第一个包存好，第二个包存进来，攒成一个内容
        System.out.println(new String(bytes, 0, len));
        s.close();

    }
}
