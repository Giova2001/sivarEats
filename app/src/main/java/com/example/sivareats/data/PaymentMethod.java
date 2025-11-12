package com.example.sivareats.data;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;


@Entity(tableName = "payment_methods")
public class PaymentMethod {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "card_number")
    private String cardNumber;

    @ColumnInfo(name = "card_holder")
    private String cardHolder;

    @ColumnInfo(name = "expiry_date")
    private String expiryDate;

    @ColumnInfo(name = "cvv")
    private String cvv;

    @ColumnInfo(name = "card_type")
    private String cardType; // Visa, Mastercard, etc.

    @ColumnInfo(name = "is_default")
    private boolean isDefault;

    public PaymentMethod() {}

    public PaymentMethod(String cardNumber, String cardHolder, String expiryDate, String cvv, String cardType) {
        this.cardNumber = cardNumber;
        this.cardHolder = cardHolder;
        this.expiryDate = expiryDate;
        this.cvv = cvv;
        this.cardType = cardType;
        this.isDefault = false;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }

    public String getCardHolder() { return cardHolder; }
    public void setCardHolder(String cardHolder) { this.cardHolder = cardHolder; }

    public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }

    public String getCvv() { return cvv; }
    public void setCvv(String cvv) { this.cvv = cvv; }

    public String getCardType() { return cardType; }
    public void setCardType(String cardType) { this.cardType = cardType; }

    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean isDefault) { this.isDefault = isDefault; }
}

