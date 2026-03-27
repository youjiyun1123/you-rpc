package com.you.yourpc.register;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class DefaultServiceRegistry implements ServiceRegistry {

    private ServiceRegistry delegate;
    private final Map<String, List<ServiceMetadata>> cache = new ConcurrentHashMap<>();

    @Override
    public void init(RegistryConfig config) throws Exception {
        this.delegate = createServiceRegistry(config);
        this.delegate.init(config);
    }

    @Override
    public void registryService(ServiceMetadata metadata) {
        log.info("向{} 注册了一个Service{}", delegate.getClass(), metadata.getServiceName());
        delegate.registryService(metadata);
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

    private ServiceRegistry createServiceRegistry(RegistryConfig config) {
        if (config.getRegistryType().equals("zookeeper")) {
            return new ZookeeperServiceRegistry();
        }
        if (config.getRegistryType().equals("redis")) {
            return new RedisServiceRegistry();
        }
        throw new IllegalArgumentException(config.getRegistryType() + "没有实现");
    }
}
