package bank;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BranchTest {

    @Test
    void loginRegistersActiveUser() {
        Logs logs = new Logs("test-branch-log.txt");
        Bank bank = new Bank("Test Bank");
        Branch branch = bank.createNewBranch("Test Branch", "555-0000", logs);

        Customer customer = new Customer("Sam", "Test", "samtest", "pass123");

        branch.userLogin(customer);

        assertEquals(customer, branch.getActiveUser());
    }

    @Test
    void logoutClearsActiveUser() {
        Logs logs = new Logs("test-branch-log.txt");
        Bank bank = new Bank("Test Bank");
        Branch branch = bank.createNewBranch("Test Branch", "555-0000", logs);
        Customer customer = new Customer("Sam", "Test", "samtest", "pass123");

        branch.userLogin(customer);
        branch.userLogout(customer);

        assertNull(branch.getActiveUser());
    }

    @Test
    void getBranchInfoContainsKeyDetails() {
        Logs logs = new Logs("test-branch-log.txt");
        Bank bank = new Bank("Test Bank");
        Branch branch = bank.createNewBranch("Test Branch", "555-0000", logs);
        Customer customer = new Customer("Sam", "Test", "samtest", "pass123");

        branch.userLogin(customer);
        String info = branch.getBranchInfo();

        assertTrue(info.contains("Test Branch"));
        assertTrue(info.contains("555-0000"));
        assertTrue(info.contains("Test Bank"));
        assertTrue(info.contains("samtest"));
    }

    @Test
    void createNewBranchAddsBranchToBank() {
        Logs logs = new Logs("test-branch-log.txt");
        Bank bank = new Bank("Test Bank");

        Branch branch = bank.createNewBranch("Test Branch", "555-0000", logs);

        assertTrue(bank.getBranches().contains(branch));
        assertEquals("Test Branch", branch.getAddress());
        assertEquals("555-0000", branch.getPhoneNumber());
    }
}
