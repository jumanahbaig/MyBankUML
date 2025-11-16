package bank;

public class Teller extends Employee {

    public Teller(String firstName, String lastName, String userName, String password,
                  AccountRepository accountRepository, UserRepository userRepository) {
        super(firstName, lastName, userName, password, accountRepository, userRepository);
    }

    public Teller(String firstName, String lastName, String userName, String password,
                  AccountRepository accountRepository) {
        this(firstName, lastName, userName, password, accountRepository, null);
    }

    public Teller(String firstName, String lastName, String userName, String password) {
        this(firstName, lastName, userName, password, null, null);
    }

    public void createNewUser(Customer newCustomer) {
        System.out.println("Teller " + getUserName() + " is collecting customer information.");
        // TODO: coordinate with other tellers/admins to avoid duplicate customer entries when onboarding in parallel.
        onboardCustomer(newCustomer);
    }

    @Override
    public void changePassword(String newPassword) {
        // TODO: implement teller password updates with validation/audit logging.
    }

    @Override
    protected String getRoleLabel() {
        return "Teller";
    }

    @Override
    public void requestForgottenPassword() {
        // TODO: send forgotten password notification to administrators for approval.
    }
}
