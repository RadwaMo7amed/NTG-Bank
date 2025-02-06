package com.NTGBank.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private Double creditAmount;
    private Double debitAmount;
    private LocalDateTime  timestamp;

    @ManyToOne
    @JoinColumn(name = "accountId")
    private Account account;
}
