package info.kgeorgiy.ja.korolenko.bank;

import org.junit.Assert;
import org.junit.BeforeClass;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Test {

    private static Bank bank;


    @BeforeClass
    public static void beforeTests() {
        final int BANK_PORT = 8888;
        bank = new RemoteBank(BANK_PORT);
        try {
            UnicastRemoteObject.exportObject(bank, BANK_PORT);
            Naming.rebind("//localhost/bank", bank);
        } catch (final RemoteException e) {
            System.out.println("Cannot export object: " + e.getMessage());
        } catch (final MalformedURLException e) {
            System.out.println("Malformed URL");
        }
    }


    // Check one client
    @org.junit.Test
    public void test_01_singlePerson() throws RemoteException {
        final String passportId = "9876";
        final Person person = bank.createPerson("Yulia", "Korolenko", passportId);
        final Person person2 = bank.createPerson("Yul", "Korolenko", passportId);
        final Person person1 = bank.getPerson(passportId, TypeObject.REMOTE);
        Assert.assertEquals("Get another person", person, person1);
        Assert.assertEquals("Get another person", person2, person);
    }

    // Check one account
    @org.junit.Test
    public void test_01_singleAccount() throws RemoteException {
        final Person person = bank.getPerson("9876", TypeObject.REMOTE);
        final Map<String, ? extends Account> accounts = bank.getPersonAccounts(person, TypeObject.REMOTE);
        Assert.assertTrue("Accounts not empty", accounts.isEmpty());
        final Account account = bank.createAccount(person, "33");
        Assert.assertEquals("Accounts not equal", account, bank.getAccount(person, "33", TypeObject.REMOTE));
    }

    //создает несколько клиентов
    @org.junit.Test
    public void test_03_quantityRemotePerson() throws RemoteException {
        for (int i = 0; i < 10; i++) {
            bank.createPerson("name" + i, "surname" + i, String.valueOf(i));
        }
        for (int i = 0; i < 10; i++) {
            final Person remotePerson = bank.getPerson(String.valueOf(i), TypeObject.REMOTE);
            final Person localPerson = bank.getPerson(String.valueOf(i), TypeObject.LOCAL);

            Assert.assertNotEquals(remotePerson, localPerson);

            Assert.assertEquals(remotePerson.getName(), "name" + i);
            Assert.assertEquals(remotePerson.getSurname(), "surname" + i);
            Assert.assertEquals(remotePerson.getPassportNumber(), String.valueOf(i));

            Assert.assertEquals(localPerson.getName(), "name" + i);
            Assert.assertEquals(localPerson.getSurname(), "surname" + i);
            Assert.assertEquals(localPerson.getPassportNumber(), String.valueOf(i));
        }
    }

    //создает несколько аккаунтов, при чем один аккаунт создается 2 раза и у одного клиента создается несколько аккаунтов
    @org.junit.Test
    public void test_04_quantityAccounts() throws RemoteException {
        final Person person = bank.createPerson("test4name", "test4surname", "test4");
        for (int i = 0; i < 5; i++) {
            bank.createAccount(person, String.valueOf(i));

            final Account remoteAccount = bank.getAccount(person, String.valueOf(i), TypeObject.REMOTE);
            final Account localAccount = bank.getAccount(person, String.valueOf(i), TypeObject.LOCAL);
            Assert.assertNotEquals(remoteAccount, localAccount);

            Assert.assertEquals(remoteAccount.getAmount(), 0);
            Assert.assertEquals(remoteAccount.getId(), person.getPassportNumber() + ":" + i);
            Assert.assertEquals(remoteAccount.setAmount(100), bank.getAccount(person, String.valueOf(i), TypeObject.REMOTE).getAmount());

            Assert.assertEquals(localAccount.getAmount(), 0);
            Assert.assertEquals(localAccount.getId(), person.getPassportNumber() + ":" + i);
            Assert.assertNotEquals(localAccount.setAmount(300), bank.getAccount(person, String.valueOf(i), TypeObject.REMOTE).getAmount());
        }
        final Map<String, ? extends Account> accounts = bank.getPersonAccounts(person, TypeObject.REMOTE);
        Assert.assertEquals("5 accounts expected, current : " + accounts.size(), 5, accounts.size());
    }

    //многопочка
    @org.junit.Test
    public void test_05_multithreading() throws RemoteException {
        final Person person = bank.createPerson("test5name", "test5surname", "test5");
        final Account account = bank.createAccount(person, "12345");
        final List<Thread> threadsList = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            final int finalI = i;
            final Thread thread = new Thread(() -> {
                try {
                    bank.createAccount(person, "test_5" + finalI);
                    account.addAmount(10);
                } catch (final RemoteException e) {
                    e.printStackTrace();
                }
            });
            thread.start();
            threadsList.add(thread);
        }
        for (final Thread thread : threadsList) {
            try {
                thread.join();
            } catch (final InterruptedException e) {
                System.err.println(e.getMessage());
            }
        }
        final Map<String, ? extends Account> accounts = bank.getPersonAccounts(person, TypeObject.REMOTE);
        Assert.assertEquals(51, accounts.size());
        Assert.assertEquals(500, account.getAmount());
    }


}
