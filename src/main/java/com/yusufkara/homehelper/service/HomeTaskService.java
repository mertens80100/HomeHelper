package com.yusufkara.homehelper.service;

import com.yusufkara.homehelper.model.HomeTask;
import com.yusufkara.homehelper.model.TaskType;
import com.yusufkara.homehelper.repository.TaskRepository;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class HomeTaskService {
    private static final Comparator<HomeTask> DISPLAY_ORDER = Comparator
            .comparing(HomeTask::completed)
            .thenComparing(HomeTask::dueDate)
            .thenComparing(HomeTask::createdAt);

    private final TaskRepository repository;
    private final Clock clock;
    private final List<HomeTask> tasks;

    public HomeTaskService(TaskRepository repository, Clock clock) throws IOException {
        this.repository = repository;
        this.clock = clock;
        this.tasks = new ArrayList<>(repository.load());
    }

    public HomeTask addReminder(String title, LocalDate dueDate) throws IOException {
        return add(TaskType.REMINDER, title, 1, dueDate);
    }

    public HomeTask addShoppingItem(String title, int quantity, LocalDate dueDate) throws IOException {
        return add(TaskType.SHOPPING, title, quantity, dueDate);
    }

    public List<HomeTask> listAll() {
        return tasks.stream().sorted(DISPLAY_ORDER).toList();
    }

    public List<HomeTask> listDueBy(LocalDate date) {
        return tasks.stream()
                .filter(task -> !task.completed())
                .filter(task -> !task.dueDate().isAfter(date))
                .sorted(DISPLAY_ORDER)
                .toList();
    }

    public HomeTask complete(String idPrefix) throws IOException {
        HomeTask target = findUnique(idPrefix);
        if (target.completed()) {
            return target;
        }
        HomeTask completed = target.markCompleted();
        tasks.set(tasks.indexOf(target), completed);
        persist();
        return completed;
    }

    public HomeTask remove(String idPrefix) throws IOException {
        HomeTask target = findUnique(idPrefix);
        tasks.remove(target);
        persist();
        return target;
    }

    public Summary summary() {
        long openReminders = tasks.stream()
                .filter(task -> task.type() == TaskType.REMINDER && !task.completed())
                .count();
        long shoppingItems = tasks.stream()
                .filter(task -> task.type() == TaskType.SHOPPING && !task.completed())
                .count();
        return new Summary(openReminders, shoppingItems);
    }

    private HomeTask add(TaskType type, String title, int quantity, LocalDate dueDate) throws IOException {
        HomeTask task = new HomeTask(
                UUID.randomUUID(),
                type,
                title,
                quantity,
                dueDate,
                false,
                Instant.now(clock));
        tasks.add(task);
        persist();
        return task;
    }

    private HomeTask findUnique(String idPrefix) {
        String normalized = idPrefix == null ? "" : idPrefix.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() < 4) {
            throw new IllegalArgumentException("Use at least 4 characters from the task ID.");
        }

        List<HomeTask> matches = tasks.stream()
                .filter(task -> task.id().toString().toLowerCase(Locale.ROOT).startsWith(normalized))
                .toList();
        if (matches.isEmpty()) {
            throw new IllegalArgumentException("No task matches ID prefix: " + idPrefix);
        }
        if (matches.size() > 1) {
            throw new IllegalArgumentException("ID prefix is ambiguous; enter more characters.");
        }
        return matches.get(0);
    }

    private void persist() throws IOException {
        repository.save(List.copyOf(tasks));
    }

    public record Summary(long openReminders, long openShoppingItems) {
    }
}

