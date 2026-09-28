package org.example.Service;

import java.util.concurrent.atomic.AtomicInteger;

public class VolatileTest {
    private final AtomicInteger count = new AtomicInteger(0);
    public void counter(){
        count.incrementAndGet();
    }
    public int getCount(){
        return count.get();
    }

    public static void main(String[] args) {
        VolatileTest volatileTest = new VolatileTest();
        System.out.println(volatileTest.getCount());
    }
}
