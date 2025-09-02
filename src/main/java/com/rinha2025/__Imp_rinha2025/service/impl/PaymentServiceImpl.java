package com.rinha2025.__Imp_rinha2025.service.impl;

import com.rinha2025.__Imp_rinha2025.entity.PaymentEntity;
import com.rinha2025.__Imp_rinha2025.model.dto.PaymentRequestDTO;
import com.rinha2025.__Imp_rinha2025.model.dto.PaymentResultsDTO;
import com.rinha2025.__Imp_rinha2025.model.dto.PaymentSummaryResponseDTO;
import com.rinha2025.__Imp_rinha2025.model.projection.PaymentSummaryProjection;
import com.rinha2025.__Imp_rinha2025.repository.PaymentRepository;
import com.rinha2025.__Imp_rinha2025.service.PaymentService;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final DataSource dataSource;
    private final BlockingQueue<String> queue = new LinkedBlockingQueue<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public PaymentServiceImpl(PaymentRepository paymentRepository, DataSource dataSource) {
        this.paymentRepository = paymentRepository;
        this.dataSource = dataSource;
    }

    @Override
    public void processPayment(PaymentRequestDTO requestDTO) {
        // TODO: Convert the requestDTO to JSON and enqueue it for processing
        String amountStr = String.format(Locale.US, "%.2f", requestDTO.amount());

        String paymentJson = "{\"correlationId\":\"" + requestDTO.correlationId() +
                "\",\"amount\":" + amountStr +
                ",\"requestedAt\":\"" + Instant.now().toString() + "\"}";
        enqueuePayment(paymentJson);
    }

    @Override
    public void enqueuePayment(String paymentJson) {
        queue.offer(paymentJson);
    }

    @Override
    public String dequeuePayment() {
        // poll() retorna null imediatamente se a fila estiver vazia,
        // ao invés de esperar como o take().
        return queue.poll();
    }

    @Override
    public void save(PaymentEntity paymentEntity) {
        paymentRepository.save(paymentEntity);
    }

    @Override
    public void saveAll(List<PaymentEntity> payments) {
        if (payments == null || payments.isEmpty()) {
            return;
        }

        // ... constrói a StringBuilder com dados em formato CSV
        StringBuilder csvData = new StringBuilder();
        for (PaymentEntity p : payments) {
            // Formato: correlation_id, amount, created_at, is_default
            csvData.append(p.getCorrelationId()).append("\t")
                    .append(p.getAmount()).append("\t")
                    .append(p.getCreatedAt().toString()).append("\t")
                    .append(p.getDefault()).append("\n");
        }

        try (Connection connection = dataSource.getConnection()) {
            // Desembrulha a conexão do pool para obter a conexão PG real
            BaseConnection baseConnection = connection.unwrap(BaseConnection.class);
            CopyManager copyManager = new CopyManager(baseConnection);
            StringReader reader = new StringReader(csvData.toString());
            copyManager.copyIn("COPY payments (correlation_id, amount, created_at, is_default) FROM STDIN", reader);
        } catch (SQLException | IOException e) {
            // Lançar exceção ou logar. Em um cenário real, trataríamos isso melhor.
            throw new RuntimeException("Erro ao usar COPY para inserir pagamentos", e);
        }
    }

    @Override
    public void drainQueue(List<String> collection, int maxElements) {
        queue.drainTo(collection, maxElements);
    }


    @Override
    public void startProcessing() {
        lock.readLock().lock();
    }

    @Override
    public void endProcessing() {
        lock.readLock().unlock();
    }

    @Override
    public void awaitAllProcessors() {
        // O métodoo de summary agora pede o lock de escrita
        lock.writeLock().lock();
        // A liberação do lock será feita no métodoo getPaymentSummary
    }


    @Override
    public PaymentSummaryResponseDTO getPaymentSummary(Instant from, Instant to) {
        awaitAllProcessors();
        try {
            List<PaymentSummaryProjection> summaryList = paymentRepository.findSummaryByDateRange(from, to);

            PaymentResultsDTO defaultResults = new PaymentResultsDTO(0, 0.0);
            PaymentResultsDTO fallbackResults = new PaymentResultsDTO(0, 0.0);

            for (PaymentSummaryProjection summary : summaryList) {
                if (Boolean.TRUE.equals(summary.getIsDefault())) {
                    defaultResults = new PaymentResultsDTO(summary.getTotalRequests(), summary.getTotalAmount());
                } else {
                    fallbackResults = new PaymentResultsDTO(summary.getTotalRequests(), summary.getTotalAmount());
                }
            }
            return new PaymentSummaryResponseDTO(defaultResults, fallbackResults);
        } finally {
            lock.writeLock().unlock(); // libera o lock
        }
    }
}
