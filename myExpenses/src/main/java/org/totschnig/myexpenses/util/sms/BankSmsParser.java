
/**
 * Base class for bank-specific SMS parsers
 */
public abstract class BankSmsParser {
    protected String bankName;
    protected String senderPattern;
    
    public BankSmsParser(String bankName, String senderPattern) {
        this.bankName = bankName;
        this.senderPattern = senderPattern;
    }
    
    /**
     * Check if this parser can handle the SMS based on sender
     */
    public boolean canParse(String sender, String smsBody) {
        return sender != null && sender.matches(senderPattern);
    }
    
    /**
     * Parse the SMS into transaction data
     */
    public abstract ParsedSmsTransaction parse(String sender, String smsBody);
    
    /**
     * Convert amount string to minor units (cents)
     */
    protected long convertToMinorUnits(String amount, int fractionDigits) {
        try {
            double amountDouble = Double.parseDouble(amount);
            return Math.round(amountDouble * Math.pow(10, fractionDigits));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    /**
     * Parse date from various formats
     */
    protected Date parseDate(String dateStr, String timeStr) {
        try {
            // Try format: "30/08/2025 AT 18:02"
            String fullDateStr = dateStr + " " + timeStr;
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ENGLISH);
            return sdf.parse(fullDateStr);
        } catch (ParseException e) {
            return new Date(); // Fallback to current date
        }
    }
}

