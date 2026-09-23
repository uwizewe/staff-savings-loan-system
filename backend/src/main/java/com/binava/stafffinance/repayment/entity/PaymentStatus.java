package com.binava.stafffinance.repayment.entity;

import jakarta.persistence.*;

public enum PaymentStatus { PENDING, PARTIAL, PAID, SETTLED, OVERDUE, REPLACED }
