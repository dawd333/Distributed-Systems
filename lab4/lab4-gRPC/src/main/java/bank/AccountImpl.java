package bank;

import bank.generated.Bank.Account;
import bank.generated.Bank.Person;
import com.zeroc.Ice.Current;

import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

public class AccountImpl implements Account {
    private static final Logger logger = Logger.getLogger(AccountImpl.class.getName());

    private Person person;
    private double income;
    private double balance;

    public AccountImpl(Person person, double income){
        this.person = person;
        this.income = income;
        this.balance = (double) ThreadLocalRandom.current().nextInt(1000, 10000);
    }

    @Override
    public double getAccountBalance(Current current){
        logger.info("Account " + current.id + " balance is " + balance);
        return balance;
    }

    @Override
    public void deposit(double money, Current current){
        logger.info("Account " + current.id + " deposit " + money);
        balance+=money;
    }
}
