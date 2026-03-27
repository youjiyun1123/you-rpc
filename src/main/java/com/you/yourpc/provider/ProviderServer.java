package com.you.yourpc.provider;

import com.you.yourpc.message.Request;
import com.you.yourpc.codec.ResponseEncoder;
import com.you.yourpc.codec.XYDecoder;
import com.you.yourpc.message.Response;
import com.you.yourpc.register.*;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class ProviderServer {
    private final int port;
    private final String host;
    private final ProviderRegister register;
    private final ServiceRegister serviceRegister;
    private final RegisterConfig registerConfig;
    private EventLoopGroup bossEventLoopGroup;
    private EventLoopGroup workEventLoopGroup;


    public ProviderServer(String host, int port, RegisterConfig registerConfig) {
        this.host = host;
        this.port = port;
        this.register = new ProviderRegister();
        this.serviceRegister = new DefaultServiceRegister();
        this.registerConfig = registerConfig;
    }

    public <I> void register(Class<I> interfaceClass, I serviceInstance) {
        register.register(interfaceClass, serviceInstance);
    }

    public void start() {
        bossEventLoopGroup = new NioEventLoopGroup();
        workEventLoopGroup = new NioEventLoopGroup(4);
        try {
            serviceRegister.init(registerConfig);
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossEventLoopGroup, workEventLoopGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<NioSocketChannel>() {
                        protected void initChannel(NioSocketChannel nioSocketChannel) throws Exception {
                            nioSocketChannel.pipeline()
                                    .addLast(new XYDecoder())
                                    .addLast(new ResponseEncoder())
                                    .addLast(new ProviderHandler());

                        }
                    });
            serverBootstrap.bind(port).sync();
            //注册服务
            register.allServiceName().stream().map(this::buildMetadata).forEach(this.serviceRegister::registerService);
        } catch (Exception e) {
            throw new RuntimeException("服务器启动异常", e);
        }

    }

    private ServiceMetadata buildMetadata(String serviceName) {
        ServiceMetadata metadata = new ServiceMetadata();
        metadata.setServiceName(serviceName);
        metadata.setPort(port);
        metadata.setHost(host);
        return metadata;
    }

    public class ProviderHandler extends SimpleChannelInboundHandler<Request> {
        protected void channelRead0(ChannelHandlerContext channelHandlerContext, Request request) throws Exception {
            ProviderRegister.Invocation<?> invocation = register.findService(request.getServiceName());
            if (invocation == null) {
                Response failResp = Response.fail(String.format("%s 没有对应的处理服务", request.getServiceName()), request.getRequestId());
                channelHandlerContext.writeAndFlush(failResp);
                return;
            }
            try {
                Object result = invocation.invoke(request.getMethodName(), request.getParamsClass(), request.getParams());
                log.info("{},函数被调用了{},结果是{}", request.getServiceName(), request.getMethodName(), result);
                channelHandlerContext.writeAndFlush(Response.success(result, request.getRequestId()));
            } catch (Exception e) {
                Response failResp = Response.fail(e.getMessage(), request.getRequestId());
                channelHandlerContext.writeAndFlush(failResp);
            }
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            log.info("地址:{}连接了", ctx.channel().remoteAddress());
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            log.info("地址:{}断开了连接了", ctx.channel().remoteAddress());
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            log.error("发生了异常", cause);
            super.exceptionCaught(ctx, cause);
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
