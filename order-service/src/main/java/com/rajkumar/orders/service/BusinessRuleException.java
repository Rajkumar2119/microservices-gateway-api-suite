package com.rajkumar.orders.service;

/** The request is well-formed but breaks a business rule (unknown product, invalid status change...). */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
