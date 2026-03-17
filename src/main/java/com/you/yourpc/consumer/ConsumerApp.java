package com.you.yourpc.consumer;

import com.you.yourpc.api.Add;
import com.you.yourpc.api.exception.RpcException;
import com.you.yourpc.codec.RequestEncoder;
import com.you.yourpc.codec.XYDecoder;
import com.you.yourpc.message.Request;
import com.you.yourpc.message.Response;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ConsumerApp {
    public static void main(String[] args) throws Exception {
        ConsumerProxyFactory consumerProxyFactory = new ConsumerProxyFactory();
        for (int i = 0; i < 10; i++) {
            Add addConsumer = consumerProxyFactory.createConsumerProxy(Add.class);
            System.out.println(addConsumer.add(1, 2));
            System.out.println(addConsumer.add(11, 22));
        }


    }

}
