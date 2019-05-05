package bank;

import com.zeroc.Ice.Communicator;
import com.zeroc.Ice.Identity;
import com.zeroc.Ice.ObjectAdapter;
import com.zeroc.Ice.Util;
import exchange.generated.Currency;
import exchange.generated.CurrencyProviderGrpc;
import exchange.generated.CurrencyType;
import exchange.generated.ExchangeRate;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class Bank {
    private static final Logger logger = Logger.getLogger(Bank.class.getName());
    private final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

    private final ManagedChannel channel;
    private final CurrencyProviderGrpc.CurrencyProviderStub currencyProviderStub;

    private final HashMap<CurrencyType, Double> exchangeRates = new HashMap<>();
    private String bankName;
    private final String bankPort = "6789";

    public Bank (String host, int port){
        channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext(true)
                .build();

        currencyProviderStub = CurrencyProviderGrpc.newStub(channel);
    }

    private void start() throws InterruptedException {
        System.out.println("Enter bank name");
        Communicator communicator = null;
        try {
            bankName = br.readLine();
            System.out.println("Type currencies which interest you (PLN, USD, EUR, CHF, NOK), separate with ;");
            String currencies = br.readLine();
            Arrays.stream(currencies.split(";"))
                    .forEach(currency -> exchangeRates.put(CurrencyType.valueOf(currency), 1.0));

            subscribeToObserver();

            communicator = Util.initialize();
            ObjectAdapter objectAdapter = communicator.createObjectAdapterWithEndpoints("Adapter",
                    "tcp -h localhost -p " + bankPort + ":udp -h localhost -p " + bankPort);

            AccountFactoryImpl accountFactoryServant = new AccountFactoryImpl(exchangeRates);
            objectAdapter.add(accountFactoryServant, new Identity(bankName, "Bank"));
            objectAdapter.activate();
            logger.info("Bank created, name " + bankName + " bank port: " + bankPort);
            communicator.waitForShutdown();
        } catch (IOException e){
            logger.warning("Something went wrong, try again");
            System.exit(1);
        } catch (IllegalArgumentException e){
            logger.warning("Wrong currency shortcut");
            System.exit(1);
        } catch (Exception e){
            logger.warning(e.getMessage());
            System.exit(1);
        } finally {
            if(communicator != null){
                try {
                    communicator.destroy();
                } catch (Exception e){
                    logger.warning(e.getMessage());
                    System.exit(1);
                }
            }
            shutdown();
        }
    }

    private void shutdown() throws InterruptedException{
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    private void subscribeToObserver(){
        currencyProviderStub.getExchangeRates(Currency.newBuilder().addAllCurrency(exchangeRates.keySet()).build(),
                new StreamObserver<ExchangeRate>() {
                    @Override
                    public void onNext(ExchangeRate value) {
                        exchangeRates.replace(value.getCurrency(), value.getRate());
                        logger.info(value.getCurrency() + " value: " + value.getRate());
                    }

                    @Override
                    public void onError(Throwable t) {
                        logger.warning("Error in bank ExchangeRates " + t.getMessage());
                    }

                    @Override
                    public void onCompleted() {
                        logger.info("Streaming ExchangeRates completed");
                    }
                }
        );
    }

    public static void main(String[] args) throws InterruptedException {
        Bank bank = new Bank("localhost", 50051);
        bank.start();
    }
}
