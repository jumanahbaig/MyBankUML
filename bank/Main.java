package bank;

public class Main {
    /**
     * Test harness that walks through multiple customer stories:
     *  1. Customer 1 opens card + saving accounts.
     *  2. Customer 2 opens card + saving via different teller/admin.
     *  3. Customer 3 opens saving (teller) + card (admin).
     *  4. Customer 4 opens saving then card with teller/admin #2.
     *  5. Customer 5 demonstrates multiple savings/cards (duplicates allowed).
     *  6. Customer 6 attempts to open another checking account (should be rejected).
     *
     * Along the way we also show staff persistence and end with classic transaction tests.
     */
    public static void main(String[] args) {
        // === Initialize SQLite-backed infrastructure ===
        DatabaseManager databaseManager = new DatabaseManager();
        UserRepository userRepository = new UserRepository(databaseManager);
        AccountRepository accountRepository = new AccountRepository(databaseManager);
        CustomerService customerService = new CustomerService(userRepository, accountRepository);

        // === Customers automatically receive checking accounts upon creation ===
        Customer customer = customerService.createCustomer("Shayan", "Aminaei", "saminaei", "password123");
        Customer customerTwo = customerService.createCustomer("Maria", "Lopez", "mlopez", "savingsPass");
        Customer customerThree = customerService.createCustomer("David", "Nguyen", "dnguyen", "securePass");
        Customer customerFour = customerService.createCustomer("Emma", "Jones", "ejones", "hybridPass");
        Customer customerFive = customerService.createCustomer("Omar", "Singh", "osingh", "multiPass");
        Customer customerSix = customerService.createCustomer("Noah", "Brown", "nbrown", "checkBlock");
        customer.printCustomerInfo();
        System.out.println();

        // === Bank staff (tellers/admins) persisted like regular users ===
        Teller tellerJamie = new Teller("Jamie", "Lee", "jlee", "tellerPass", accountRepository, userRepository);
        Teller tellerPriya = new Teller("Priya", "Kumar", "pkumar", "tellerPass2", accountRepository, userRepository);
        userRepository.addUsers(tellerJamie);
        userRepository.addUsers(tellerPriya);

        Admin adminAlex = new Admin("Alex", "Morgan", "amorgan", "adminPass", accountRepository, userRepository);
        Admin adminLina = new Admin("Lina", "Chen", "lchen", "adminPass2", accountRepository, userRepository);
        userRepository.addUsers(adminAlex);
        userRepository.addUsers(adminLina);

        // === Test 1: first customer opens card and saving accounts ===
        Card card = new Card(customer);
        Account check = customer.getCheckingAccount();
        Saving saving = new Saving(customer);
        customer.requestNewAccount(card);
        tellerJamie.acceptNewAccount(card);
        customer.requestNewAccount(saving);
        adminAlex.acceptNewAccount(saving);

        // === Test 2: second customer opens card + saving via teller Priya / admin Lina ===
        Card cardTwo = new Card(customerTwo);
        Saving savingTwo = new Saving(customerTwo);
        customerTwo.requestNewAccount(cardTwo);
        tellerPriya.acceptNewAccount(cardTwo);
        customerTwo.requestNewAccount(savingTwo);
        adminLina.acceptNewAccount(savingTwo);

        // === Test 3: third customer saving (teller Jamie) followed by card (admin Alex) ===
        Saving savingThree = new Saving(customerThree);
        customerThree.requestNewAccount(savingThree);
        tellerJamie.acceptNewAccount(savingThree);
        Card cardThree = new Card(customerThree);
        customerThree.requestNewAccount(cardThree);
        adminAlex.acceptNewAccount(cardThree);

        // === Test 4: fourth customer uses teller Priya / admin Lina for saving + card ===
        Saving savingFour = new Saving(customerFour);
        Card cardFour = new Card(customerFour);
        customerFour.requestNewAccount(savingFour);
        tellerPriya.acceptNewAccount(savingFour);
        customerFour.requestNewAccount(cardFour);
        adminLina.acceptNewAccount(cardFour);

        // === Test 5: fifth customer demonstrates duplicate savings/cards (allowed) via Jamie/Alex ===
        Saving savingFiveOne = new Saving(customerFive);
        Saving savingFiveTwo = new Saving(customerFive);
        Card cardFiveOne = new Card(customerFive);
        Card cardFiveTwo = new Card(customerFive);
        customerFive.requestNewAccount(savingFiveOne);
        tellerJamie.acceptNewAccount(savingFiveOne);
        customerFive.requestNewAccount(savingFiveTwo);
        tellerJamie.acceptNewAccount(savingFiveTwo);
        customerFive.requestNewAccount(cardFiveOne);
        adminAlex.acceptNewAccount(cardFiveOne);
        customerFive.requestNewAccount(cardFiveTwo);
        adminAlex.acceptNewAccount(cardFiveTwo);

        // === Test 6: sixth customer attempts another checking account (should be rejected) ===
        Account extraCheck = new Check(customerSix);
        customerSix.requestNewAccount(extraCheck);
        tellerJamie.acceptNewAccount(extraCheck);

        // === Test 7: Admin modifies another admin's role and verifies permissions ===
        adminAlex.editRole(adminLina); // demote Lina to teller
        adminLina.editRole(tellerJamie); // should fail because Lina is no longer an admin

        // TODO: Test 8 - simulate concurrent account creation/user onboarding using threads/executors.

        // === Legacy transaction demo for the first customer's accounts ===
        Transaction t1 = new Transaction();
        Transaction t2 = new Transaction();
        Transaction t3 = new Transaction();

        card.addTransaction(t1);
        check.addTransaction(t2);
        saving.addTransaction(t3);

        // Transactions
        card.pay();
        card.receipt();
        System.out.println();

        check.pay();
        check.receipt();
        System.out.println();

        saving.pay();
        saving.receipt();
        System.out.println();

        // === Bank/branch printing demo ===
        Bank bank = new Bank("National Bank");
        Branch branch1 = new Branch("Branch no1 ", bank);
        Branch branch2 = new Branch("Branch no2 ", bank);

        bank.printBankInfo();
        System.out.println();

        // === Transaction summary counts ===
        System.out.println("Card   transactions count:   " + card.getTransactions().size());
        System.out.println("Check  transactions count:   " + check.getTransactions().size());
        System.out.println("Saving transactions count:   " + saving.getTransactions().size());
    }
}
