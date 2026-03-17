package com.you.yourpc.provider;

import com.you.yourpc.api.Add;

public class AddImpl implements Add {
    @Override
    public Integer add(int a, int b) {
        return a + b;
    }

    @Override
    public Integer minus(int a, int b) {
        return a - b;
    }
}
