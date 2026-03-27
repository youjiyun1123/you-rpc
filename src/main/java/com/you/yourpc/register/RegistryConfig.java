package com.you.yourpc.register;

import lombok.Data;

@Data
public class RegistryConfig {
    private String registryType="zookeeper";
    private String connectString;
}
