package com.weiwei.netty.entity;

import org.msgpack.annotation.Message;

import java.io.Serializable;

/**
 * 消息类型分离器
 *
 * @author Administrator
 */
@Message
public class Model implements Serializable {

    private static final long serialVersionUID = 1L;
    //类型
    private int type;
    //内容
    private Object body;

    public Model() {
    }

    public Model(int type) {
        this.type = type;
    }

    public Object getBody() {
        return body;
    }

    public void setBody(Object body) {
        this.body = body;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "Model [type=" + type + ", body=" + body + "]";
    }
}
