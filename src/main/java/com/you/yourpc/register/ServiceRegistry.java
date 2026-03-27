package com.you.yourpc.register;

import java.util.List;

public interface ServiceRegistry {
    void init(RegistryConfig config) throws Exception;
    void registryService(ServiceMetadata metadata);

    List<ServiceMetadata> fetchServiceList(String serviceName) throws Exception;
}
