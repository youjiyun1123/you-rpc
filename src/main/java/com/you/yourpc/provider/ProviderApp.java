package com.you.yourpc.provider;

import com.you.yourpc.api.Add;
import com.you.yourpc.register.RegistryConfig;

public class ProviderApp {
    public static void main(String[] args) {
        RegistryConfig registerConfig = new RegistryConfig();
        registerConfig.setRegistryType("zookeeper");
        registerConfig.setConnectString("127.0.0.1:2181");
        ProviderProperties properties = new ProviderProperties();
        properties.setHost("127.0.0.1");
        properties.setPort(8888);
        properties.setRegistryConfig(registerConfig);
        ProviderServer providerServer = new ProviderServer(properties);
        providerServer.register(Add.class, new AddImpl());
        providerServer.start();
    }
}
