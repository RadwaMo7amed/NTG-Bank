package com.NTGBank.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "transactions")
public class Transaction {
    @Id
    private Long transactionId;
    private String description;
    private String creditAmount;
    private String debitAmount;
    private Timestamp timestamp;
    @ManyToOne
    @JoinColumn(name = "accountId")
    private Account account;
}
