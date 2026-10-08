package com.financeapp.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Concrete transaction representing money inflows (income).
 */
public class Income extends Transaction {

    public enum Frequency {
        ONE_TIME,
        MONTHLY,
        BI_WEEKLY,
        WEEKLY;

        public static Frequency fromString(String str) {
            if (str == null || str.trim().isEmpty()) {
                return MONTHLY;
            }
            try {
                return Frequency.valueOf(str.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return MONTHLY;
            }
        }
    }

    private String source;
    private Frequency frequency = Frequency.MONTHLY;
    private String notes;

    public Income() {
    }

    public Income(Long id, Long userId, String source, BigDecimal amount,
                  Frequency frequency, LocalDate incomeDate, String notes,
                  LocalDateTime createdAt) {
        super(id, userId, amount, source, incomeDate, createdAt);
        setSource(source);
        setFrequency(frequency);
        setNotes(notes);
    }

    @Override
    public BigDecimal signedAmount() {
        return getAmount() != null ? getAmount() : BigDecimal.ZERO;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        if (source == null || source.trim().isEmpty()) {
            throw new IllegalArgumentException("Income source cannot be empty");
        }
        this.source = source.trim();
        super.setDescription(this.source);
    }

    public Frequency getFrequency() {
        return frequency;
    }

    public void setFrequency(Frequency frequency) {
        this.frequency = frequency != null ? frequency : Frequency.MONTHLY;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes != null ? notes.trim() : "";
    }

    @Override
    public String toString() {
        return "Income{" +
                "id=" + getId() +
                ", userId=" + getUserId() +
                ", source='" + source + '\'' +
                ", amount=" + getAmount() +
                ", frequency=" + frequency +
                ", date=" + getDate() +
                '}';
    }
}
