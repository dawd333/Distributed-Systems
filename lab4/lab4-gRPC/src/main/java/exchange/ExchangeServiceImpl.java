package exchange;

import exchange.generated.Currency;
import exchange.generated.CurrencyProviderGrpc;
import exchange.generated.CurrencyType;
import exchange.generated.ExchangeRate;
import io.grpc.stub.StreamObserver;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class ExchangeServiceImpl extends CurrencyProviderGrpc.CurrencyProviderImplBase {
    private static final Logger logger = Logger.getLogger(ExchangeServiceImpl.class.getName());

    private final Map<CurrencyType, Double> exchangeRates = new HashMap<>();
    private final Map<StreamObserver<ExchangeRate>, List<CurrencyType>> bankCurrencies = new HashMap<>();

    private final int currencyChangeRatio = 7;
    private final int bankNotifyRatio = 5;

    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(2);

    public ExchangeServiceImpl() {
        exchangeRates.put(CurrencyType.PLN, 1.0);
        exchangeRates.put(CurrencyType.USD, 3.82);
        exchangeRates.put(CurrencyType.EUR, 4.28);
        exchangeRates.put(CurrencyType.CHF, 3.76);
        exchangeRates.put(CurrencyType.NOK, 0.44);

        scheduledExecutorService.scheduleAtFixedRate(this::currencyChange, 1, currencyChangeRatio, TimeUnit.SECONDS);
        scheduledExecutorService.scheduleAtFixedRate(this::notifyBanks, 1, bankNotifyRatio, TimeUnit.SECONDS);

    }

    @Override
    public void getExchangeRates(Currency request, StreamObserver<ExchangeRate> responseObserver){
        bankCurrencies.putIfAbsent(responseObserver, new ArrayList<>());
        bankCurrencies.get(responseObserver).addAll(request.getCurrencyList());
    }

    private void currencyChange(){
        for (CurrencyType currencyType : exchangeRates.keySet()) {
            if(ThreadLocalRandom.current().nextInt(2) == 0){
                double newCurrencyValue = exchangeRates.get(currencyType) * ThreadLocalRandom.current().nextDouble(0.95, 1.1);
                exchangeRates.put(currencyType, round(newCurrencyValue, 2));
                logger.info("Currency " + currencyType + " changed");
            }
        }
    }

    private void notifyBanks(){
        bankCurrencies.forEach((responseObserver, currencyTypes) -> currencyTypes
                .forEach(currencyType -> currencyNotify(responseObserver, currencyType)));
        logger.info("Banks notified");
    }

    private void currencyNotify(StreamObserver<ExchangeRate> responseObserver, CurrencyType currencyType) {
        ExchangeRate exchangeRate = ExchangeRate.newBuilder()
                .setCurrency(currencyType)
                .setRate(exchangeRates.get(currencyType))
                .build();
        responseObserver.onNext(exchangeRate);
    }

    private static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
