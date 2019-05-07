import sys
import Ice
import logging
import Bank

Ice.loadSlice('../slice/bank.ice')


class Logger():
    def __init__(self):
        self.logger = logging.getLogger(__name__)
        self.logger.setLevel(logging.INFO)
        self.handler = logging.StreamHandler()
        self.format = logging.Formatter(
            '%(asctime)s - %(name)s - %(levelname)s - %(message)s')
        self.handler.setFormatter(self.format)
        self.logger.addHandler(self.handler)

    def log_msg(self, msg):
        self.logger.log(self.logger.getEffectiveLevel(), msg)

    def error_msg(self, msg):
        self.logger.error(msg)

    def set_logger_lvl(self, level):
        log_level = logging.getLevelName(level)
        self.logger.setLevel(log_level)


LOG = Logger()

currencies = {
    'PLN': Bank.CurrencyType.PLN,
    'USD': Bank.CurrencyType.USD,
    'EUR': Bank.CurrencyType.EUR,
    'CHF': Bank.CurrencyType.CHF,
    'NOK': Bank.CurrencyType.NOK
}


def configure_connection(communicator):
    global bank_name, port, acc_factory, connection
    bank_name = input("Input bank NAME you want connect to\n")
    port = input("Input bank PORT you want connect to\n")
    connection = communicator.stringToProxy(
        "Bank/{0}:tcp -h localhost -p {1}:udp -h localhost -p {1}".format(bank_name, port))
    acc_factory = Bank.AccountFactoryPrx.checkedCast(connection)


def create_account():
    global account, password
    try:
        name, surname, pesel, income = input(
            "Input name, surname, pesel and your monthly income (Separate with ;)\n").split(";")
        person = Bank.Person(name, surname, pesel)
        account = acc_factory.createAccount(person, float(income))
        password = acc_factory.getPassword()
        LOG.log_msg("Your password is: " + password)
    except ValueError:
        LOG.error_msg("Couldn't unpack inserted string")
        sys.exit(1)
    except Ice.UnknownLocalException:
        LOG.error_msg("You are already registered. Try to log in using pesel")
    except Bank.InvalidPeselFormat:
        LOG.error_msg("Pesel should be made only with digits")
    except Exception:
        LOG.error_msg("Something went wrong. Sorry. Try again.")
        sys.exit(1)


def login_to_account(communicator):
    global account, password
    pesel = input("Provide your pesel\n")
    obj = communicator.stringToProxy(
        "Client/{0}:tcp -h localhost -p {1}:udp -h localhost -p {1}".format(pesel, port))
    account = Bank.AccountPrx.checkedCast(obj)

    if 'password' not in globals() or password is None:
        password = input("Enter your password: \n")

with Ice.initialize(sys.argv) as communicator:
    configure_connection(communicator)

    user_task = ''

    while user_task != 'exit':
        user_task = input(
            "Type 'create' to create account \n'login' to log into existing account\n'exit' to exit application\n")

        if user_task == 'create':
            create_account()
        elif user_task == 'login':
            login_to_account(communicator)
        elif user_task == 'exit':
            sys.exit(0)
        else:
            LOG.error_msg("Invalid command")

        if 'password' not in globals():  # Case when we try to create already existing user
            login_to_account(communicator)
            user_task = 'login'

        ctx = {"password": password}

        user_demand = ''
        while user_task == 'login' and user_demand != 'disconnect':
            try:
                user_demand = input(
                    "Available commands: balance, deposit, credit, disconnect\n")

                if user_demand == 'balance':
                    LOG.log_msg("Your account balance is " +
                                str(account.getAccountBalance(ctx)))
                elif user_demand == 'deposit':
                    money = float(input("How much you want to deposit?\n"))
                    account.deposit(money, ctx)
                elif user_demand == 'credit':
                    premium_acc = Bank.PremiumAccountPrx.uncheckedCast(account)

                    start_year, start_month, start_day = input(
                        "Please provide start year, month and day in format: yyyy-mm-dd\n").split("-")
                    end_year, end_month, end_day = input(
                        "Please provide end year, month and day in format: yyyy-mm-dd\n").split("-")
                    value, currency_type = input(
                        "Please provide credit value and currency in format value;currency\n").split(";")

                    start_date = Bank.Date(int(start_day), int(
                        start_month), int(start_year))
                    end_date = Bank.Date(
                        int(end_day), int(end_month), int(end_year))
                    credit = Bank.Credit(start_date, end_date, float(
                        value), currencies[currency_type])

                    credit_info = premium_acc.getCredit(credit, ctx)
                    LOG.log_msg("Credit info: " + str(credit_info))
                elif user_demand == 'disconnect':
                    LOG.log_msg("Quiting from this account\n")
                    password = None
                else:
                    LOG.error_msg("Invalid command\n")
            except Ice.OperationNotExistException:
                LOG.error_msg("Credits are only available for premium accounts\n")
            except Bank.UnsupportedCurrencyType:
                LOG.error_msg("Unsupported currency\n")
            except Bank.InvalidDateFormat:
                LOG.error_msg("Start date cannot be after end date\n")
            except Bank.InvalidPassword:
                LOG.error_msg("Bad password provided, please do it again")
                password = input("Enter your password:\n")
                ctx = {"password": password}
            except ValueError:
                LOG.error_msg("Couldn't unpack date values\n")
            except Exception:
                LOG.error_msg("Something went wrong. Sorry. Try again.\n")
                sys.exit(1)
