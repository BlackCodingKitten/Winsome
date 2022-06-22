import java.util.Date;

/*questa classe rappresenta una singola transazione che gli utenti di winsome vogliono fare */

public class WalletMovement {
    private final double movementAmount;// ->valore del movimento
    private final Date date;// ->data del movimento
    private final String movementReason;// -> causale del movimento
    // costruttore della classe WalletMovement

    public WalletMovement(String reason, double movementAmount) {
        this.date = new Date();
        this.movementReason = reason;
        this.movementAmount = movementAmount;

    }

    // metodo getter del valore della transazione
    public double getAmount() {
        return this.movementAmount;
    }

    // metodo getter della causale del movimento
    public String getReason() {
        return this.movementReason;
    }

    // metodo getter della data della transazione
    public Date getDate() {
        return this.date;
    }

    @Override
    public int hashCode() {
        String movement = this.date.toString() + String.valueOf(movementAmount) + movementReason;
        return movement.hashCode();
    }

}