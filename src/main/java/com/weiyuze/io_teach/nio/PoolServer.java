package com.weiyuze.io_teach.nio;

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

    ExecutorService pool = Executors.newFixedThreadPool(50);//固定多少个线程

    private Selector selector;
    //中文测试

    /**
     *
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        PoolServer server = new PoolServer();
        server.initServer(8000);
        server.listen();
    }

    /**
     *
     * @param port
     * @throws IOException
     */
    public void initServer(int port) throws IOException {
        //
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        //
        serverChannel.configureBlocking(false);
        //
        serverChannel.socket().bind(new InetSocketAddress(port));
        //
        this.selector = Selector.open();

        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("服务端启动成功！");
    }

    /**
     *
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    public void listen() throws IOException {
        // 轮询访问selector  
        while (true) {
            //
            selector.select();
            //
            Iterator ite = this.selector.selectedKeys().iterator();
            while (ite.hasNext()) {
                SelectionKey key = (SelectionKey) ite.next();
                //
                ite.remove();
                //
                if (key.isAcceptable()) {
                    ServerSocketChannel server = (ServerSocketChannel) key.channel();
                    //
                    SocketChannel channel = server.accept();
                    //
                    channel.configureBlocking(false);
                    //
                    channel.register(this.selector, SelectionKey.OP_READ);
                    //
                } else if (key.isReadable()) {
                    //位运算，去除掉事件 相当于Iterator.remove()
                    key.interestOps(key.interestOps()&(~SelectionKey.OP_READ));
                    //启一个新线程交给线程池处理，线程池里谁空谁来处理
                    pool.execute(new ThreadHandlerChannel(key));
                }
            }
        }
    }
}

/**
 *
 * @param
 * @throws IOException
 */
class ThreadHandlerChannel extends Thread{
    private SelectionKey key;
    ThreadHandlerChannel(SelectionKey key){
        this.key=key;
    }
    @Override
    public void run() {
        //
        SocketChannel channel = (SocketChannel) key.channel();//通过key拿到对应的Channel
        //
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        //
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            int size = 0;
            while ((size = channel.read(buffer)) > 0) {
                buffer.flip();
                baos.write(buffer.array(),0,size);//读进来的内容写到字节数组
                buffer.clear();
            }
            baos.close();
            //写完之后 将字节数组组织起来 返回回去
            byte[] content=baos.toByteArray();
            ByteBuffer writeBuf = ByteBuffer.allocate(content.length);
            writeBuf.put(content);
            writeBuf.flip();
            channel.write(writeBuf);//
            if(size==-1){

                channel.close();
            }else{
                //处理完将事件添加回来
                key.interestOps(key.interestOps()|SelectionKey.OP_READ);
                key.selector().wakeup();//通知大管家继续执行
            }
        }catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
