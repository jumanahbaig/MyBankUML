package bank;

//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
//notes are on check files
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

public class Saving extends Account {
    public Saving(Customer customer) {
        super(customer);
    }

    // @Override
    // public void pay() {
    //     title();
    //     // TODO: implement interest calculation and withdrawal limits before processing payment.
    //     System.out.println("Payment From saving account For: " + customer.getName());
    // }

    // @Override
    // public void receipt() {
    //     // TODO: show updated balance and interest adjustments on the saving receipt.
    //     System.out.println("Payment receipt from saving account for: " + customer.getName());
    // }
}
