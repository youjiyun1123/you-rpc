package com.you.yourpc.consumer;

import com.you.yourpc.api.Add;
import com.you.yourpc.api.exception.RpcException;
import com.you.yourpc.codec.RequestEncoder;
import com.you.yourpc.message.Request;
import com.you.yourpc.message.Response;
import com.you.yourpc.codec.ResponseEncoder;
import com.you.yourpc.codec.XYDecoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class Consumer implements Add {
    public int add(int a, int b) {
        try {
            CompletableFuture<Integer> addResultFuture = new CompletableFuture<>();
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
                                            if (response.getCode() == 200) {
                                                addResultFuture.complete(Integer.valueOf(response.getResult().toString()));
                                            } else {
                                                addResultFuture.completeExceptionally(new RpcException(response.getErrorMessage()));
                                            }
                                            channelHandlerContext.close();
                                        }
                                    });
                        }
                    });
            ChannelFuture channelFuture = bootstrap.connect("localhost", 8888).sync();
            Request request = new Request();
            request.setMethodName("add");
            request.setParams(new Object[]{a, b});
            request.setParamsClass(new Class[]{int.class, int.class});
            request.setServiceName(Add.class.getName());
            channelFuture.channel().writeAndFlush(request);
            return addResultFuture.get(3, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
