import java.io.Serializable;

// CO2: ADT (Abstract Data Type) - Expense models a single financial record as an ADT
// CO5: Linear Data Structure element - Expense objects are stored and processed in linear structures
public class Expense implements Serializable {

    // CO2: ADT fields - encapsulated data representing an expense entity
    private String date;
    private String category;
    private double amount;
    private String description;

    // CO2: ADT Constructor - initializes the Expense ADT with all required fields
    public Expense(String date, String category, double amount, String description) {
        this.date = date;
        this.category = category;
        this.amount = amount;
        this.description = description;
    }
    
    // CO2: ADT Accessors (Getters) - standard ADT interface for reading encapsulated state
    public String getDate()        { return date; }
    public String getCategory()    { return category; }
    public double getAmount()      { return amount; }
    public String getDescription() { return description; }

    // CO2: ADT Mutators (Setters) - standard ADT interface for modifying encapsulated state
    public void setCategory(String category) { this.category = category; }
    public void setAmount(double amount)     { this.amount = amount; }

    // CO5: toString used during file I/O and display - serialization of ADT to string
    // Format: date,category,amount,description (comma-separated for easy file parsing)
    @Override
    public String toString() {
        return date + "," + category + "," + amount + "," + description;
    }
}
