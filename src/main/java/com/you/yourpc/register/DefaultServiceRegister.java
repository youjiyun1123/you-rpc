package com.you.yourpc.register;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class DefaultServiceRegister implements ServiceRegister {

    private ServiceRegister delegate;
    private final Map<String, List<ServiceMetadata>> cache = new ConcurrentHashMap<>();

    @Override
    public void init(RegisterConfig config) throws Exception {
        this.delegate = createServiceRegister(config);
        this.delegate.init(config);
    }

    @Override
    public void registerService(ServiceMetadata metadata) {
        log.info("向{} 注册了一个Service{}", delegate.getClass(), metadata.getServiceName());
        delegate.registerService(metadata);
    }

    @Override
    public List<ServiceMetadata> fetchServiceList(String serviceName) {
        try {
            List<ServiceMetadata> serviceMetadata = delegate.fetchServiceList(serviceName);
            cache.put(serviceName, serviceMetadata);
            return serviceMetadata;
        } catch (Exception e) {
            log.error("{}注册中心查询{}出现异常", delegate.getClass().getSimpleName(), serviceName, e);
            return cache.getOrDefault(serviceName, new ArrayList<>());
        }
    }

    private ServiceRegister createServiceRegister(RegisterConfig config) {
        if (config.getRegisterType().equals("zookeeper")) {
            return new ZookeeperServiceRegister();
        }
        if (config.getRegisterType().equals("redis")) {
            return new RedisServiceRegister();
        }
        throw new IllegalArgumentException(config.getRegisterType() + "没有实现");
    }
}
