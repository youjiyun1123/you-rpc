package com.you.yourpc.provider;

import com.you.yourpc.api.Add;

public class ProviderApp {
    public static void main(String[] args) {
        ProviderServer providerServer = new ProviderServer(8888);
        providerServer.register(Add.class, new AddImpl());
        providerServer.start();
    }
}
