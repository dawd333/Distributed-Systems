package bank;

import bank.generated.Bank.Account;
import bank.generated.Bank.InvalidPassword;
import bank.generated.Bank.Person;
import com.zeroc.Ice.Current;

import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

public class AccountImpl implements Account {
    private static final Logger logger = Logger.getLogger(AccountImpl.class.getName());

    private Person person;
    private double income;
    private double balance;
    private String password;

    public AccountImpl(Person person, double income, String password){
        this.person = person;
        this.income = income;
        this.balance = (double) ThreadLocalRandom.current().nextInt(1000, 10000);
        this.password = password;
    }

    @Override
    public double getAccountBalance(Current current) throws InvalidPassword {
        checkPassword(current.ctx.get("password"));
        logger.info("Account " + current.id + " balance is " + balance);
        return balance;
    }

    @Override
    public void deposit(double money, Current current) throws InvalidPassword {
        checkPassword(current.ctx.get("password"));
        logger.info("Account " + current.id + " deposit " + money);
        balance+=money;
    }

    void checkPassword(String password) throws InvalidPassword {
        if(!this.password.equals(password)){
            logger.warning("Wrong password provided");
            throw new InvalidPassword("Wrong password provided");
        }
    }
}
