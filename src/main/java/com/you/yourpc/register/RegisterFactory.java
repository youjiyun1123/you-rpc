package com.you.yourpc.register;

public class RegisterFactory {
    public static ServiceRegister createServiceRegister(RegisterConfig config) {
        if (config.getRegisterType().equals("zookeeper")) {
            return new ZookeeperServiceRegister();
        }
        if (config.getRegisterType().equals("redis")) {
            return new RedisServiceRegister();
        }
        throw new IllegalArgumentException(config.getRegisterType() + "没有实现");
    }
}
