package com.NTGBank.processor;

import com.NTGBank.entity.Transaction;
import org.springframework.batch.item.ItemProcessor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
public class TransactionProcessor implements ItemProcessor<Transaction, Transaction> {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    // DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Transaction process(Transaction transaction) {
        // Convert String timestamp to LocalDateTime before returning the object
        if (transaction.getTimestamp() != null) {
            String timestampStr = transaction.getTimestamp().toString(); // Ensure it's in String format
            String inputDate = timestampStr.replace("T", " ");
            LocalDateTime timestamp = LocalDateTime.parse(inputDate, FORMATTER);
            transaction.setTimestamp(timestamp); // Set converted LocalDateTime
        }
        return transaction;
}
}