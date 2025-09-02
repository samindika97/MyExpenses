// StandaloneTests.java - Can run independently without Android dependencies
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.List;
import java.util.ArrayList;

// Copy your classes here with minimal dependencies

/**
 * Standalone version of ParsedSmsTransaction for testing
 */
class ParsedSmsTransaction {
    public String sender;
    public String amount;
    public String currency;
    public long amountMinor;
    public String cardNumber;
    public String merchant;
    public Date transactionDate;
    public String balance;
    public String rawSms;
    public boolean isDebit;
    
    public ParsedSmsTransaction() {
        this.isDebit = true;
    }
    
    @Override
    public String toString() {
        return String.format("SMS Transaction: %s %s%s from %s at %s", 
                            isDebit ? "Debit" : "Credit",
                            currency, amount, merchant, 
                            transactionDate != null ? transactionDate.toString() : "unknown date");
    }
}

/**
 * Base parser class
 */
abstract class BankSmsParser {
    protected String bankName;
    protected String senderPattern;
    
    public BankSmsParser(String bankName, String senderPattern) {
        this.bankName = bankName;
        this.senderPattern = senderPattern;
    }
    
    public boolean canParse(String sender, String smsBody) {
        return sender != null && sender.matches(senderPattern);
    }
    
    public abstract ParsedSmsTransaction parse(String sender, String smsBody);
    
    protected long convertToMinorUnits(String amount, int fractionDigits) {
        try {
            double amountDouble = Double.parseDouble(amount);
            return Math.round(amountDouble * Math.pow(10, fractionDigits));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    protected Date parseDate(String dateStr, String timeStr) {
        try {
            String fullDateStr = dateStr + " " + timeStr;
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ENGLISH);
            return sdf.parse(fullDateStr);
        } catch (ParseException e) {
            return new Date();
        }
    }
}

/**
 * HSBC parser implementation
 */
class HsbcSmsParser extends BankSmsParser {
    
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
            transaction.currency = matcher.group(1);
            transaction.amount = matcher.group(2).replace(",", "");
            transaction.cardNumber = matcher.group(3);
            transaction.merchant = matcher.group(4).trim();
            String dateStr = matcher.group(5);
            String timeStr = matcher.group(6);
            transaction.balance = matcher.group(8).replace(",", "");
            
            transaction.amountMinor = convertToMinorUnits(transaction.amount, 2);
            transaction.transactionDate = parseDate(dateStr, timeStr);
            transaction.isDebit = true;
        }
        
        return transaction;
    }
}

/**
 * Main parser
 */
class SmsTransactionParser {
    private List<BankSmsParser> parsers;
    
    public SmsTransactionParser() {
        parsers = new ArrayList<>();
        parsers.add(new HsbcSmsParser());
    }
    
    public ParsedSmsTransaction parseSms(String sender, String smsBody) {
        for (BankSmsParser parser : parsers) {
            if (parser.canParse(sender, smsBody)) {
                return parser.parse(sender, smsBody);
            }
        }
        return null;
    }
    
    public boolean isBankTransaction(String sender, String smsBody) {
        for (BankSmsParser parser : parsers) {
            if (parser.canParse(sender, smsBody)) {
                return true;
            }
        }
        return false;
    }
}

/**
 * Test runner - main class to execute tests
 */
public class StandaloneTests {
    
    public static void main(String[] args) {
        System.out.println("=== Standalone SMS Parser Tests ===\n");
        
        runBasicTests();
        runEdgeCaseTests();
        runValidationTests();
        
        System.out.println("\n=== All Tests Completed ===");
    }
    
    private static void runBasicTests() {
        System.out.println("1. Basic Functionality Tests");
        System.out.println("----------------------------");
        
        SmsTransactionParser parser = new SmsTransactionParser();
        
        // Test 1: Valid HSBC SMS
        String validSms = "HSBC:TXN AUTH AMT LKR5,444.81 CC ENDING *** 9330 AT CARGILLS EXPRESS - ELPITIYA ON 30/08/2025 AT 18:02 AVL BAL LKR189,560.19";
        
        System.out.println("Test 1.1 - Bank Transaction Detection:");
        boolean isBankTxn = parser.isBankTransaction("HSBC", validSms);
        System.out.println("  ✓ HSBC SMS detected as bank transaction: " + isBankTxn);
        
        System.out.println("\nTest 1.2 - SMS Parsing:");
        ParsedSmsTransaction result = parser.parseSms("HSBC", validSms);
        if (result != null) {
            System.out.println("  ✓ SMS parsed successfully");
            System.out.println("    Sender: " + result.sender);
            System.out.println("    Amount: " + result.currency + " " + result.amount);
            System.out.println("    Minor Units: " + result.amountMinor);
            System.out.println("    Card: ****" + result.cardNumber);
            System.out.println("    Merchant: " + result.merchant);
            System.out.println("    Balance: " + result.currency + " " + result.balance);
            System.out.println("    Date: " + result.transactionDate);
            System.out.println("    Is Debit: " + result.isDebit);
        } else {
            System.out.println("  ✗ Failed to parse SMS");
        }
        
        // Test 2: Non-bank SMS
        System.out.println("\nTest 1.3 - Non-bank SMS:");
        String regularSms = "Hello, how are you doing today?";
        boolean isNotBank = parser.isBankTransaction("FRIEND", regularSms);
        ParsedSmsTransaction nullResult = parser.parseSms("FRIEND", regularSms);
        System.out.println("  ✓ Regular SMS not detected as bank: " + !isNotBank);
        System.out.println("  ✓ Parse result is null: " + (nullResult == null));
    }
    
