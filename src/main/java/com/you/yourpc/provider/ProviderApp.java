package com.you.yourpc.provider;

import com.you.yourpc.api.Add;
import com.you.yourpc.register.RegisterConfig;

public class ProviderApp {
    public static void main(String[] args) {
        RegisterConfig registerConfig=new RegisterConfig();
        registerConfig.setRegisterType("zookeeper");
        registerConfig.setConnectString("127.0.0.1:2181");
        ProviderServer providerServer = new ProviderServer("127.0.0.1",8888,registerConfig);
        providerServer.register(Add.class, new AddImpl());
        providerServer.start();
    }
}
