package com.yusufkara.homehelper.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record HomeTask(
        UUID id,
        TaskType type,
        String title,
        int quantity,
        LocalDate dueDate,
        boolean completed,
        Instant createdAt) {

    public HomeTask {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(dueDate, "dueDate");
        Objects.requireNonNull(createdAt, "createdAt");

        title = title.trim();
        if (title.isEmpty()) {
            throw new IllegalArgumentException("Title cannot be blank.");
        }
        if (title.length() > 120) {
            throw new IllegalArgumentException("Title cannot exceed 120 characters.");
        }
        if (quantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1.");
        }
        if (type == TaskType.REMINDER && quantity != 1) {
            throw new IllegalArgumentException("A reminder must have quantity 1.");
        }
    }

    public HomeTask markCompleted() {
        return new HomeTask(id, type, title, quantity, dueDate, true, createdAt);
    }
}

