package bank;

public class Check extends Account {
    public Check(Customer customer) {
        super(customer);

    }
    public void title(){
        // TODO: localize title formatting based on branch or language preferences.
        System.out.println("**Check Title**");
    }

    @Override
    public void pay() {
        //check title
        title();
        // TODO: support writing to an external check ledger or printing physical checks.
        System.out.println("Check payment for customer: " + customer.getName());
    }

    @Override
    public void receipt() {
        // TODO: include check number and signature data on printed/digital receipts.
        System.out.println("Check receipt for customer: " + customer.getName());
    }
}
