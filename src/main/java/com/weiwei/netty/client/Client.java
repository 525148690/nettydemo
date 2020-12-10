package com.weiwei.netty.client;

import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.weiwei.netty.decode.KyroMsgDecoder;
import com.weiwei.netty.decode.KyroMsgEncoder;
import com.weiwei.netty.entity.Model;
import com.weiwei.netty.entity.TypeData;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Client {
    private static final Logger log = LoggerFactory.getLogger(Client.class);
    private final String ADDRESS = "localhost";
    private final Integer PORT = 8081;
    private String name;
    private static int reconnectCount = 0; // 重连次数

    public Client(String clientName) {
        this.name = clientName;
    }

    public String getName() {
        return this.name;
    }

    private NioEventLoopGroup worker = new NioEventLoopGroup();

    private static CountDownLatch countDownLatch = new CountDownLatch(1);
    private Channel channel;

    private Bootstrap bootstrap;

    public static void main(String[] args) throws InterruptedException {
        Client client = new Client("client1");
        log.info("客户端启动中......");
        client.start();
        countDownLatch.await();
        log.info("客户端启动成功");
        client.sendData();
    }

    private void start() {
        bootstrap = new Bootstrap();
        bootstrap.group(worker)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new IdleStateHandler(0, 0, 10)); // 设置客户端的机制为 10 秒触发一次ping
                        //pipeline.addLast(new MsgPckDecode()); // 编码器
                        //pipeline.addLast(new MsgPckEncode()); // 解码器
                        pipeline.addLast(new KyroMsgDecoder());
                        pipeline.addLast(new KyroMsgEncoder());

                        pipeline.addLast(new ClientHandler(Client.this));   // 当前客户端的控制器
                    }
                });
        doConnect();
    }

    /**
     * 连接服务端 and 重连
     */
    protected void doConnect() {

        if (channel != null && channel.isActive()) {
            return;
        }
        ChannelFuture connect = bootstrap.connect(ADDRESS, PORT);
        //实现监听通道连接的方法
        connect.addListener(new ChannelFutureListener() {

            public void operationComplete(final ChannelFuture channelFuture) throws Exception {

                if (channelFuture.isSuccess()) {
                    channel = channelFuture.channel();
                    log.info("连接成功");
                    reconnectCount = 0; //重置重连次数
                    countDownLatch.countDown();
                } else {
                    //System.out.println("每隔3s重连....");
                    channelFuture.channel().eventLoop().schedule(new Runnable() {

                        public void run() {
                            if (reconnectCount < 5) {
                                reconnectCount ++;
                                log.info(String.format("每隔3s重连.....重连%d次", reconnectCount));
                                doConnect();
                            } else {
                                channelFuture.channel().close();
                                log.info("超过5次连接，终止重连");
                                System.exit(-1);
                            }
                        }
                    }, 3, TimeUnit.SECONDS);
                }
            }
        });
    }

    /**
     * 向服务端发送消息
     */
    private void sendData() {
        Scanner sc = new Scanner(System.in);
        while (true) {
            if (channel != null && channel.isActive()) {
                //获取一个键盘扫描器
                System.err.println("请输入要发送的消息");
                String nextLine = sc.nextLine();
                Model model = new Model();

                model.setType(TypeData.CONTENT);

                model.setBody(nextLine);
                System.err.println("client 发送数据到server， msg = " + model.toString());
                channel.writeAndFlush(model);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

