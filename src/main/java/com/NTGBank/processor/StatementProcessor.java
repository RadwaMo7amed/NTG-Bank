//package com.NTGBank.processor;
//
//import com.NTGBank.dto.AccountSummary;
//import com.NTGBank.dto.Statement;
//import com.NTGBank.entity.Account;
//import com.NTGBank.entity.Customer;
//import com.NTGBank.entity.Transaction;
//import com.NTGBank.service.AccountService;
//import jakarta.transaction.Transactional;
//import lombok.AllArgsConstructor;
//import org.springframework.batch.item.ItemProcessor;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.stereotype.Component;
//
//import java.util.ArrayList;
//import java.util.List;
//@Component
//@AllArgsConstructor
//public class StatementProcessor {
//    private final AccountService accountService;
//
//    @Transactional
//    public ItemProcessor<Customer, Statement> statementProcessor(JdbcTemplate jdbcTemplate) {
//        return customer -> {
//            Statement statement = new Statement();
//            statement.setCustomerId(customer.getCustomerId());
//            statement.setCustomerName(customer.getFirstName() + " " + customer.getLastName());
//            statement.setCustomerAddress(customer.getAddress1());
//            statement.setPostalCode(customer.getPostalCode());
//            statement.setCustomerCity(customer.getCity());
//            statement.setState(customer.getState());
//
//            List<Account> accounts = customer.getAccounts();
//            if (accounts == null || accounts.isEmpty()) {
//                return statement; // Return an empty statement or handle accordingly
//            }
//            List<AccountSummary> accountSummaries = new ArrayList<>();
//            for (Account account : accounts) {
//                AccountSummary accountSummary = new AccountSummary();
//                accountSummary.setAccountId(account.getAccountId());
//
//                double currentBalance = accountService.calculateCurrentBalance(account.getAccountId());
//                accountSummary.setCurrentBalance(currentBalance);
//
//                List<Transaction> transactions = account.getTransactions();
//
//                if (!transactions.isEmpty()) {
//                    double totalCredits = transactions.stream()
//                            .filter(t -> t.getCreditAmount() != null)
//                            .mapToDouble(Transaction::getCreditAmount)
//                            .sum();
//
//                    double totalDebits = transactions.stream()
//                            .filter(t -> t.getDebitAmount() != null)
//                            .mapToDouble(Transaction::getDebitAmount)
//                            .sum();
//
//                    accountSummary.setTransactions(transactions);
//                    accountSummary.setTotalCredit(totalCredits);
//                    accountSummary.setTotalDebit(totalDebits);
//                } else {
//                    accountSummary.setTotalCredit(0.0);
//                    accountSummary.setTotalDebit(0.0);
//                    accountSummary.setTransactions(new ArrayList<>());
//                }
//
//                accountSummaries.add(accountSummary);
//            }
//
//            statement.setAccountSummaries(accountSummaries);
//            return statement;
//        };
//
//    }
//}
package com.NTGBank.processor;

import com.NTGBank.dto.AccountSummary;
import com.NTGBank.dto.Statement;
import com.NTGBank.entity.Account;
import com.NTGBank.entity.Customer;
import com.NTGBank.entity.Transaction;
import lombok.AllArgsConstructor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

    @Component
    @AllArgsConstructor
    public class StatementProcessor {

        private final JdbcTemplate jdbcTemplate;

        private Double calculateBalanceForAccount(Long accountId) {
            return jdbcTemplate.queryForObject(
                    "SELECT COALESCE(SUM(credit_amount), 0) - COALESCE(SUM(debit_amount), 0) FROM transactions WHERE account_id = ?",
                    Double.class,
                    accountId
            );
        }

        private List<Transaction> getTransactionsForAccount(Long accountId) {
            return jdbcTemplate.query(
                    "SELECT * FROM transactions WHERE account_id = ?",
                    new BeanPropertyRowMapper<>(Transaction.class),
                    accountId
            );
        }

        private AccountSummary processAccount(Account account) {
            AccountSummary accountSummary = new AccountSummary();
            accountSummary.setAccountId(account.getAccountId());

            Double currentBalance = calculateBalanceForAccount(account.getAccountId());
            accountSummary.setCurrentBalance(Optional.ofNullable(currentBalance).orElse(0.0));

            List<Transaction> transactions = getTransactionsForAccount(account.getAccountId());

            if (!transactions.isEmpty()) {
                double totalCredits = transactions.stream()
                        .filter(t -> t.getCreditAmount() != null)
                        .mapToDouble(Transaction::getCreditAmount)
                        .sum();

                double totalDebits = transactions.stream()
                        .filter(t -> t.getDebitAmount() != null)
                        .mapToDouble(Transaction::getDebitAmount)
                        .sum();

                accountSummary.setTransactions(transactions);
                accountSummary.setTotalCredit(totalCredits);
                accountSummary.setTotalDebit(totalDebits);
            } else {
                accountSummary.setTotalCredit(0.0);
                accountSummary.setTotalDebit(0.0);
                accountSummary.setTransactions(new ArrayList<>());
            }

            return accountSummary;
        }

        public ItemProcessor<Customer, Statement> statementProcessor() {
            return customer -> {
                Statement statement = new Statement();
                statement.setCustomerId(customer.getCustomerId());
                statement.setCustomerName(customer.getFirstName() + " " + customer.getLastName());
                statement.setCustomerAddress(customer.getAddress1());
                statement.setPostalCode(customer.getPostalCode());
                statement.setCustomerCity(customer.getCity());
                statement.setState(customer.getState());

                // Fetch accounts for the customer
                List<Account> accounts = jdbcTemplate.query(
                        "SELECT * FROM accounts WHERE customer_id = ?",
                        new BeanPropertyRowMapper<>(Account.class),
                        customer.getCustomerId()
                );
                List<AccountSummary> accountSummaries = new ArrayList<>();
                for (Account account : accounts) {
                    AccountSummary accountSummary = processAccount(account);
                    accountSummaries.add(accountSummary);
                }

                statement.setAccountSummaries(accountSummaries);
                return statement;
            };
        }
    }
