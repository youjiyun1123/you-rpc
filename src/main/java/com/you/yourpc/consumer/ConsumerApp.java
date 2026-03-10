package com.you.yourpc.consumer;

import com.you.yourpc.api.Add;

public class ConsumerApp {
    public static void main(String[] args) throws Exception {
        Add consumer=new Consumer();
        System.out.println(consumer.add(1,2));
    }
}
