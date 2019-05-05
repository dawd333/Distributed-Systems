package bank;

import bank.generated.Bank.*;
import com.zeroc.Ice.Current;
import com.zeroc.Ice.Identity;
import exchange.generated.CurrencyType;

import java.util.HashMap;
import java.util.logging.Logger;

public class AccountFactoryImpl implements AccountFactory {
    private static final Logger logger = Logger.getLogger(AccountFactoryImpl.class.getName());
    private final double normalAccountLimit = 8000.0;

    private HashMap<CurrencyType, Double> exchangeRates;

    public AccountFactoryImpl(HashMap<CurrencyType, Double> exchangeRates){
        this.exchangeRates = exchangeRates;
    }

    //ObjectAdapter map object identity to servant
    //Prx, because we return interface. Pointers are similar to pointers from cpp.
    @Override
    public AccountPrx createAccount(Person person, double income, Current current) throws InvalidPeselFormat{
        validatePesel(person.pesel);
        logger.info("New user created with ID: " + person.pesel);
        if(income > normalAccountLimit){
            return PremiumAccountPrx.uncheckedCast(current.adapter.add(new PremiumAccountImpl(person, income, exchangeRates), new Identity(person.pesel, "Client")));
        }
        return AccountPrx.uncheckedCast(current.adapter.add(new AccountImpl(person, income), new Identity(person.pesel, "Client")));
    }

    private void validatePesel(String pesel) throws InvalidPeselFormat{
        if(!pesel.matches("\\d+")) throw new InvalidPeselFormat("Pesel should be made only with digits");
    }
}
