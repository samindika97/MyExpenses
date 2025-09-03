package org.totschnig.myexpenses.util.sms;

import android.content.Context;
import android.content.SharedPreferences;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.util.sms.ParsedSmsTransaction;
import org.totschnig.myexpenses.util.sms.SmsTransactionParser;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps SMS senders to user's accounts
 */
public class SmsAccountMapper {
    private static final String PREFS_NAME = "sms_account_mapping";
    private Context context;
    private SharedPreferences prefs;
    
    public SmsAccountMapper(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * Save mapping: SMS sender -> Account ID
     */
    public void mapSenderToAccount(String sender, long accountId) {
        prefs.edit()
            .putLong(sender.toUpperCase(), accountId)
            .apply();
    }
    
    /**
     * Get account ID for SMS sender
     */
    public long getAccountId(String sender) {
        return prefs.getLong(sender.toUpperCase(), -1);
    }
    
    /**
     * Remove mapping
     */
    public void removeMappingForSender(String sender) {
        prefs.edit()
            .remove(sender.toUpperCase())
            .apply();
    }
    
    /**
     * Get all current mappings
     */
    public Map<String, Long> getAllMappings() {
        Map<String, Long> mappings = new HashMap<>();
        Map<String, ?> all = prefs.getAll();
        for (Map.Entry<String, ?> entry : all.entrySet()) {
            if (entry.getValue() instanceof Long) {
                mappings.put(entry.getKey(), (Long) entry.getValue());
            }
        }
        return mappings;
    }
    
    /**
     * Check if sender is already mapped
     */
    public boolean isMapped(String sender) {
        return prefs.contains(sender.toUpperCase());
    }
}

