package info.kgeorgiy.ja.korolenko.bank;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RemoteBank implements Bank {
    private final int port;
    private final ConcurrentMap<String, Account> accounts = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Set<String>> passportNumberAccounts = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Person> persons = new ConcurrentHashMap<>();

    public RemoteBank(final int port) {
        this.port = port;
    }

    @Override
    public Account createAccount(final Person person, final String subId) throws RemoteException {
        final String id = person.getPassportNumber() + ":" + subId;
        System.out.println("Creating account " + id);
        final Account account = new RemoteAccount(id);
        if (accounts.putIfAbsent(id, account) == null) {
            UnicastRemoteObject.exportObject(account, port);
            passportNumberAccounts.get(person.getPassportNumber()).add(id);
            return account;
        } else {
            return getAccount(person, id, TypeObject.REMOTE);
        }
    }

    @Override
    public Person getPerson(final String id, final TypeObject mode) throws RemoteException {
        return switch (mode) {
            case LOCAL -> getLocalPerson(id);
            case REMOTE -> getRemotePerson(id);
        };
    }

    private LocalPerson getLocalPerson(final String id) throws RemoteException {
        final Person person = persons.get(id);
        if (person != null) {
            return new LocalPerson(person.getName(), person.getSurname(), person.getPassportNumber());
        } else {
            return null;
        }
    }

    private RemotePerson getRemotePerson(final String id) throws RemoteException {
        return (RemotePerson) persons.get(id);
    }

    @Override
    public Person createPerson(final String name, final String surName, final String passportNumber) throws RemoteException {
        final Person person = new RemotePerson(name, surName, passportNumber);
        if (persons.putIfAbsent(passportNumber, person) == null) {
            passportNumberAccounts.put(passportNumber, ConcurrentHashMap.newKeySet());
            UnicastRemoteObject.exportObject(person, port);
            return person;
        } else {
            return persons.get(passportNumber);
        }

    }

    @Override
    public Account getAccount(final Person person, final String accountId, final TypeObject mode) throws RemoteException {
        final String id = person.getPassportNumber() + ":" + accountId;
        System.out.println("Retrieving account. passport:subID " + id);
        return switch (mode) {
            case LOCAL -> getLocalAccount(id);
            case REMOTE -> getRemoteAccount(id);
        };
    }

    private LocalAccount getLocalAccount(final String passportSubId) throws RemoteException {
        final Account account = accounts.get(passportSubId);
        if (account != null) {
            return new LocalAccount(account.getId());
        } else {
            return null;
        }
    }

    private RemoteAccount getRemoteAccount(final String passportSubId) throws RemoteException {
        return (RemoteAccount) accounts.get(passportSubId);
    }

    @Override
    public Map<String, ? extends Account> getPersonAccounts(final Person person, final TypeObject mode) throws RemoteException {
        final Set<String> accountsId = passportNumberAccounts.get(person.getPassportNumber());
        if (accountsId != null) {
            return switch (mode) {
                case LOCAL -> getLocalPersonAccounts(accountsId);
                case REMOTE -> getRemotePersonAccounts(accountsId);
            };
        }
        return null;
    }

    private Map<String, LocalAccount> getLocalPersonAccounts(final Set<String> accountsId) throws RemoteException {
        final Map<String, LocalAccount> personAccounts = new ConcurrentHashMap<>();
        for (final String id : accountsId) {
            personAccounts.put(id, new LocalAccount(id, accounts.get(id).getAmount()));
        }
        return personAccounts;
    }


    private Map<String, RemoteAccount> getRemotePersonAccounts(final Set<String> accountsId) throws RemoteException {
        final Map<String, RemoteAccount> personAccounts = new ConcurrentHashMap<>();
        accountsId.forEach(id -> personAccounts.put(id, (RemoteAccount) accounts.get(id)));
        return personAccounts;
    }
}
