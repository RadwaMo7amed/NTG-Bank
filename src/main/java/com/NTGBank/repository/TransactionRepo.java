package com.NTGBank.repository;

import com.NTGBank.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepo extends JpaRepository<Transaction,Long> {
}
