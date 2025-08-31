package org.totschnig.myexpenses.util;

import android.content.ContentResolver;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.model.Money;
import org.totschnig.myexpenses.model.CurrencyUnit;

public class SampleDataHelper {

    // Add a single sample transaction
    public static void insertSampleTransaction(ContentResolver contentResolver, long accountId, CurrencyUnit currencyUnit) {
        Transaction transaction = Transaction.getNewInstance(accountId, currencyUnit);

        // Set sample data fields
        transaction.setAmount(new Money(currencyUnit, 1000L)); // 10.00 in minor units
        transaction.setPayee("Sample Payee");
        transaction.setComment("This is a sample transaction");

        // Save to DB
        transaction.save(contentResolver);
    }

    // Add multiple sample transactions
    public static void insertSampleTransactions(ContentResolver contentResolver, long accountId, CurrencyUnit currencyUnit) {
        for (int i = 0; i < 5; i++) {
            Transaction transaction = Transaction.getNewInstance(accountId, currencyUnit);
            transaction.setAmount(new Money(currencyUnit, 500L + i * 100L)); // 5.00, 6.00, 7.00, etc.
            transaction.setPayee("Sample Payee #" + (i + 1));
            transaction.setComment("Sample transaction " + (i + 1));
            transaction.save(contentResolver);
        }
    }
}