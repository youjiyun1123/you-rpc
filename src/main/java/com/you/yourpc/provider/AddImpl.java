package com.you.yourpc.provider;

import com.you.yourpc.api.Add;

public class AddImpl implements Add {
    @Override
    public int add(int a, int b) {
        return a + b;
    }
}
