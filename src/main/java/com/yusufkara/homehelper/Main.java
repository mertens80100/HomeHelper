package com.yusufkara.homehelper;

import com.yusufkara.homehelper.model.HomeTask;
import com.yusufkara.homehelper.repository.FileTaskRepository;
import com.yusufkara.homehelper.service.HomeTaskService;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        Path dataFile = Path.of("data", "home-helper.db");
        try {
            HomeTaskService service = new HomeTaskService(new FileTaskRepository(dataFile), Clock.systemDefaultZone());
            runShell(service);
        } catch (IOException exception) {
            System.err.println("Could not open the local data file: " + exception.getMessage());
            System.exit(1);
        }
    }

    private static void runShell(HomeTaskService service) {
        System.out.println("HomeHelper - daily reminders and shopping list");
        printHelp();

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                HomeTaskService.Summary summary = service.summary();
                System.out.printf("%n[%d reminders, %d shopping items] > ",
                        summary.openReminders(), summary.openShoppingItems());

                if (!scanner.hasNextLine()) {
                    break;
                }
                String input = scanner.nextLine().trim();
                if (input.isEmpty()) {
                    continue;
                }
                if (input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("quit")) {
                    break;
                }

                try {
                    handle(service, tokenize(input));
                } catch (IllegalArgumentException | IOException exception) {
                    System.out.println("Error: " + exception.getMessage());
                }
            }
        }
        System.out.println("Goodbye.");
    }

    private static void handle(HomeTaskService service, List<String> tokens) throws IOException {
        String command = tokens.get(0).toLowerCase();
        switch (command) {
            case "reminder" -> addReminder(service, tokens);
            case "shopping" -> addShopping(service, tokens);
            case "list" -> printTasks(service.listAll());
            case "today" -> printTasks(service.listDueBy(LocalDate.now()));
            case "done" -> {
                requireSize(tokens, 2, "done <task-id>");
                HomeTask task = service.complete(tokens.get(1));
                System.out.println("Completed: " + task.title());
            }
            case "remove" -> {
                requireSize(tokens, 2, "remove <task-id>");
                HomeTask task = service.remove(tokens.get(1));
                System.out.println("Removed: " + task.title());
            }
            case "help" -> printHelp();
            default -> throw new IllegalArgumentException("Unknown command. Type 'help' to see commands.");
        }
    }

    private static void addReminder(HomeTaskService service, List<String> tokens) throws IOException {
        requireSize(tokens, 4, "reminder add <YYYY-MM-DD> \"title\"");
        if (!tokens.get(1).equalsIgnoreCase("add")) {
            throw new IllegalArgumentException("Use: reminder add <YYYY-MM-DD> \"title\"");
        }
        LocalDate date = parseDate(tokens.get(2));
        HomeTask task = service.addReminder(tokens.get(3), date);
        System.out.println("Added reminder with ID " + shortId(task));
    }

    private static void addShopping(HomeTaskService service, List<String> tokens) throws IOException {
        requireSize(tokens, 5, "shopping add <YYYY-MM-DD> <quantity> \"item\"");
        if (!tokens.get(1).equalsIgnoreCase("add")) {
            throw new IllegalArgumentException("Use: shopping add <YYYY-MM-DD> <quantity> \"item\"");
        }
        LocalDate date = parseDate(tokens.get(2));
        int quantity;
        try {
            quantity = Integer.parseInt(tokens.get(3));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Quantity must be a whole number.");
        }
        HomeTask task = service.addShoppingItem(tokens.get(4), quantity, date);
        System.out.println("Added shopping item with ID " + shortId(task));
    }

    private static LocalDate parseDate(String text) {
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Date must use YYYY-MM-DD format.");
        }
    }

    private static void printTasks(List<HomeTask> tasks) {
        if (tasks.isEmpty()) {
            System.out.println("No matching tasks.");
            return;
        }
        System.out.printf("%-8s  %-9s  %-10s  %-5s  %s%n", "ID", "TYPE", "DUE", "QTY", "TITLE");
        for (HomeTask task : tasks) {
            String marker = task.completed() ? "[done] " : "";
            System.out.printf("%-8s  %-9s  %-10s  %-5d  %s%s%n",
                    shortId(task), task.type(), task.dueDate(), task.quantity(), marker, task.title());
        }
    }

    private static String shortId(HomeTask task) {
        return task.id().toString().substring(0, 8);
    }

    private static void requireSize(List<String> tokens, int expected, String usage) {
        if (tokens.size() != expected) {
            throw new IllegalArgumentException("Use: " + usage);
        }
    }

    static List<String> tokenize(String input) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        boolean escaping = false;

        for (char character : input.toCharArray()) {
            if (escaping) {
                current.append(character);
                escaping = false;
            } else if (character == '\\') {
                escaping = true;
            } else if (character == '"') {
                quoted = !quoted;
            } else if (Character.isWhitespace(character) && !quoted) {
                if (!current.isEmpty()) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(character);
            }
        }

        if (escaping || quoted) {
            throw new IllegalArgumentException("Unclosed quote or escape sequence.");
        }
        if (!current.isEmpty()) {
            tokens.add(current.toString());
        }
        if (tokens.isEmpty()) {
            throw new IllegalArgumentException("Command cannot be blank.");
        }
        return tokens;
    }

    private static void printHelp() {
        System.out.println("""
                Commands:
                  reminder add <YYYY-MM-DD> "title"
                  shopping add <YYYY-MM-DD> <quantity> "item"
                  list
                  today
                  done <task-id>
                  remove <task-id>
                  help
                  exit
                """);
    }
}

