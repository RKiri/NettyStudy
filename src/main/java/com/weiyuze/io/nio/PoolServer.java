package com.weiyuze.io.nio;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PoolServer {
    ExecutorService pool = Executors.newFixedThreadPool(50);
    Selector selector;

    public static void main(String[] args) throws IOException {
        PoolServer server = new PoolServer();
        server.initServer(8000);
        server.listen();
    }

    private void initServer(int port) throws IOException {
        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);
        ssc.socket().bind(new InetSocketAddress(port));
        selector = Selector.open();
        ssc.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("服务器启动成功...");
    }

    private void listen() throws IOException {
        while (true) {
            selector.select();
            Iterator<SelectionKey> it = selector.selectedKeys().iterator();
            while (it.hasNext()) {
                SelectionKey key = it.next();
                it.remove();
                if (key.isAcceptable()) {
                    ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
                    SocketChannel sc = ssc.accept();
                    sc.configureBlocking(false);
                    sc.register(selector, SelectionKey.OP_READ);
                } else if (key.isReadable()) {
                    key.interestOps(key.interestOps() & (~SelectionKey.OP_READ));
                    pool.execute(new ThreadHandlerChannel(key));
                }
            }
        }

    }

}

class ThreadHandlerChannel extends Thread {
    SelectionKey key;

    ThreadHandlerChannel(SelectionKey key) {
        this.key = key;
    }

    public void run() {
        SocketChannel sc = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            int len = sc.read(buffer);
            while (len > 0) {
                buffer.flip();
                baos.write(buffer.array(), 0, len);
                buffer.clear();
            }
            baos.close();
            byte[] content = baos.toByteArray();
            ByteBuffer bufferToWrite = ByteBuffer.allocate(content.length);
            bufferToWrite.put(content);
            bufferToWrite.flip();
            sc.write(bufferToWrite);
            if (len == -1) {
                sc.close();
            } else {
                key.interestOps(key.interestOps() | SelectionKey.OP_READ);
                key.selector().wakeup();
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
