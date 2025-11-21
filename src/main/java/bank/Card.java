package bank;

//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
//notes are on check files
//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
public class Card extends Account {
    public Card(Customer customer) {
        super(customer);
    }



    // @Override
    // public void pay() {
    //     // TODO: integrate with payment gateway and handle declined transactions.
    //     System.out.println("Card payment for: " + customer.getName());
    // }

    // @Override
    // public void receipt() {
    //     // TODO: capture card transaction details (merchant, amount, timestamp) for the receipt.
    //     System.out.println("Card receipt for: " + customer.getName());
    // }
}
