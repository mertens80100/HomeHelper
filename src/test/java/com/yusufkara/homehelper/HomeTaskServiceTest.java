package com.yusufkara.homehelper;

import com.yusufkara.homehelper.model.HomeTask;
import com.yusufkara.homehelper.model.TaskType;
import com.yusufkara.homehelper.repository.FileTaskRepository;
import com.yusufkara.homehelper.repository.TaskRepository;
import com.yusufkara.homehelper.service.HomeTaskService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

public final class HomeTaskServiceTest {
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-09-01T09:00:00Z"), ZoneOffset.UTC);

    private int passed;

    public static void main(String[] args) throws Exception {
        HomeTaskServiceTest suite = new HomeTaskServiceTest();
        suite.run("adds and lists tasks", suite::addsAndListsTasks);
        suite.run("filters due tasks", suite::filtersDueTasks);
        suite.run("completes and removes by ID prefix", suite::completesAndRemoves);
        suite.run("validates task input", suite::validatesInput);
        suite.run("persists a UTF-8 round trip", suite::persistsRoundTrip);
        suite.run("tokenizes quoted commands", suite::tokenizesCommands);
        System.out.println("Passed " + suite.passed + " tests.");
    }

    private void addsAndListsTasks() throws Exception {
        HomeTaskService service = service();
        service.addShoppingItem("Milk", 2, LocalDate.of(2026, 9, 2));
        service.addReminder("Call mom", LocalDate.of(2026, 9, 1));

        List<HomeTask> tasks = service.listAll();
        equal(2, tasks.size());
        equal("Call mom", tasks.get(0).title());
        equal(TaskType.REMINDER, tasks.get(0).type());
        equal(1L, service.summary().openReminders());
        equal(1L, service.summary().openShoppingItems());
    }

    private void filtersDueTasks() throws Exception {
        HomeTaskService service = service();
        service.addReminder("Overdue", LocalDate.of(2026, 8, 31));
        service.addReminder("Today", LocalDate.of(2026, 9, 1));
        service.addReminder("Future", LocalDate.of(2026, 9, 2));
        equal(2, service.listDueBy(LocalDate.of(2026, 9, 1)).size());
    }

    private void completesAndRemoves() throws Exception {
        HomeTaskService service = service();
        HomeTask first = service.addReminder("Medicine", LocalDate.of(2026, 9, 1));
        HomeTask second = service.addShoppingItem("Bread", 1, LocalDate.of(2026, 9, 1));

        HomeTask completed = service.complete(first.id().toString().substring(0, 8));
        check(completed.completed(), "Task should be completed.");
        equal(1L, service.summary().openShoppingItems());

        HomeTask removed = service.remove(second.id().toString().substring(0, 8));
        equal("Bread", removed.title());
        equal(1, service.listAll().size());
    }

    private void validatesInput() throws Exception {
        HomeTaskService service = service();
        expectFailure(() -> service.addReminder(" ", LocalDate.of(2026, 9, 1)));
        expectFailure(() -> service.addShoppingItem("Milk", 0, LocalDate.of(2026, 9, 1)));
        expectFailure(() -> service.complete("abc"));
    }

    private void persistsRoundTrip() throws Exception {
        Path directory = Files.createTempDirectory("home-helper-test-");
        Path dataFile = directory.resolve("tasks.db");
        try {
            HomeTaskService writer = new HomeTaskService(new FileTaskRepository(dataFile), FIXED_CLOCK);
            writer.addShoppingItem("Süt ve ekmek", 3, LocalDate.of(2026, 9, 3));

            HomeTaskService reader = new HomeTaskService(new FileTaskRepository(dataFile), FIXED_CLOCK);
            equal(1, reader.listAll().size());
            equal("Süt ve ekmek", reader.listAll().get(0).title());
            equal(3, reader.listAll().get(0).quantity());
        } finally {
            Files.deleteIfExists(dataFile);
            Files.deleteIfExists(directory);
        }
    }

    private void tokenizesCommands() {
        equal(
                List.of("shopping", "add", "2026-09-02", "2", "olive oil"),
                Main.tokenize("shopping add 2026-09-02 2 \"olive oil\""));
        expectFailure(() -> Main.tokenize("reminder add 2026-09-01 \"unfinished"));
    }

    private HomeTaskService service() throws IOException {
        return new HomeTaskService(new InMemoryRepository(), FIXED_CLOCK);
    }

    private void run(String name, CheckedRunnable test) throws Exception {
        try {
            test.run();
            passed++;
            System.out.println("PASS: " + name);
        } catch (Throwable throwable) {
            System.err.println("FAIL: " + name);
            throw throwable;
        }
    }

    private static void equal(Object expected, Object actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("Expected " + expected + " but got " + actual);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void expectFailure(CheckedRunnable action) {
        try {
            action.run();
        } catch (Exception expected) {
            return;
        }
        throw new AssertionError("Expected an exception.");
    }

    @FunctionalInterface
    private interface CheckedRunnable {
        void run() throws Exception;
    }

    private static final class InMemoryRepository implements TaskRepository {
        private List<HomeTask> tasks = new ArrayList<>();

        @Override
        public List<HomeTask> load() {
            return new ArrayList<>(tasks);
        }

        @Override
        public void save(List<HomeTask> tasks) {
            this.tasks = new ArrayList<>(tasks);
        }
    }
}

