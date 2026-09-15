package org.example.Domain;
public class ShutDown extends Order{

    public ShutDown() {
        super(Integer.MAX_VALUE, null, null, 0, Priority.NORMAL);
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
