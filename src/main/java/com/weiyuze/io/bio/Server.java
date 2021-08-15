package com.weiyuze.io.bio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    public static void main(String[] args) throws IOException {

        //在服务器端建立Server的socket(插座)，等着客户端插入建立连接,每一个客户端都准备一个插座
        ServerSocket ss = new ServerSocket();//ServerSocket 总面板 有很多插座
        ss.bind(new InetSocketAddress("127.0.0.1", 8888));//监听在哪一个端口

        //处理用户链接
        while (true) {
            Socket s = ss.accept();//阻塞方法 服务器到这会停，下面的代码执行不了
            // 直到有一个客户端连入，建立一个通道，s代表通道

            //Java8 lambda表达式
            //如果不new一个新线程，服务器处理连接，如果是长链接，无法继续循环、accept，新的客户端无法连接
            new Thread(() -> {
                handle(s);
            }).start();
        }
    }

    private static void handle(Socket s) {
        try {
            //单向通道
            byte[] bytes = new byte[1024];
            int len = s.getInputStream().read(bytes);//输入通道读数据 阻塞方法
            System.out.println(new String(bytes, 0, len));

            s.getOutputStream().write(bytes, 0, len);//OutputStream写数据 阻塞方法
            s.getOutputStream().flush();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
