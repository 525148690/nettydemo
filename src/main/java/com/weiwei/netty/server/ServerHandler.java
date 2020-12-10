package com.weiwei.netty.server;

import com.weiwei.netty.entity.Model;
import com.weiwei.netty.entity.TypeData;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServerHandler extends ChannelInboundHandlerAdapter {
    private final Logger log = LoggerFactory.getLogger(ServerHandler.class);
    private String name;
    private static final Integer errorCount = 5; // 允许超时的次数

    // 存放当前的连接
    private static Map<Integer, Channel> channels = new ConcurrentHashMap<Integer, Channel>();
    // 消息超时的时候记录  超过一定次数就断开客户端
    private static Map<Integer, Integer> errors = new ConcurrentHashMap<Integer, Integer>();

    ServerHandler(String name) {
        this.name = name;
    }

    /**
     * 有连接进入的时候触发
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info(" ---客户端" + ctx.channel().remoteAddress() + "----已连接到server");
        putChannel(ctx);
    }

    /**
     * 有连接退出的时候触发
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.warn(" ---" + ctx.channel().remoteAddress() + "----客户端连接退出");
        removeChannel(ctx.hashCode());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Model m = (Model) msg;
        int type = m.getType();
        if (TypeData.PING == type) {
            // 收到客户端的ping  消息，回复 pong 表示收到
            sendPongMsg(ctx);
        } else {
            //其他类型消息
            Model model = (Model) msg;
            log.info("server收到client的消息， msg = " + model.toString());
            ctx.channel().writeAndFlush(model); // 返回给客户端
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent stateEvent = (IdleStateEvent) evt;
            if (stateEvent.state() == IdleState.READER_IDLE) {
                handlerReaderIdle(ctx);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.info(name + "  exception" + cause.toString());
        super.exceptionCaught(ctx, cause);
    }

    /**
     * 回复pong  内容
     *
     * @param ctx
     */
    private void sendPongMsg(ChannelHandlerContext ctx) {
        Model model = new Model();
        model.setType(TypeData.PONG);
        ctx.channel().writeAndFlush(model);
        // 收到ping 消息  回复的时候清空 超时次数
        restErroes(ctx.hashCode());
        log.info(this.name + " 发送 pong 消息 to " + ctx.channel().remoteAddress() /*+" , count :" + heartbeatCount*/);
    }

    /**
     * 消息超时处理
     *
     * @param ctx
     */
    private void handlerReaderIdle(ChannelHandlerContext ctx) {
        log.info("已经10s未收到客户端的消息啦");
        if (addErrors(ctx.hashCode()) >= errorCount) {
            // 超时次数超过5次，将客户端连接移除
            ctx.close();
            removeChannel(ctx.hashCode());
            log.warn(" ---- client " + ctx.channel().remoteAddress().toString() + " reader timeOut, --- close it");
        }
    }

    /**
     * 连接成功，重置错误次数
     *
     * @param hashCode
     */
    private static void restErroes(Integer hashCode) {
        errors.put(hashCode, 0);
    }

    /**
     * 超时，计数加一
     *
     * @param hashCode
     */
    private Integer addErrors(Integer hashCode) {
        Integer hearCount;
        if (errors.containsKey(hashCode)) {
            hearCount = errors.get(hashCode);
            hearCount++;
        } else {
            hearCount = 1;
        }
        errors.put(hashCode, hearCount);
        return hearCount;
    }

    // 判断 连接是否在线
    public static boolean containsKey(Integer hashCode) {
        return channels.containsKey(hashCode);
    }

    /**
     * 添加连接
     */
    private synchronized void putChannel(ChannelHandlerContext ctx) {
        Integer hashCode = ctx.hashCode();
        // 存入连接
        channels.put(hashCode, ctx.channel());
    }

    /**
     * 断开连接
     *
     * @param hashCode
     */
    private synchronized void removeChannel(Integer hashCode) {
        if (channels.containsKey(hashCode)) {
            channels.remove(hashCode);
            errors.remove(hashCode);
        }
    }
}
