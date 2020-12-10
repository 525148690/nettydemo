package com.weiwei.netty.server;

import com.weiwei.netty.decode.KyroMsgDecoder;
import com.weiwei.netty.decode.KyroMsgEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Server {
    private static final Logger log = LoggerFactory.getLogger(Server.class);
    private static final Integer PORT = 8081;

    public static void main(String[] args) {

        EventLoopGroup bossGroup = new NioEventLoopGroup(1);

        EventLoopGroup workerGroup = new NioEventLoopGroup(4);
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();

            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .localAddress(PORT)
                    .childHandler(new ChannelInitializer<Channel>() {

                        @Override
                        protected void initChannel(Channel ch) throws Exception {
                            // TODO Auto-generated method stub
                            ChannelPipeline pipeline = ch.pipeline();
                            // 设置读 机制为 20秒读一次(执行channelRead方法，20s后没读到数据就会触发userEventTriggered方法)
                            pipeline.addLast(new IdleStateHandler(20,0,0));
                            //pipeline.addLast(new MsgPckDecode());
                            //pipeline.addLast(new MsgPckEncode());
                            pipeline.addLast(new KyroMsgDecoder());
                            pipeline.addLast(new KyroMsgEncoder());
                            pipeline.addLast(new ServerHandler("server"));
                        }
                    });
            log.info("服务启动端口为： "+PORT+" --");
            ChannelFuture sync = serverBootstrap.bind().sync();
            sync.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            log.error(e.getMessage());
        }finally{
            //优雅的关闭资源
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}

