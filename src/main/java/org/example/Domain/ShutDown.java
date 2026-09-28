package org.example.Domain;

import java.util.Map;

public class ShutDown extends Order{

    public ShutDown() {
        super(Integer.MAX_VALUE, Map.of(), null, Priority.NORMAL);
    }



    @Override
    public boolean shutdown() {
        return true;
    }

    @Override
    public String toString() {
        return "Shut Down";
    }
}
