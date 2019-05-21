package bank;

import bank.generated.Bank.*;
import com.zeroc.Ice.Current;
import com.zeroc.Ice.Identity;
import exchange.generated.CurrencyType;


import java.util.HashMap;
import java.util.Random;
import java.util.logging.Logger;

public class AccountFactoryImpl implements AccountFactory {
    private static final Logger logger = Logger.getLogger(AccountFactoryImpl.class.getName());
    private final double normalAccountLimit = 8000.0;

    private String password;

    private HashMap<CurrencyType, Double> exchangeRates;

    public AccountFactoryImpl(HashMap<CurrencyType, Double> exchangeRates){
        this.exchangeRates = exchangeRates;
    }

    @Override
    public AccountPrx createAccount(Person person, double income, Current current) throws InvalidPeselFormat{
        validatePesel(person.pesel);
        logger.info("New user created with ID: " + person.pesel);
        password = "ala123" + new Random().nextInt(5);
        if(income > normalAccountLimit){
            return PremiumAccountPrx.uncheckedCast(current.adapter.add(new PremiumAccountImpl(person, income, password, exchangeRates), new Identity(person.pesel, "Client")));
        }
        return AccountPrx.uncheckedCast(current.adapter.add(new AccountImpl(person, income, password), new Identity(person.pesel, "Client")));
    }

    @Override
    public String getPassword(Current current){
        return this.password;
    }

    private void validatePesel(String pesel) throws InvalidPeselFormat{
        if(!pesel.matches("\\d+")) throw new InvalidPeselFormat("Pesel should be made only with digits");
    }
}
