package com.backend.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.backend.Entity.Transaction;



public interface TransactionRepository extends JpaRepository<Transaction, Long> {
   
}

