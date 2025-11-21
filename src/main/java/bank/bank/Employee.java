package bank;

/**
 * Represents shared behavior between bank employees such as tellers and admins.
 */
public abstract class Employee extends User {
    protected final AccountRepository accountRepository;
    protected final UserRepository userRepository;

    protected Employee(String firstName, String lastName, String userName, String password,
                       AccountRepository accountRepository, UserRepository userRepository) {
        super(firstName, lastName, userName, password);
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    protected Employee(String firstName, String lastName, String userName, String password) {
        this(firstName, lastName, userName, password, null, null);
    }

    public void acceptNewAccount(Account accountType) {
        System.out.println(getUserName() + " accepting new account: " + accountType.getClass().getSimpleName());
        Customer owner = accountType.getCustomer();
        if (owner == null) {
            System.out.println(getUserName() + " cannot accept account without an owner.");
            return;
        }
        if (owner.addAccount(accountType)) {
            System.out.println(getUserName() + " approved account for customer " + owner.getUserName());
            if (accountRepository != null) {
                accountRepository.addAccount(accountType);
            } else {
                System.out.println(getUserName() + " could not persist the account (repository unavailable).");
            }
        }
    }

    protected void onboardCustomer(Customer newCustomer) {
        if (userRepository == null || accountRepository == null) {
            System.out.println("Cannot create customer; repositories unavailable for " + getRoleLabel()
                    + " " + getUserName());
            return;
        }
        userRepository.addUsers(newCustomer);
        accountRepository.addAccount(newCustomer.getCheckingAccount());
        System.out.println(getRoleLabel() + " " + getUserName()
                + " onboarded customer " + newCustomer.getUserName());
    }

    protected abstract String getRoleLabel();

    @Override
    public void printUserInfo() {
        super.printUserInfo();
        System.out.println("Role: " + getRoleLabel());
    }

    @Override
    public void changePassword(String newPassword) {
        super.changePassword(newPassword);
    }
}
