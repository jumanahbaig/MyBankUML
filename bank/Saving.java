package bank;

public class Saving extends Account {
    public Saving(Customer customer) {
        super(customer);
    }
    public void title(){
        // TODO: customize payment headings with account numbers or plan descriptions.
        System.out.println("**Payments**");
    };

    @Override
    public void pay() {
        title();
        // TODO: implement interest calculation and withdrawal limits before processing payment.
        System.out.println("Payment From saving account For: " + customer.getName());
    }

    @Override
    public void receipt() {
        // TODO: show updated balance and interest adjustments on the saving receipt.
        System.out.println("Payment receipt from saving account for: " + customer.getName());
    }
}
