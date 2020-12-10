package com.weiwei.netty.client;

import com.weiwei.netty.entity.Model;
import com.weiwei.netty.entity.TypeData;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientHandler extends ChannelInboundHandlerAdapter {
    private final Logger log = LoggerFactory.getLogger(ClientHandler.class);
    private String name;
    private Client client;
    //记录次数
    private int heartbeatCount = 0;

    public ClientHandler(Client client) {
        this.client = client;
        this.name = client.getName();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("--已成功连接服务端：" + ctx.channel().remoteAddress());
        //System.out.println(ctx.channel().isActive());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        //super.channelInactive(ctx);
        log.info(name + "已断开与服务端的连接");
        //client.doConnect();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Model m = (Model) msg;
        int type = m.getType();
        switch (type) {
            case TypeData.PONG:
                log.info(name + " 收到回复  pong  消息来自服务端： " + ctx.channel().remoteAddress() + ", 连接正常");
                break;
            default:
                log.info("client 收到 server发送的消息， msg = " + m.toString());
                break;
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent stateEvent = (IdleStateEvent) evt;
            switch (stateEvent.state()) {
                case READER_IDLE:
                    //handlerReaderIdle(ctx);
                    break;
                case WRITER_IDLE:
                    //handlerWriterIdle(ctx);
                    break;
                case ALL_IDLE:
                    sendPingMsg(ctx);
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.info(name + "  exception" + cause.toString());
        super.exceptionCaught(ctx, cause);
    }

    /**
     * 客户端发送ping 消息使用
     *
     * @param ctx
     */
    protected void sendPingMsg(ChannelHandlerContext ctx) {
        Model model = new Model();
        model.setType(TypeData.PING);
        ctx.channel().writeAndFlush(model);
        heartbeatCount++;
        log.info(name + " 发送 ping 消息 to 服务端：" + ctx.channel().remoteAddress() + ",count : " + heartbeatCount);
    }
}

