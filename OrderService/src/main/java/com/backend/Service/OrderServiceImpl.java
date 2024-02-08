package com.backend.Service;

import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import com.backend.Entity.Orders;
import com.backend.Entity.Transaction;
import com.backend.Exception.InsufficientQuantityException;
import com.backend.Exception.ItemNotFoundException;
import com.backend.Exception.PaymentFailedException;
import com.backend.Repository.OrderRepository;
import com.backend.Repository.TransactionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Service
public class OrderServiceImpl implements OrderService {

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private TransactionRepository transactionRepository;

	private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);

	private static final String SERVICE2_URL = "http://API-Gateway:9090/pay/payments";
	private static final String SERVICE2_TRANSACTIONS_URL = "http://API-Gateway:9090/pay/transactions";

	private OkHttpClient httpClient = new OkHttpClient();

	@Override
	public void placeOrder(Orders orderRequest) {
		Orders existingOrder = orderRepository.findByItem(orderRequest.getItem());

		if (existingOrder == null) {

			logger.error("Item: {} not found for user: {} with CorelationId: {}", orderRequest.getItem(),

					orderRequest.getEmail(), orderRequest.getCorelation_id());
			throw new ItemNotFoundException("Item not found: " + orderRequest.getItem());
		}

		if (existingOrder.getQuantity() >= orderRequest.getQuantity()) {
			existingOrder.setQuantity(existingOrder.getQuantity() - orderRequest.getQuantity());
			orderRepository.save(existingOrder);
			makePaymentRequest(orderRequest);
			logger.info("Order is being placed for user: {} with CorelationId: {}", orderRequest.getEmail(),
					orderRequest.getCorelation_id());
		} else {
			logger.error("Insufficient quantity for user: {} with CorelationId: {}", orderRequest.getEmail(),
					orderRequest.getCorelation_id());
			throw new InsufficientQuantityException("Insufficient quantity for item: " + orderRequest.getItem());
		}
	}

	private void makePaymentRequest(Orders orderRequest) {
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			String requestBody = objectMapper.writeValueAsString(orderRequest);

			MediaType mediaType = MediaType.parse("application/json");
			RequestBody body = RequestBody.create(mediaType, requestBody);
			logger.info("before setting the request");

			Request request = new Request.Builder().url(SERVICE2_URL).post(body)
					.addHeader("Co-relationID", orderRequest.getCorelation_id())
					.addHeader("Authorization", orderRequest.getJwtToken()).build();
			logger.info("Before sending the post call");

			Response response = httpClient.newCall(request).execute();
			logger.info("After post call");
			if (response.isSuccessful()) {
				logger.info("Payment successful for items: {} of user: {} with CorelationId: {}",
						orderRequest.getItem(), orderRequest.getEmail(), orderRequest.getCorelation_id());
				response.close();
				getTransactionDetails(orderRequest);
			} else {
				if (response.code() == HttpStatus.BAD_REQUEST.value()) {

					logger.error("Invalid bank name provided for item: {} for user: {} with CorelationId: {}",
							orderRequest.getItem(), orderRequest.getEmail(), orderRequest.getCorelation_id());
					throw new IllegalArgumentException("Invalid bank name provided");
				} else {
					logger.error("Payment failed for item: {} for user: {} with CorelationId: {}",

							orderRequest.getItem(), orderRequest.getEmail(), orderRequest.getCorelation_id());
					throw new PaymentFailedException("Payment failed for item: " + orderRequest.getItem());
				}
			}
		} catch (IOException e) {

			logger.error("Error occurred during payment:{} for user: {} with CorelationId: {}", e.getMessage(),

					orderRequest.getEmail(), orderRequest.getCorelation_id());
			throw new PaymentFailedException("Error occurred during payment: " + e.getMessage());
		}
	}

	private void getTransactionDetails(Orders orderRequest) {
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			String requestBody = objectMapper.writeValueAsString(orderRequest);

			MediaType mediaType = MediaType.parse("application/json");
			@SuppressWarnings("deprecation")
			RequestBody body = RequestBody.create(mediaType, requestBody);

			Request request = new Request.Builder().url(SERVICE2_TRANSACTIONS_URL).post(body).build();

			Response response = httpClient.newCall(request).execute();
			if (response.isSuccessful()) {
				String responseBody = response.body().string();
				List<Transaction> transactions = objectMapper.readValue(responseBody,
						new TypeReference<List<Transaction>>() {
						});
				System.out.println(transactions);
				transactionRepository.saveAll(transactions);

				logger.info("Successfully retrieved and saved transaction details for user: {} with CorelationId: {}",
						orderRequest.getEmail(), orderRequest.getCorelation_id());
				response.close();
			} else {
				logger.error("Failed to retrieve transaction details for user: {} with CorelationId: {}",
						orderRequest.getEmail(), orderRequest.getCorelation_id());
			}
		} catch (IOException e) {
			logger.error("Error occurred while retrieving transaction details for user: {} with CorelationId: {}",
					orderRequest.getEmail(), orderRequest.getCorelation_id());
		}
	}

	@Override
	public List<Transaction> getAllTransactions() {
		return transactionRepository.findAll();
	}
}
