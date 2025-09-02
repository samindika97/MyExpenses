/**
 * Main SMS Transaction Processor
 */

package org.totschnig.myexpenses.util.sms;

import android.content.Context;
import org.totschnig.myexpenses.model.Account;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.model.Money;
import java.util.Map;


public class SmsTransactionProcessor {
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
        // Check if it's a bank transaction SMS
        if (!parser.isBankTransaction(sender, smsBody)) {
            return false;
        }
        
        // Parse the SMS
        ParsedSmsTransaction parsed = parser.parseSms(sender, smsBody);
        if (parsed == null) {
            return false;
        }
        
        // Check if we have account mapping
        long accountId = accountMapper.getAccountId(sender);
        if (accountId == -1) {
            // No mapping found - show user a dialog to map this sender
            showAccountMappingDialog(parsed);
            return false;
        }
        
        // Create the transaction
        return createTransactionFromSms(parsed, accountId);
    }
    
    /**
     * Create transaction from parsed SMS data
     */
    private boolean createTransactionFromSms(ParsedSmsTransaction parsed, long accountId) {
        try {
            // Get account to get currency
            // You'll need to implement getAccountById() or similar
            Account account = getAccountById(accountId);
            if (account == null) return false;
            
            // Create transaction
            Transaction transaction = Transaction.getNewInstance(accountId, account.getCurrencyUnit());
            
            // Set amount (negative for debit, positive for credit)
            long amount = parsed.isDebit ? -parsed.amountMinor : parsed.amountMinor;
            transaction.setAmount(new Money(account.getCurrencyUnit(), amount));
            
            // Set other fields
            transaction.setPayee(parsed.merchant);
            transaction.setComment("Auto-imported from SMS: " + parsed.sender);
            
            // Set date if parsed successfully
            if (parsed.transactionDate != null) {
                transaction.setDate(parsed.transactionDate.getTime() / 1000);
            }
            
            // Save transaction
            transaction.save(context.getContentResolver());
            return true;
            
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Show dialog for user to map SMS sender to account
     */
    private void showAccountMappingDialog(ParsedSmsTransaction parsed) {
        // TODO: Implement dialog to let user choose which account
        // this SMS sender should map to
        // For now, just log it
        System.out.println("Need to map sender: " + parsed.sender + " to an account");
    }
    
    /**
     * Get account by ID - you'll need to implement this
     */
    private Account getAccountById(long accountId) {
        // TODO: Implement account lookup
        return null;
    }
    
    /**
     * Configure mapping for a sender
     */
    public void configureSenderMapping(String sender, long accountId) {
        accountMapper.mapSenderToAccount(sender, accountId);
    }
    
    /**
     * Get all configured mappings for UI display
     */
    public Map<String, Long> getAllMappings() {
        return accountMapper.getAllMappings();
    }
}