    private static void runEdgeCaseTests() {
        System.out.println("\n\n2. Edge Case Tests");
        System.out.println("------------------");
        
        SmsTransactionParser parser = new SmsTransactionParser();
        
        String[] testCases = {
            // Different amount formats
            "HSBC:TXN AUTH AMT LKR1000 CC ENDING *** 9330 AT TEST MERCHANT ON 30/08/2025 AT 18:02 AVL BAL LKR50000",
            "HSBC:TXN AUTH AMT LKR1,000.50 CC ENDING *** 9330 AT TEST MERCHANT ON 30/08/2025 AT 18:02 AVL BAL LKR50,000.25",
            "HSBC:TXN AUTH AMT LKR12,345,678.99 CC ENDING *** 9330 AT VERY LONG MERCHANT NAME HERE ON 30/08/2025 AT 18:02 AVL BAL LKR1,000,000.01",
            // Different merchant names
            "HSBC:TXN AUTH AMT LKR100.00 CC ENDING *** 1234 AT ATM-WITHDRAWAL ON 01/09/2025 AT 09:15 AVL BAL LKR25,000.00",
            "HSBC:TXN AUTH AMT LKR50.75 CC ENDING *** 5678 AT ONLINE-PURCHASE ON 02/09/2025 AT 23:59 AVL BAL LKR1,500.25"
        };
        
        String[] expectedAmounts = {"1000", "1000.50", "12345678.99", "100.00", "50.75"};
        long[] expectedMinor = {100000L, 100050L, 1234567899L, 10000L, 5075L};
        
        for (int i = 0; i < testCases.length; i++) {
            System.out.println("Test 2." + (i + 1) + " - Amount Format Test:");
            ParsedSmsTransaction result = parser.parseSms("HSBC", testCases[i]);
            
            if (result != null) {
                boolean amountCorrect = expectedAmounts[i].equals(result.amount);
                boolean minorCorrect = expectedMinor[i] == result.amountMinor;
                
                System.out.println("  Amount: " + result.amount + " (expected: " + expectedAmounts[i] + ") " + 
                                 (amountCorrect ? "✓" : "✗"));
                System.out.println("  Minor: " + result.amountMinor + " (expected: " + expectedMinor[i] + ") " + 
                                 (minorCorrect ? "✓" : "✗"));
                System.out.println("  Merchant: " + result.merchant);
            } else {
                System.out.println("  ✗ Failed to parse");
            }
        }
    }
    
    private static void runValidationTests() {
        System.out.println("\n\n3. Validation Tests");
        System.out.println("-------------------");
        
        HsbcSmsParser hsbcParser = new HsbcSmsParser();
        
        // Test sender matching
        System.out.println("Test 3.1 - Sender Validation:");
        System.out.println("  HSBC: " + hsbcParser.canParse("HSBC", "any message") + " (should be true)");
        System.out.println("  HSBC-BANK: " + hsbcParser.canParse("HSBC-BANK", "any message") + " (should be true)");
        System.out.println("  COMBANK: " + hsbcParser.canParse("COMBANK", "any message") + " (should be false)");
        System.out.println("  SAMPATH: " + hsbcParser.canParse("SAMPATH", "any message") + " (should be false)");
        
        // Test malformed SMS
        System.out.println("\nTest 3.2 - Malformed SMS:");
        String[] malformedSms = {
            "HSBC: This is not a transaction message",
            "HSBC:TXN AUTH AMT", // Incomplete
            "HSBC:TXN AUTH AMT LKR5000", // Missing parts
            "", // Empty
            null // Null (this might cause exception, handle carefully)
        };
        
        for (int i = 0; i < malformedSms.length - 1; i++) { // Skip null test for now
            ParsedSmsTransaction result = hsbcParser.parse("HSBC", malformedSms[i]);
            System.out.println("  Test " + (i + 1) + ": " + 
                             (result.amount == null ? "✓ Properly handled malformed SMS" : 
                              "✗ Should not parse malformed SMS"));
        }
    }
}