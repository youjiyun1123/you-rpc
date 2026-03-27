package com.you.yourpc.loadbalance;

import com.you.yourpc.register.ServiceMetadata;

import java.util.List;

public interface LoadBalancer {
    ServiceMetadata select(List<ServiceMetadata> metadataList);
}
