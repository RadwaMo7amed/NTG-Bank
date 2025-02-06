package com.NTGBank.service;

import com.NTGBank.entity.Account;
import com.NTGBank.entity.Customer;
import com.NTGBank.entity.Transaction;
import com.NTGBank.repository.AccountRepo;
import com.NTGBank.repository.CustomerRepo;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
@AllArgsConstructor
public class AccountService {
    private final AccountRepo accountRepo;
    public double calculateCurrentBalance(Long accountId){
        Account account = accountRepo.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found with ID: " + accountId));
        List<Transaction>transactions=account.getTransactions();
        double currentBalance=0.0;
        for (Transaction transaction:transactions){
            if (transaction.getDebitAmount() != null)
                currentBalance-=transaction.getDebitAmount();
            if (transaction.getCreditAmount() != null)
                currentBalance+=transaction.getCreditAmount();
        }
        return currentBalance;
    }
}
