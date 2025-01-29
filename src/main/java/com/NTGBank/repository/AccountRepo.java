package com.NTGBank.repository;

import com.NTGBank.entity.Account;
import com.NTGBank.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepo extends JpaRepository<Account,Long> {
}
