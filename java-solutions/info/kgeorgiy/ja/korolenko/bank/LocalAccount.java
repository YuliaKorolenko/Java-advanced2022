package info.kgeorgiy.ja.korolenko.bank;

import java.io.Serializable;
import java.rmi.RemoteException;

public class LocalAccount implements Account, Serializable {

    private final String id;
    private int amount;

    public LocalAccount(final String id) {
        this.id = id;
        amount = 0;
    }

    public LocalAccount(final String id, final int amount){
        this.id = id;
        this.amount = amount;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public synchronized int getAmount() {
        System.out.println("Getting amount of money for account " + id);
        return amount;
    }

    @Override
    public synchronized int setAmount(final int amount) {
        System.out.println("Setting amount of money for account " + id);
        this.amount = amount;
        return this.amount;
    }

    @Override
    public synchronized int addAmount(final int amount) throws RemoteException {
        System.out.println("Adding amount of money for account " + id);
        this.amount += amount;
        return this.amount;
    }


}
