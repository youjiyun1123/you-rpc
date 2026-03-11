package com.you.yourpc.message;

import lombok.Data;

import java.util.Objects;

@Data
public class Response {
    private Object result;
    private int code;
    private String errorMessage;
    private int requestId;

    public static Response fail(String errMessage,int requestId){
        Response response = new Response();
        response.errorMessage=errMessage;
        response.code=400;
        response.requestId=requestId;
        return response;
    }
    public static Response success(Object result,int requestId){
        Response response = new Response();
        response.result=result;
        response.code=200;
        response.requestId=requestId;
        return response;
    }
}
