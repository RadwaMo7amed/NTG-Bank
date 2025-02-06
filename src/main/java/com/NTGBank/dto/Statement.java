package com.NTGBank.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Statement {
    private Long customerId;
    private String customerName;
    private String customerAddress;
    private String customerCity;
    private String state;
    private String postalCode;
    private List<AccountSummary>accountSummaries;
}
