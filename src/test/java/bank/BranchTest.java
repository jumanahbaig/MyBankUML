package bank;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BranchTest {

    private Logs createTempLogs() {
        try {
            Path logPath = Files.createTempFile("branch-test-log-", ".txt");
            logPath.toFile().deleteOnExit();
            return new Logs(logPath.toString());
        } catch (IOException e) {
            throw new RuntimeException("Failed to create temp log file for tests", e);
        }
    }

    @Test
    void loginRegistersActiveUser() {
        Logs logs = createTempLogs();
        Bank bank = new Bank("Test Bank");
        Branch branch = bank.createNewBranch("Test Branch", "555-0000", logs);

        Customer customer = new Customer("Sam", "Test", "samtest", "pass123");

        branch.userLogin(customer);

        assertEquals(customer, branch.getActiveUser());
    }

    @Test
    void logoutClearsActiveUser() {
        Logs logs = createTempLogs();
        Bank bank = new Bank("Test Bank");
        Branch branch = bank.createNewBranch("Test Branch", "555-0000", logs);
        Customer customer = new Customer("Sam", "Test", "samtest", "pass123");

        branch.userLogin(customer);
        branch.userLogout(customer);

        assertNull(branch.getActiveUser());
    }

    @Test
    void getBranchInfoContainsKeyDetails() {
        Logs logs = createTempLogs();
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
        Logs logs = createTempLogs();
        Bank bank = new Bank("Test Bank");

        Branch branch = bank.createNewBranch("Test Branch", "555-0000", logs);

        assertTrue(bank.getBranches().contains(branch));
        assertEquals("Test Branch", branch.getAddress());
        assertEquals("555-0000", branch.getPhoneNumber());
    }
}
