package com.you.yourpc.consumer;

import com.you.yourpc.api.Add;
import com.you.yourpc.api.exception.RpcException;
import com.you.yourpc.codec.RequestEncoder;
import com.you.yourpc.codec.XYDecoder;
import com.you.yourpc.message.Request;
import com.you.yourpc.message.Response;
import com.you.yourpc.register.RegisterConfig;
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
        RegisterConfig registerConfig=new RegisterConfig();
        registerConfig.setRegisterType("zookeeper");
        registerConfig.setConnectString("127.0.0.1:2181");
        ConsumerProxyFactory consumerProxyFactory = new ConsumerProxyFactory(registerConfig);
        Add addConsumer = consumerProxyFactory.createConsumerProxy(Add.class);
        while (true){
            try {
                System.out.println(addConsumer.add(1, 2));
            }catch (Exception e){
                e.printStackTrace();
            }
            Thread.sleep(1000);
        }

    }

}
