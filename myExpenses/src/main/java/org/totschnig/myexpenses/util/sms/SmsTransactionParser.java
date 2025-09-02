/**
 * Main SMS Transaction Parser - handles multiple banks
 */

package org.totschnig.myexpenses.util.sms;

import java.util.List;
import java.util.ArrayList;

public class SmsTransactionParser {
    private java.util.List<BankSmsParser> parsers;
    
    public SmsTransactionParser() {
        parsers = new java.util.ArrayList<>();
        // Register bank parsers
        parsers.add(new HsbcSmsParser());
        // Add more banks here: parsers.add(new ComBankSmsParser());
    }
    
    /**
     * Parse SMS from any supported bank
     */
    public ParsedSmsTransaction parseSms(String sender, String smsBody) {
        for (BankSmsParser parser : parsers) {
            if (parser.canParse(sender, smsBody)) {
                return parser.parse(sender, smsBody);
            }
        }
        return null; // No parser found for this SMS
    }
    
    /**
     * Check if SMS looks like a bank transaction
     */
    public boolean isBankTransaction(String sender, String smsBody) {
        for (BankSmsParser parser : parsers) {
            if (parser.canParse(sender, smsBody)) {
                return true;
            }
        }
        return false;
    }
}