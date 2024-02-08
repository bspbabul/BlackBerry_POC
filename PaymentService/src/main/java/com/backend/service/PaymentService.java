package com.backend.service;

import java.util.List;

import com.backend.Entiry.Orders;
import com.backend.Entiry.Transaction;

public interface PaymentService {
	public void makePayment(Orders orderRequest);

	public List<Transaction> getAllTransaction();

}
