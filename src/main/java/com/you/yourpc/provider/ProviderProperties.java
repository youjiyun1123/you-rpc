package com.you.yourpc.provider;

import com.you.yourpc.register.RegistryConfig;
import lombok.Data;

@Data
public class ProviderProperties {
    private String host;
    private int port;
    private int workThreadNum = 4;
    private RegistryConfig registryConfig;

}
