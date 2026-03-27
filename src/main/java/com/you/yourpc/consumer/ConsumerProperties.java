package com.you.yourpc.consumer;

import com.you.yourpc.register.RegistryConfig;
import lombok.Data;

@Data
public class ConsumerProperties {
    private Integer workThreadNum=4;
    private Integer connectTimeoutMs=3000;
    private Integer requestTimeoutMs=3000;
    private String loadBalancerPolicy="robin";
    private RegistryConfig registryConfig=new RegistryConfig();
}
