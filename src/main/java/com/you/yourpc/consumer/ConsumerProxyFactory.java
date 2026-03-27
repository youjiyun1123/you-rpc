package com.you.yourpc.consumer;

import com.you.yourpc.api.Add;
import com.you.yourpc.api.exception.RpcException;
import com.you.yourpc.codec.RequestEncoder;
import com.you.yourpc.codec.XYDecoder;
import com.you.yourpc.loadbalance.LoadBalancer;
import com.you.yourpc.loadbalance.RandomLoadBalancer;
import com.you.yourpc.loadbalance.RoundRobinLoadBalancer;
import com.you.yourpc.message.Request;
import com.you.yourpc.message.Response;
import com.you.yourpc.provider.AddImpl;
import com.you.yourpc.register.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
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
    private final Map<Integer, CompletableFuture<Response>> inFlightRequestTable;
    private final ConnectionManager manager;

    private final ServiceRegistry registry;
    private final ConsumerProperties consumerProperties;

    public ConsumerProxyFactory(ConsumerProperties consumerProperties) throws Exception {
        this.registry = new DefaultServiceRegistry();
        this.registry.init(consumerProperties.getRegistryConfig());
        this.manager = new ConnectionManager(createBootstrap(consumerProperties));
        this.inFlightRequestTable = new ConcurrentHashMap<>();
        this.consumerProperties = consumerProperties;
    }

    private Bootstrap createBootstrap(ConsumerProperties consumerProperties) {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(new NioEventLoopGroup(consumerProperties.getWorkThreadNum()))
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, consumerProperties.getConnectTimeoutMs())
                .handler(new ChannelInitializer<NioSocketChannel>() {

                    protected void initChannel(NioSocketChannel nioSocketChannel) throws Exception {
                        nioSocketChannel.pipeline()
                                .addLast(new XYDecoder())
                                .addLast(new RequestEncoder())
                                .addLast(new ConsumerHandler());
                    }
                });
        return bootstrap;
    }

    private class ConsumerHandler extends SimpleChannelInboundHandler<Response> {
        @Override
        protected void channelRead0(ChannelHandlerContext channelHandlerContext, Response response) throws Exception {
            CompletableFuture<Response> responseFuture = inFlightRequestTable.remove(response.getRequestId());
            if (responseFuture == null) {
                log.warn("requestId{}找不到", response.getRequestId());
                return;
            }
            responseFuture.complete(response);
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

    public <I> I createConsumerProxy(Class<I> interfaceClass) {
        return (I) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                new Class[]{interfaceClass},
                new ConsumerInvocationHandler(interfaceClass,createLoadBalancer())
        );
    }

    private LoadBalancer createLoadBalancer() {
        switch (this.consumerProperties.getLoadBalancerPolicy()) {
            case "robin":
                return new RoundRobinLoadBalancer();
            case "random":
                return new RandomLoadBalancer();
            default:
                throw new IllegalArgumentException(this.consumerProperties.getLoadBalancerPolicy() + "负载均衡不支持");
        }
    }

    public class ConsumerInvocationHandler implements InvocationHandler {
        final Class<?> interfaceClass;
        final LoadBalancer loadBalancer;

        public ConsumerInvocationHandler(Class<?> interfaceClass, LoadBalancer loadBalancer) {
            this.interfaceClass = interfaceClass;
            this.loadBalancer = loadBalancer;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getDeclaringClass() == Object.class) {
                return invokeObjectMethod(proxy, method, args);
            }
            CompletableFuture<Response> responseFuture = new CompletableFuture<>();
            try {
                List<ServiceMetadata> serviceMetadata = registry.fetchServiceList(interfaceClass.getName());
                if (serviceMetadata.isEmpty()) {
                    throw new RpcException(interfaceClass.getName() + "没有对应的provider");
                }
                ServiceMetadata providerMetadata = loadBalancer.select(serviceMetadata);

                Channel channel = manager.getChannel(providerMetadata.getHost(), providerMetadata.getPort());
                if (channel == null) {
                    throw new RpcException("provider 连接失败");
                }
                Request request = buildRequest(method, args);
                inFlightRequestTable.put(request.getRequestId(), responseFuture);
                channel.writeAndFlush(request).addListener((f) -> {
                    if (!f.isSuccess()) {
                        inFlightRequestTable.remove(request.getRequestId());
                        responseFuture.completeExceptionally(f.cause());
                    }
                });
                Response response = responseFuture.get(consumerProperties.getRequestTimeoutMs(), TimeUnit.MILLISECONDS);
                return processResponse(response);

            } catch (RpcException rpcException) {
                throw rpcException;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }

        private Object processResponse(Response response) {
            if (response.getCode() == 200) {
                return response.getResult();
            }
            throw new RpcException(response.getErrorMessage());
        }

        private Request buildRequest(Method method, Object[] args) {
            Request request = new Request();
            request.setMethodName(method.getName());
            request.setParams(args);
            request.setParamsClass(method.getParameterTypes());
            request.setServiceName(interfaceClass.getName());
            return request;
        }

        private Object invokeObjectMethod(Object proxy, Method method, Object[] args) {
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
    }
}
