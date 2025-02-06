package com.NTGBank.repository;

import com.NTGBank.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


public interface CustomerRepo extends JpaRepository<Customer,Long> {
}
