package com.you.yourpc.consumer;

import com.you.yourpc.api.Add;
import com.you.yourpc.api.exception.RpcException;
import com.you.yourpc.codec.RequestEncoder;
import com.you.yourpc.codec.XYDecoder;
import com.you.yourpc.message.Request;
import com.you.yourpc.message.Response;
import com.you.yourpc.provider.AddImpl;
import com.you.yourpc.register.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ConsumerProxyFactory {
    private final Map<Integer, CompletableFuture<Response>> inFlightRequestTable = new ConcurrentHashMap<>();
    private final ConnectionManager manager = new ConnectionManager(createBootstrap());

    private final ServiceRegister register;

    public ConsumerProxyFactory(RegisterConfig registerConfig) throws Exception {
        this.register = new DefaultServiceRegister();
        this.register.init(registerConfig);
    }

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
                                        CompletableFuture<Response> responseFuture = inFlightRequestTable.remove(response.getRequestId());
                                        if (responseFuture == null) {
                                            log.warn("requestId{}找不到", response.getRequestId());
                                            return;
                                        }
                                        responseFuture.complete(response);
                                    }
                                });
                    }
                });
        return bootstrap;
    }

    public <I> I createConsumerProxy(Class<I> interfaceClass) {
        return (I) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[]{interfaceClass}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if (method.getDeclaringClass() == Object.class) {
                    if (method.getName().equals("toString")) {
                        return "You Proxy Consumer " + interfaceClass.getName();
                    }
                    if (method.getName().equals("equals")) {
                        return proxy == args[0];
                    }
                    if (method.getName().equals("hashCode")) {
                        return System.identityHashCode(proxy);
                    }
                    throw new UnsupportedOperationException("代理对象不支持这个函数" + method.getName());
                }
                try {
                    CompletableFuture<Response> responseFuture = new CompletableFuture<>();
                    List<ServiceMetadata> serviceMetadata = register.fetchServiceList(interfaceClass.getName());
                    if (serviceMetadata.isEmpty()){
                        throw new RpcException(interfaceClass.getName() + "没有对应的provider");
                    }
                    ServiceMetadata providerMetadata = serviceMetadata.get(0);

                    Channel channel = manager.getChannel(providerMetadata.getHost(), providerMetadata.getPort());
                    if (channel == null) {
                        throw new RpcException("provider 连接失败");
                    }
                    Request request = new Request();
                    request.setMethodName(method.getName());
                    request.setParams(args);
                    request.setParamsClass(method.getParameterTypes());
                    request.setServiceName(interfaceClass.getName());
                    inFlightRequestTable.put(request.getRequestId(), responseFuture);
                    channel.writeAndFlush(request).addListener((f) -> {
                        if (!f.isSuccess()) {
                            inFlightRequestTable.remove(request.getRequestId());
                            responseFuture.completeExceptionally(f.cause());
                        }
                    });
                    Response response = responseFuture.get(3, TimeUnit.SECONDS);
                    if (response.getCode() == 200) {
                        return response.getResult();
                    }
                    throw new RpcException(response.getErrorMessage());

                } catch (RpcException rpcException) {
                    throw rpcException;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

            }
        });
    }
}
