package com.you.yourpc.provider;

import com.you.yourpc.message.Request;
import com.you.yourpc.codec.ResponseEncoder;
import com.you.yourpc.codec.XYDecoder;
import com.you.yourpc.message.Response;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class ProviderServer {
    private final int port;
    private final ProviderRegister register;
    private EventLoopGroup bossEventLoopGroup;
    private EventLoopGroup workEventLoopGroup;


    public ProviderServer(int port) {
        this.port = port;
        this.register = new ProviderRegister();
    }

    public <I> void register(Class<I> interfaceClass, I serviceInstance) {
        register.register(interfaceClass, serviceInstance);
    }

    public void start() {
        bossEventLoopGroup = new NioEventLoopGroup();
        workEventLoopGroup = new NioEventLoopGroup(4);
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossEventLoopGroup, workEventLoopGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<NioSocketChannel>() {
                        protected void initChannel(NioSocketChannel nioSocketChannel) throws Exception {
                            nioSocketChannel.pipeline()
                                    .addLast(new XYDecoder())
                                    .addLast(new ResponseEncoder())
                                    .addLast(new SimpleChannelInboundHandler<Request>() {
                                        // add,1,2
                                        protected void channelRead0(ChannelHandlerContext channelHandlerContext, Request request) throws Exception {
                                            ProviderRegister.Invocation<?> service = register.findService(request.getServiceName());
                                            Object result = service.invoke(request.getMethodName(), request.getParamsClass(),request.getParams());
                                            Response response = new Response();
                                            response.setResult(result);
                                            channelHandlerContext.writeAndFlush(response);
                                        }
                                    });

                        }
                    });
            serverBootstrap.bind(port).sync();
        } catch (Exception e) {
            throw new RuntimeException("服务器启动异常", e);
        }

    }

    public void stop() {
        if (bossEventLoopGroup != null) {
            bossEventLoopGroup.shutdownGracefully();
        }
        if (workEventLoopGroup != null) {
            workEventLoopGroup.shutdownGracefully();
        }
    }


}
