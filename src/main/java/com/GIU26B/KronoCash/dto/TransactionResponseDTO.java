package com.GIU26B.KronoCash.dto;

import com.GIU26B.KronoCash.model.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Representación segura de un movimiento para los endpoints de filtrado. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponseDTO {

    private Long id;
    private TransactionType type;
    private String category;
    private BigDecimal amount;
    private String description;
    private LocalDate transactionDate;

    public static TransactionResponseDTO from(com.GIU26B.KronoCash.model.Transaction transaction) {
        return TransactionResponseDTO.builder()
                .id(transaction.getId())
                .type(transaction.getType())
                .category(transaction.getCategory())
                .amount(transaction.getAmount())
                .description(transaction.getDescription())
                .transactionDate(transaction.getTransactionDate())
                .build();
    }
}
