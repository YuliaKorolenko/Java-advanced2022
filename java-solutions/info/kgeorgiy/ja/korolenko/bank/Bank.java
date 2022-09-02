package info.kgeorgiy.ja.korolenko.bank;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

public interface Bank extends Remote {
    /**
     * Creates a new account with specified identifier if it is not already exists.
     *
     * @param person person, who create account.
     * @param id account id
     * @return created or existing account.
     */
    Account createAccount(Person person, String id) throws RemoteException;

    /**
     * Returns account by identifier.
     *
     * @param person person, who make account.
     * @param mode mode type of return object : Local or Remote.
     * @return account with specified identifier or {@code null} if such account does not exists.
     */
    Account getAccount(Person person, String id, TypeObject mode) throws RemoteException;

    /**
     * Creates a new person with specified identifier if it is not already exists.
     *
     * @param name           person name.
     * @param surName        person surname.
     * @param passportNumber person passportNumber.
     * @return created or existing account.
     */

    Person createPerson(String name, String surName, String passportNumber) throws RemoteException;

    /**
     * Returns person by identifier.
     *
     * @param id person passportId.
     * @param mode type of return object : Local or Remote.
     * @return created or existing person.
     * @throws RemoteException
     */
    Person getPerson(String id, TypeObject mode) throws RemoteException;

    /**
     * Return Map from id to Account, which belong to person
     * @param person who want to get accounts map
     * @param mode type or returns accounts
     * @return Map of Accounts
     * @throws RemoteException
     */
    Map<String, ? extends Account> getPersonAccounts(final Person person, final TypeObject mode) throws RemoteException;
}
