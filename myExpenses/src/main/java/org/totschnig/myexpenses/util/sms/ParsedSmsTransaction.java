package org.totschnig.myexpenses.util.sms;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Locale;

/**
 * Represents a parsed SMS transaction
 */
public class ParsedSmsTransaction {
    public String sender;           // e.g., "HSBC"
    public String amount;          // e.g., "5444.81"
    public String currency;        // e.g., "LKR"
    public long amountMinor;       // Amount in minor units (cents)
    public String cardNumber;      // e.g., "9330" (last 4 digits)
    public String merchant;        // e.g., "CARGILLS EXPRESS - ELPITI"
    public Date transactionDate;   // Parsed date
    public String balance;         // e.g., "189560.19"
    public String rawSms;          // Original SMS text
    public boolean isDebit;        // true for debit, false for credit
    
    public ParsedSmsTransaction() {
        this.isDebit = true; // Default to debit
    }
    
    @Override
    public String toString() {
        return String.format("SMS Transaction: %s %s%s from %s at %s", 
                            isDebit ? "Debit" : "Credit",
                            currency, amount, merchant, 
                            transactionDate != null ? transactionDate.toString() : "unknown date");
    }
}
