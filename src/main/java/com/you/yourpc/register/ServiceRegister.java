package com.you.yourpc.register;

import java.util.List;

public interface ServiceRegister {
    void init(RegisterConfig config) throws Exception;
    void registerService(ServiceMetadata metadata);

    List<ServiceMetadata> fetchServiceList(String serviceName) throws Exception;
}
