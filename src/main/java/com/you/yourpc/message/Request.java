package com.you.yourpc.message;

import lombok.Data;

import java.util.Objects;

@Data
public class Request {
    private String serviceName;
    private String methodName;
    private String[] paramsClass;
    private Object[] params;
}
