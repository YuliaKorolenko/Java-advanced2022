package info.kgeorgiy.ja.korolenko.bank;

import java.io.Serializable;

public class LocalPerson implements Person, Serializable {

    private final String name;
    private final String surname;
    private final String passportNumber;

    LocalPerson(final String name, final String surname, final String passportNumber){
        this.name = name;
        this.surname = surname;
        this.passportNumber = passportNumber;
    }

    @Override
    public String getName(){
        return name;
    }

    @Override
    public String getSurname(){
        return surname;
    }

    @Override
    public String getPassportNumber(){
        return passportNumber;
    }

}
