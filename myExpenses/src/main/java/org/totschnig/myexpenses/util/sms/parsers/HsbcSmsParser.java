/**
 * HSBC SMS Parser
 */
public class HsbcSmsParser extends BankSmsParser {
    
    // HSBC SMS Pattern
    private static final Pattern HSBC_PATTERN = Pattern.compile(
        "HSBC:TXN AUTH AMT ([A-Z]{3})([0-9,]+\\.?[0-9]*) CC ENDING \\*\\*\\* ([0-9]{4}) AT (.+?) ON ([0-9]{2}/[0-9]{2}/[0-9]{4}) AT ([0-9]{2}:[0-9]{2}) AVL BAL ([A-Z]{3})([0-9,]+\\.?[0-9]*)",
        Pattern.CASE_INSENSITIVE
    );
    
    public HsbcSmsParser() {
        super("HSBC", "HSBC.*");
    }
    
    @Override
    public ParsedSmsTransaction parse(String sender, String smsBody) {
        ParsedSmsTransaction transaction = new ParsedSmsTransaction();
        transaction.sender = sender;
        transaction.rawSms = smsBody;
        
        Matcher matcher = HSBC_PATTERN.matcher(smsBody);
        if (matcher.find()) {
            // Extract data
            transaction.currency = matcher.group(1);           // LKR
            transaction.amount = matcher.group(2).replace(",", ""); // 5444.81
            transaction.cardNumber = matcher.group(3);         // 9330
            transaction.merchant = matcher.group(4).trim();    // CARGILLS EXPRESS - ELPITI
            String dateStr = matcher.group(5);                 // 30/08/2025
            String timeStr = matcher.group(6);                 // 18:02
            transaction.balance = matcher.group(8).replace(",", ""); // 189560.19
            
            // Convert amount to minor units (assuming 2 decimal places for LKR)
            transaction.amountMinor = convertToMinorUnits(transaction.amount, 2);
            
            // Parse date
            transaction.transactionDate = parseDate(dateStr, timeStr);
            
            // HSBC format shows debits
            transaction.isDebit = true;
        }
        
        return transaction;
    }
}
