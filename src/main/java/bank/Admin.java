package bank;

public class Admin extends Employee {

    public Admin(String firstName, String lastName, String userName, String password,
                 AccountRepository accountRepository, UserRepository userRepository) {
        super(firstName, lastName, userName, password, accountRepository, userRepository);
    }

    public Admin(String firstName, String lastName, String userName, String password,
                 UserRepository userRepository) {
        this(firstName, lastName, userName, password, null, userRepository);
    }

    public Admin(String firstName, String lastName, String userName, String password) {
        this(firstName, lastName, userName, password, null, null);
    }

    public void editRole(User userToModify) {
        if (userRepository == null) {
            System.out.println("User repository unavailable; cannot persist role changes.");
            return;
        }

        String currentRole = userRepository.findRoleByUsername(getUserName());
        if (currentRole == null || !"ADMIN".equalsIgnoreCase(currentRole)) {
            System.out.println("User " + getUserName() + " no longer has admin privileges.");
            return;
        }

        if (userToModify instanceof Customer) {
            System.out.println("Cannot modify role for customers: " + userToModify.getUserName());
            return;
        }

        if (userToModify instanceof Teller) {
            System.out.println("Promoting teller " + userToModify.getUserName() + " to admin.");
            Admin promoted = new Admin(
                    userToModify.getFirstName(),
                    userToModify.getLastName(),
                    userToModify.getUserName(),
                    userToModify.getPassword(),
                    accountRepository,
                    userRepository);
            userRepository.addUsers(promoted);
            System.out.println("Role updated in database for " + promoted.getUserName());
            return;
        }

        if (userToModify instanceof Admin) {
            System.out.println("Demoting admin " + userToModify.getUserName() + " to teller.");
            Teller demoted = new Teller(
                    userToModify.getFirstName(),
                    userToModify.getLastName(),
                    userToModify.getUserName(),
                    userToModify.getPassword(),
                    accountRepository,
                    userRepository);
            userRepository.addUsers(demoted);
            System.out.println("Role updated in database for " + demoted.getUserName());
            return;
        }

        System.out.println("Unknown user type; no role changes performed for " + userToModify.getUserName());
    }

    @Override
    public void acceptNewAccount(Account accountType) {
        System.out.println("Admin handling account request...");
        super.acceptNewAccount(accountType);
    }

    public void createNewUser(User newUser) {
        if (userRepository == null) {
            System.out.println("Cannot create user; repositories unavailable for admin " + getUserName());
            return;
        }

        if (newUser instanceof Customer) {
            onboardCustomer((Customer) newUser);
            return;
        }

        if (newUser instanceof Teller || newUser instanceof Admin) {
            // TODO: ensure concurrent admin/teller creation does not overwrite each other's data (locking).
            userRepository.addUsers(newUser);
            System.out.println("Admin " + getUserName() + " onboarded staff member " + newUser.getUserName());
            return;
        }

        System.out.println("Unsupported user type: " + newUser.getClass().getSimpleName());
    }

    @Override
    public void printUserInfo() {
        super.printUserInfo();
    }

    @Override
    protected String getRoleLabel() {
        return "Admin";
    }

    @Override
    public void changePassword(String newPassword) {
        // TODO: implement admin-level password change with approval workflow.
    }

    @Override
    public void requestForgottenPassword() {
        // TODO: log admin request and require peer approval before resetting credentials.
    }

    public void acceptForgottenPasswordRequest(User user) {
        if (userRepository == null) {
            System.out.println("Cannot reset password; user repository unavailable for admin " + getUserName());
            return;
        }
        // TODO: generate secure temporary password, notify the user, and persist the new credential.
        System.out.println("Admin " + getUserName() + " approved forgotten password request for " + user.getUserName());
    }
}
