package com.you.yourpc.provider;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ProviderRegister {
    private final Map<String, Invocation<?>> serviceInstanceMap = new ConcurrentHashMap<>();

    public <I> void register(Class<I> interfaceClass, I serviceInstance) {
        if (!interfaceClass.isInterface()) {
            throw new IllegalArgumentException("注册类型必须是一个接口！");
        }
        if (serviceInstanceMap.putIfAbsent(interfaceClass.getName(), new Invocation<>(interfaceClass, serviceInstance)) != null) {
            throw new IllegalArgumentException(interfaceClass.getName() + "重复注册了！");
        }
    }

    public Invocation<?> findService(String serviceName) {
        return serviceInstanceMap.get(serviceName);
    }

    public List<String> allServiceName(){
        return new ArrayList<>(this.serviceInstanceMap.keySet());
    }
    public static class Invocation<I> {
        final I serviceInstance;
        final Class<I> interfaceClass;

        public Invocation(Class<I> interfaceClass, I serviceInstance) {
            this.interfaceClass = interfaceClass;
            this.serviceInstance = serviceInstance;
        }

        public Object invoke(String methodName, Class<?>[] paramsClass, Object[] params) throws Exception {
            Method invokeMethod = interfaceClass.getDeclaredMethod(methodName, paramsClass);
            return invokeMethod.invoke(serviceInstance, params);

        }
    }

}
