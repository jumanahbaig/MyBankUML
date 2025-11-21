package bank;

//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
//I Dont see a need for additional fields or methods specific to Check accounts at this time,
//  nore the other subclasses, so there are just distinct account types for ease of access later.
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

public class Check extends Account {
    public Check(Customer customer) {
        super(customer);

    }
    
    

    // @Override
    // public void pay() {
    //     //check title
    //     title();
    //     // TODO: support writing to an external check ledger or printing physical checks.
    //     System.out.println("Check payment for customer: " + customer.getName());
    // }

    // @Override
    // public void receipt() {
    //     // TODO: include check number and signature data on printed/digital receipts.
    //     System.out.println("Check receipt for customer: " + customer.getName());
    // }
}
