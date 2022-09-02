package info.kgeorgiy.ja.korolenko.bank;

import java.rmi.RemoteException;

public class RemotePerson implements Person {

    private final String name;
    private final String surname;
    private final String passportNumber;

    RemotePerson(final String name, final String surname, final String passportNumber){
        this.name = name;
        this.surname = surname;
        this.passportNumber = passportNumber;
    }

    @Override
    public String getName() throws RemoteException {
        return name;
    }

    @Override
    public String getSurname() throws RemoteException {
        return surname;
    }

    @Override
    public String getPassportNumber() throws RemoteException {
        return passportNumber;
    }
}
