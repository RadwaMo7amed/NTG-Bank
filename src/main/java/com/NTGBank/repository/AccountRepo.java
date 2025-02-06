package com.NTGBank.repository;

import com.NTGBank.entity.Account;
import com.NTGBank.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
public interface AccountRepo extends JpaRepository<Account,Long> {
}
