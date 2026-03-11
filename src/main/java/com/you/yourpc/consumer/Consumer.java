package com.you.yourpc.consumer;

import com.you.yourpc.api.Add;
import com.you.yourpc.api.exception.RpcException;
import com.you.yourpc.codec.RequestEncoder;
import com.you.yourpc.message.Request;
import com.you.yourpc.message.Response;
import com.you.yourpc.codec.ResponseEncoder;
import com.you.yourpc.codec.XYDecoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class Consumer implements Add {
    //在途请求
    private final Map<Integer, CompletableFuture<?>> inFlightRequestTable = new ConcurrentHashMap<>();
    private ConnectionManager manager=new ConnectionManager(createBootstrap());

    private Bootstrap createBootstrap() {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(new NioEventLoopGroup(4))
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {

                    protected void initChannel(NioSocketChannel nioSocketChannel) throws Exception {
                        nioSocketChannel.pipeline()
                                .addLast(new XYDecoder())
                                .addLast(new RequestEncoder())
                                .addLast(new SimpleChannelInboundHandler<Response>() {
                                    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Response response) throws Exception {
                                        CompletableFuture requestFuture = inFlightRequestTable.remove(response.getRequestId());
                                        if (response.getCode() == 200) {
                                            requestFuture.complete(Integer.valueOf(response.getResult().toString()));
                                        } else {
                                            requestFuture.completeExceptionally(new RpcException(response.getErrorMessage()));
                                        }
                                    }
                                });
                    }
                });
        return bootstrap;
    }

    public int add(int a, int b) {
        try {
            CompletableFuture<Integer> addResultFuture = new CompletableFuture<>();
            Channel channel = manager.getChannel("localhost", 8888);
            if (channel == null) {
                throw new RpcException("provider 连接失败");
            }
            Request request = new Request();
            request.setMethodName("add");
            request.setParams(new Object[]{a, b});
            request.setParamsClass(new Class[]{int.class, int.class});
            request.setServiceName(Add.class.getName());
            channel.writeAndFlush(request).addListener((f) -> {
                if (f.isSuccess()) {
                    inFlightRequestTable.put(request.getRequestId(), addResultFuture);
                }
            });
            return addResultFuture.get(3, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
