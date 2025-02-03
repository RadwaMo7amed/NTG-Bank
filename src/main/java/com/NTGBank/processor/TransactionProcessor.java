//package com.NTGBank.processor;
//
//import com.NTGBank.entity.Transaction;
//import org.springframework.batch.item.ItemProcessor;
//
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//public class TransactionProcessor implements ItemProcessor<Transaction, Transaction> {
//    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//    // DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//
//    @Override
//    public Transaction process(Transaction transaction) {
//        // Convert String timestamp to LocalDateTime before returning the object
//        if (transaction.getTimestamp() != null) {
//            String timestampStr = transaction.getTimestamp().toString(); // Ensure it's in String format
//            String inputDate = timestampStr.replace("T", " ");
//            LocalDateTime timestamp = LocalDateTime.parse(inputDate, FORMATTER);
//            transaction.setTimestamp(timestamp); // Set converted LocalDateTime
//        }
//        return transaction;
//}
//}
package com.NTGBank.processor;

import com.NTGBank.entity.Transaction;
import org.springframework.batch.item.ItemProcessor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class TransactionProcessor implements ItemProcessor<Transaction, Transaction> {
    private static final DateTimeFormatter FORMATTER_WITH_SECONDS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter FORMATTER_WITHOUT_SECONDS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public Transaction process(Transaction transaction) {
        if (transaction.getTimestamp() != null) {

            String timestampStr = transaction.getTimestamp().toString();
            timestampStr = timestampStr.replace("T", " ");
            if (timestampStr.startsWith("'") && timestampStr.endsWith("'")) {
                timestampStr = timestampStr.substring(1, timestampStr.length() - 1);
            }
            try {
                // Try parsing with seconds first
                LocalDateTime timestamp = LocalDateTime.parse(timestampStr, FORMATTER_WITH_SECONDS);
                transaction.setTimestamp(timestamp);
            } catch (DateTimeParseException e) {
                // If parsing with seconds fails, try without seconds
                LocalDateTime timestamp = LocalDateTime.parse(timestampStr, FORMATTER_WITHOUT_SECONDS);
                transaction.setTimestamp(timestamp);
            }
        }
        return transaction;
    }
}