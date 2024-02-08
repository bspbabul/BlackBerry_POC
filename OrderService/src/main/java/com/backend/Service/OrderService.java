package com.backend.Service;

import java.util.List;

import com.backend.Entity.Orders;
import com.backend.Entity.Transaction;

public interface OrderService {

	void placeOrder(Orders orderRequest);
	public List<Transaction> getAllTransactions();
}
