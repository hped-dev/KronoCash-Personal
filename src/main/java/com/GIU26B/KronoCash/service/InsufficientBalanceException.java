package com.GIU26B.KronoCash.service;

import java.math.BigDecimal;

/**
 * Excepción de negocio controlada: un egreso supera el saldo disponible del
 * usuario. El Balance Total nunca debe quedar negativo; el resultado mensual
 * sí puede serlo si se cubre con saldo acumulado de periodos anteriores.
 */
public class InsufficientBalanceException extends RuntimeException {

    private final BigDecimal availableBalance;
    private final BigDecimal attemptedAmount;

    public InsufficientBalanceException(BigDecimal availableBalance, BigDecimal attemptedAmount) {
        super("Saldo insuficiente: el egreso de " + attemptedAmount
                + " supera el balance disponible de " + availableBalance + ".");
        this.availableBalance = availableBalance;
        this.attemptedAmount = attemptedAmount;
    }

    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }

    public BigDecimal getAttemptedAmount() {
        return attemptedAmount;
    }
}
