package com.you.yourpc.loadbalance;

import com.you.yourpc.register.ServiceMetadata;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinLoadBalancer implements LoadBalancer {
    private final AtomicInteger index = new AtomicInteger();

    @Override
    public ServiceMetadata select(List<ServiceMetadata> metadataList) {
        int metadataIndex = index.getAndIncrement() % metadataList.size();
        return metadataList.get(Math.abs(metadataIndex));
    }
}
