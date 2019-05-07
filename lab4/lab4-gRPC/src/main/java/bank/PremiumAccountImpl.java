package bank;

import bank.generated.Bank.*;
import com.zeroc.Ice.Current;
import exchange.generated.CurrencyType;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class PremiumAccountImpl extends AccountImpl implements PremiumAccount {
    private static final Logger logger = Logger.getLogger(PremiumAccountImpl.class.getName());

    private Map<CurrencyType, Double> exchangeRates;
    private final double creditMultiplier = 1.1;

    public PremiumAccountImpl(Person person, double income, String password, HashMap<CurrencyType, Double> exchangeRates){
        super(person, income, password);
        this.exchangeRates = exchangeRates;
    }

    @Override
    public CreditInfo getCredit(Credit credit, Current current) throws InvalidDateFormat, UnsupportedCurrencyType, InvalidPassword {
        checkPassword(current.ctx.get("password"));
        validateDate(credit.startDate, credit.endDate);

        double creditCost = getCreditValueInForeignCurrency(credit.value * creditMultiplier, credit.currencyType);
        logger.info("Client " + current.id + " took credit " + creditCost + " in " + credit.currencyType);
        return new CreditInfo(bank.generated.Bank.CurrencyType.PLN, credit.currencyType, credit.value * creditMultiplier, creditCost);
    }

    private void validateDate(Date start, Date end) throws InvalidDateFormat {
        if (start.year > end.year || (start.year == end.year && start.month > end.month) ||
                (start.year == end.year && start.month == end.month && start.day > end.day)){
            throw new InvalidDateFormat("Start date has to be before end date :)");
        }
    }

    private double getCreditValueInForeignCurrency(double value, bank.generated.Bank.CurrencyType currencyType) throws UnsupportedCurrencyType{
        Double rate = exchangeRates.get(convertCurrencyTypes(currencyType));
        Double baseRate = exchangeRates.get(CurrencyType.PLN);

        if (rate == null) throw new UnsupportedCurrencyType("Currency not supported");
        return value * baseRate / rate;
    }

    private CurrencyType convertCurrencyTypes(bank.generated.Bank.CurrencyType currencyType) throws UnsupportedCurrencyType{
        switch(currencyType){
            case PLN:
                return CurrencyType.PLN;
            case USD:
                return CurrencyType.USD;
            case EUR:
                return CurrencyType.EUR;
            case CHF:
                return CurrencyType.CHF;
            case NOK:
                return CurrencyType.NOK;
                default:
                    throw new UnsupportedCurrencyType("Currency not supported");

        }
    }
}
