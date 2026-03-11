package com.you.yourpc.message;

import lombok.Data;

import java.util.Objects;

@Data
public class Response {
    private Object result;
    private int code;
    private String errorMessage;

    public static Response fail(String errMessage){
        Response response = new Response();
        response.errorMessage=errMessage;
        response.code=400;
        return response;
    }
    public static Response success(Object result){
        Response response = new Response();
        response.result=result;
        response.code=200;
        return response;
    }
}
