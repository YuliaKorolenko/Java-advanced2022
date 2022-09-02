package info.kgeorgiy.ja.korolenko.bank;

import java.rmi.*;

public interface Account extends Remote {
    /**
     * Returns account identifier.
     */
    String getId() throws RemoteException;

    /**
     * Returns amount of money at the account.
     */
    int getAmount() throws RemoteException;

    /**
     * Sets amount of money at the account.
     */
    int setAmount(int amount) throws RemoteException;

    /**
     * Add money at the account
     */
    int addAmount(int amount) throws RemoteException;
}