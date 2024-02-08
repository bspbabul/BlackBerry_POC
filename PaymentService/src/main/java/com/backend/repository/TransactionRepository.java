package com.backend.repository;


import org.springframework.data.jpa.repository.JpaRepository;

import com.backend.Entiry.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
   
}
