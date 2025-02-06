package com.NTGBank.dto;

import com.NTGBank.entity.Transaction;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountSummary {
    private Long accountId;
    private double currentBalance;
    private List<Transaction> transactions;
    private double totalDebit;
    private double totalCredit;
}
