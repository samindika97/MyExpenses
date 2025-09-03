package org.totschnig.myexpenses.util.sms;

import android.content.Context;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.content.ContentUris;
import android.util.Log;

import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.CurrencyUnit;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.provider.DatabaseConstants;

import java.util.Map;

import static org.totschnig.myexpenses.provider.DatabaseConstants.*;

/**
 * Main SMS Transaction Processor
 */
public class SmsTransactionProcessor {
    private static final String TAG = "SmsTransactionProcessor";
    
    private SmsTransactionParser parser;
    private SmsAccountMapper accountMapper;
    private Context context;
    
    public SmsTransactionProcessor(Context context) {
        this.context = context;
        this.parser = new SmsTransactionParser();
        this.accountMapper = new SmsAccountMapper(context);
    }
    
    /**
     * Process incoming SMS and create transaction if applicable
     */
    public boolean processSms(String sender, String smsBody) {
        Log.d(TAG, "Processing SMS from: " + sender);
        
        // Check if it's a bank transaction SMS
        if (!parser.isBankTransaction(sender, smsBody)) {
            Log.d(TAG, "Not a bank transaction SMS");
            return false;
        }
        
        // Parse the SMS
        ParsedSmsTransaction parsed = parser.parseSms(sender, smsBody);
        if (parsed == null) {
            Log.e(TAG, "Failed to parse SMS");
            return false;
        }
        
        Log.d(TAG, "SMS parsed successfully: " + parsed.toString());
        
        // Check if we have account mapping
        long accountId = accountMapper.getAccountId(sender);
        if (accountId == -1) {
            Log.d(TAG, "No mapping found for sender: " + sender);
            // No mapping found - show user a dialog to map this sender
            showAccountMappingDialog(parsed);
            return false;
        }
        
        Log.d(TAG, "Found account mapping: " + sender + " -> " + accountId);
        
        // Create the transaction
        return createTransactionFromSms(parsed, accountId);
    }
    
    /**
     * Create transaction from parsed SMS data
     */
private boolean createTransactionFromSms(ParsedSmsTransaction parsed, long accountId) {
    try {
        Log.d(TAG, "Creating transaction for account ID: " + accountId);
        
        // Get account currency from database
        String currencyCode = getAccountCurrency(accountId);
        if (currencyCode == null) {
            Log.e(TAG, "Could not find account with ID: " + accountId);
            return false;
        }
        
        Log.d(TAG, "Account currency: " + currencyCode);
        
        // Get Java Currency instance to extract symbol and fraction digits
        Currency javaCurrency = Currency.getInstance(currencyCode);
        String currencySymbol = javaCurrency.getSymbol();
        int fractionDigits = javaCurrency.getDefaultFractionDigits();
        
        // Create currency unit with all required parameters
        CurrencyUnit currencyUnit = new CurrencyUnit(currencyCode, currencySymbol, fractionDigits);
        
        // Create transaction
        Transaction transaction = Transaction.getNewInstance(accountId, currencyUnit);
        
        // Set amount (negative for debit, positive for credit)
        long amount = parsed.isDebit ? -parsed.amountMinor : parsed.amountMinor;
        transaction.setAmount(new Money(currencyUnit, amount));
        
        // Set other fields
        transaction.setPayee(parsed.merchant);
        transaction.setComment("Auto-imported from SMS: " + parsed.sender);
        
        // Set date if parsed successfully
        if (parsed.transactionDate != null) {
            transaction.setDate(parsed.transactionDate.getTime() / 1000);
        }
        
        // Save transaction
        transaction.save(context.getContentResolver());
        
        Log.d(TAG, "Transaction created successfully");
        return true;
        
    } catch (Exception e) {
        Log.e(TAG, "Error creating transaction", e);
        return false;
    }
}
    
    /**
     * Get account currency from database using ContentResolver
     */
    private String getAccountCurrency(long accountId) {
        ContentResolver resolver = context.getContentResolver();
        Uri accountUri = ContentUris.withAppendedId(TransactionProvider.ACCOUNTS_URI, accountId);
        
        Cursor cursor = null;
        try {
            cursor = resolver.query(
                accountUri,
                new String[]{KEY_CURRENCY}, // Only query currency column
                null,
                null,
                null
            );
            
            if (cursor != null && cursor.moveToFirst()) {
                String currency = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CURRENCY));
                Log.d(TAG, "Found currency for account " + accountId + ": " + currency);
                return currency;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error querying account currency", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        
        Log.e(TAG, "No account found with ID: " + accountId);
        return null;
    }
    
    /**
     * Show dialog for user to map SMS sender to account
     */
    private void showAccountMappingDialog(ParsedSmsTransaction parsed) {
        // TODO: Implement dialog to let user choose which account
        // this SMS sender should map to
        Log.d(TAG, "Need to show account mapping dialog for sender: " + parsed.sender);
        Log.d(TAG, "Parsed transaction: " + parsed.toString());
    }
    
    /**
     * Configure mapping for a sender
     */
    public void configureSenderMapping(String sender, long accountId) {
        accountMapper.mapSenderToAccount(sender, accountId);
        Log.d(TAG, "Configured mapping: " + sender + " -> " + accountId);
    }
    
    /**
     * Get all configured mappings for UI display
     */
    public Map<String, Long> getAllMappings() {
        return accountMapper.getAllMappings();
    }
}