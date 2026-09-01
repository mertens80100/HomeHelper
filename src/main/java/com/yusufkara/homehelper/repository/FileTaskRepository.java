package com.yusufkara.homehelper.repository;

import com.yusufkara.homehelper.model.HomeTask;
import com.yusufkara.homehelper.model.TaskType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

public final class FileTaskRepository implements TaskRepository {
    private static final String HEADER = "# HomeHelper data v1";

    private final Path dataFile;

    public FileTaskRepository(Path dataFile) {
        this.dataFile = dataFile.toAbsolutePath().normalize();
    }

    @Override
    public List<HomeTask> load() throws IOException {
        if (!Files.exists(dataFile)) {
            return new ArrayList<>();
        }

        List<HomeTask> tasks = new ArrayList<>();
        int lineNumber = 0;
        for (String line : Files.readAllLines(dataFile, StandardCharsets.UTF_8)) {
            lineNumber++;
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            try {
                tasks.add(decode(line));
            } catch (RuntimeException exception) {
                throw new IOException("Invalid data at line " + lineNumber + " in " + dataFile, exception);
            }
        }
        return tasks;
    }

    @Override
    public void save(List<HomeTask> tasks) throws IOException {
        Path parent = dataFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        for (HomeTask task : tasks) {
            lines.add(encode(task));
        }

        Path temporary = dataFile.resolveSibling(dataFile.getFileName() + ".tmp");
        Files.write(temporary, lines, StandardCharsets.UTF_8);
        try {
            Files.move(temporary, dataFile,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporary, dataFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private String encode(HomeTask task) {
        String title = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(task.title().getBytes(StandardCharsets.UTF_8));
        return String.join("\t",
                task.id().toString(),
                task.type().name(),
                title,
                Integer.toString(task.quantity()),
                task.dueDate().toString(),
                Boolean.toString(task.completed()),
                task.createdAt().toString());
    }

    private HomeTask decode(String line) {
        String[] fields = line.split("\t", -1);
        if (fields.length != 7) {
            throw new IllegalArgumentException("Expected 7 fields but found " + fields.length + ".");
        }
        String title = new String(Base64.getUrlDecoder().decode(fields[2]), StandardCharsets.UTF_8);
        return new HomeTask(
                UUID.fromString(fields[0]),
                TaskType.valueOf(fields[1]),
                title,
                Integer.parseInt(fields[3]),
                LocalDate.parse(fields[4]),
                Boolean.parseBoolean(fields[5]),
                Instant.parse(fields[6]));
    }
}

