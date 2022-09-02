package info.kgeorgiy.ja.korolenko.bank;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public final class Client {
    /**
     * Utility class.
     */
    private Client() {
    }

    public static void main(final String... args) throws RemoteException {
        if (args == null || args.length != 5) {
            System.err.println("Wrong arguments");
            return;
        }
        final Bank bank;
        try {
            bank = (Bank) Naming.lookup("//localhost/bank");
        } catch (final NotBoundException e) {
            System.out.println("Bank is not bound");
            return;
        } catch (final MalformedURLException e) {
            System.out.println("Bank URL is invalid");
            return;
        }

        final String name = args[0];
        final String surname = args[1];
        final String passportNumber = args[2];
        final String accountId = args[3];
        int change = 0;
        try {
            change = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            System.err.println(e);
        }


        Person person = bank.getPerson(passportNumber, TypeObject.REMOTE);
        if (person == null) {
            System.out.println("Creating person");
            person = bank.createPerson(name, surname, passportNumber);
        } else {
            System.out.println("Person already exists");
        }

        Account account = bank.getAccount(person, accountId, TypeObject.REMOTE);
        if (account == null) {
            System.out.println("Creating account");
            account = bank.createAccount(person, accountId);
        } else {
            System.out.println("Account already exists");
        }
        System.out.println("Account id: " + account.getId());
        System.out.println("Money: " + account.getAmount());
        System.out.println("Adding money");
        account.setAmount(account.getAmount() + change);
        System.out.println("Money: " + account.getAmount());
    }
    }
