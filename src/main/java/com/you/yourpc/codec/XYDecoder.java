package com.you.yourpc.codec;

import com.alibaba.fastjson2.JSONObject;
import com.you.yourpc.message.Message;
import com.you.yourpc.message.Request;
import com.you.yourpc.message.Response;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

public class XYDecoder extends LengthFieldBasedFrameDecoder {
    public XYDecoder() {
        //最大帧1mb,规定表示消息长度的标识位占4字节，0,4,0,4刚好取出消息内容
        super(1024 * 1024, 0, Integer.BYTES, 0, Integer.BYTES);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        ByteBuf frame = (ByteBuf) super.decode(ctx, in);
        byte[] logic = new byte[Message.LOGIC.length];
        //读魔数
        frame.readBytes(logic);
        if (!Arrays.equals(logic, Message.LOGIC)) {
            throw new IllegalArgumentException("魔数不对！协议有问题");
        }
        //读类型
        byte messageType = frame.readByte();
        byte[] body = new byte[frame.readableBytes()];
        //读剩下的字节
        frame.readBytes(body);
        if (Objects.equals(Message.MessageType.REQUEST.getCode(), messageType)) {
            return deserializeRequest(body);
        }
        if (Objects.equals(Message.MessageType.RESPONSE.getCode(), messageType)) {
            return deserializeResponse(body);
        }
        throw new IllegalArgumentException("消息类型不支持" + messageType);
    }

    private Object deserializeResponse(byte[] body) {
        String jsonStr = new String(body, StandardCharsets.UTF_8);
        return JSONObject.parseObject(jsonStr, Response.class);
    }

    private Object deserializeRequest(byte[] body) {
        String jsonStr = new String(body, StandardCharsets.UTF_8);
        return JSONObject.parseObject(jsonStr, Request.class);
    }
}
