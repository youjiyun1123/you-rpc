
package com.you.yourpc.loadbalance;

import com.you.yourpc.register.ServiceMetadata;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class RandomLoadBalancer implements LoadBalancer {
    private final Random random=new Random();

    @Override
    public ServiceMetadata select(List<ServiceMetadata> metadataList) {
        int metadataIndex = random.nextInt( metadataList.size());
        return metadataList.get(Math.abs(metadataIndex));
    }
}
