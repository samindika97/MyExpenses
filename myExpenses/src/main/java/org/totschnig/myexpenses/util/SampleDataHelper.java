package org.totschnig.myexpenses.util;

import android.content.ContentResolver;
import android.content.Context;
import android.util.Log;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.CurrencyUnit;
import org.totschnig.myexpenses.util.sms.SmsTransactionParser;
import org.totschnig.myexpenses.util.sms.ParsedSmsTransaction;
import org.totschnig.myexpenses.util.sms.SmsAccountMapper;

public class SampleDataHelper {

    private static final String TAG = "SmsProcessorTestx";

    // Your existing sample transaction methods...
    public static void insertSampleTransactions(ContentResolver contentResolver, long accountId, CurrencyUnit currencyUnit) {
        for (int i = 0; i < 5; i++) {
            Transaction transaction = Transaction.getNewInstance(accountId, currencyUnit);
            transaction.setAmount(new Money(currencyUnit, 500L + i * 100L));
            transaction.setPayee("Sample Payee #" + (i + 1));
            transaction.setComment("Sample transaction " + (i + 1));
            transaction.save(contentResolver);
        }
    }

    // NEW: Test SMS parsing without real SMS
    public static void testSmsParser(Context context) {
        Log.d(TAG, "Starting transaction creation for account: ");
        SmsTransactionParser parser = new SmsTransactionParser();
        
        // Test SMS samples
        String[] testSms = {
            "HSBC:TXN AUTH AMT LKR5444.81 CC ENDING *** 9330 AT CARGILLS EXPRESS - ELPITI ON 30/08/2025 AT 18:02 AVL BAL LKR189560.19 IF UNAUTHORISED CALL 009411 4472200",
            "ComBank: Rs.2500.00 debited from A/C ***4567 at KEELLS SUPER ON 31/08/2025 12:30 Bal: Rs.45000.00",
            "Sampath: LKR 1200.50 withdrawn from Card ***8901 at ATM-KANDY on 29/08/2025 15:45"
        };
        
        String[] testSenders = {"HSBC", "ComBank", "Sampath"};
        
        Log.d("SMS_TEST", "=== Testing SMS Parser ===");
        
        for (int i = 0; i < testSms.length; i++) {
            String sender = testSenders[i];
            String sms = testSms[i];
            
            Log.d("SMS_TEST", "\n--- Testing: " + sender + " ---");
            Log.d("SMS_TEST", "SMS: " + sms);
            
            // Test if parser recognizes it
            boolean canParse = parser.isBankTransaction(sender, sms);
            Log.d("SMS_TEST", "Can parse: " + canParse);
            
            if (canParse) {
                // Test parsing
                ParsedSmsTransaction parsed = parser.parseSms(sender, sms);
                if (parsed != null) {
                    Log.d("SMS_TEST", "Parsed result: " + parsed.toString());
                    Log.d("SMS_TEST", "Amount: " + parsed.currency + parsed.amount);
                    Log.d("SMS_TEST", "Merchant: " + parsed.merchant);
                    Log.d("SMS_TEST", "Card: ***" + parsed.cardNumber);
                } else {
                    Log.d("SMS_TEST", "Parsing failed!");
                }
            }
        }
    }
    
    // NEW: Test account mapping
    public static void testAccountMapping(Context context) {
        SmsAccountMapper mapper = new SmsAccountMapper(context);
        
        Log.d("SMS_TEST", "=== Testing Account Mapping ===");
        
        // Test mapping SMS senders to accounts
        mapper.mapSenderToAccount("HSBC", 1L);
        mapper.mapSenderToAccount("ComBank", 2L);
        mapper.mapSenderToAccount("Sampath", 3L);
        
        // Test retrieval
        Log.d("SMS_TEST", "HSBC maps to account: " + mapper.getAccountId("HSBC"));
        Log.d("SMS_TEST", "ComBank maps to account: " + mapper.getAccountId("ComBank"));
        Log.d("SMS_TEST", "Unknown bank maps to: " + mapper.getAccountId("UnknownBank"));
        
        // Show all mappings
        Log.d("SMS_TEST", "All mappings: " + mapper.getAllMappings().toString());
    }
    
    // NEW: Test complete SMS processing (simulate SMS arrival)
    public static void testSmsProcessing(Context context, ContentResolver contentResolver, long selectedAccountId, CurrencyUnit currencyUnit) {
        Log.d("SMS_TEST", "=== Testing Complete SMS Processing ===");
        Log.d("SMS_TEST", "Using selected account ID: " + selectedAccountId);
        
        // First, set up account mapping for the currently selected account
        SmsAccountMapper mapper = new SmsAccountMapper(context);
        mapper.mapSenderToAccount("HSBC", selectedAccountId);
        
        // Simulate SMS arrival
        String testSender = "HSBC";
        String testSms = "HSBC:TXN AUTH AMT LKR1500.00 CC ENDING *** 9330 AT TEST MERCHANT ON 30/08/2025 AT 18:02 AVL BAL LKR189560.19";
        
        SmsTransactionParser parser = new SmsTransactionParser();
        
        if (parser.isBankTransaction(testSender, testSms)) {
            ParsedSmsTransaction parsed = parser.parseSms(testSender, testSms);
            if (parsed != null) {
                Log.d("SMS_TEST", "Would create transaction: " + parsed.toString());
                
                // Actually create the transaction in the selected account
                Transaction transaction = Transaction.getNewInstance(selectedAccountId, currencyUnit);
                transaction.setAmount(new Money(currencyUnit, -parsed.amountMinor)); // Negative for debit
                transaction.setPayee(parsed.merchant);
                transaction.setComment("SMS Import Test: " + parsed.sender);
                transaction.save(contentResolver);
                
                Log.d("SMS_TEST", "Transaction created successfully in account: " + selectedAccountId);
            }
        }
    }
    
    // NEW: Test with multiple accounts
    public static void testAccountMapping(Context context, long selectedAccountId) {
        SmsAccountMapper mapper = new SmsAccountMapper(context);
        
        Log.d("SMS_TEST", "=== Testing Account Mapping ===");
        Log.d("SMS_TEST", "Current selected account: " + selectedAccountId);
        
        // Test mapping SMS senders to the selected account
        mapper.mapSenderToAccount("HSBC", selectedAccountId);
        mapper.mapSenderToAccount("ComBank", selectedAccountId + 1); // Different account
        mapper.mapSenderToAccount("Sampath", selectedAccountId + 2); // Another account
        
        // Test retrieval
        Log.d("SMS_TEST", "HSBC maps to account: " + mapper.getAccountId("HSBC"));
        Log.d("SMS_TEST", "ComBank maps to account: " + mapper.getAccountId("ComBank"));
        Log.d("SMS_TEST", "Unknown bank maps to: " + mapper.getAccountId("UnknownBank"));
        
        // Show all mappings
        Log.d("SMS_TEST", "All mappings: " + mapper.getAllMappings().toString());
    }
}