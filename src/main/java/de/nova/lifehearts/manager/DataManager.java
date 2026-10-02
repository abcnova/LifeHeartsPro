package de.nova.lifehearts.manager;

import de.nova.lifehearts.LifeHeartsPlugin;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;

/**
 * Owns persistent YAML state. Mutations stay on the server thread while disk writes are coalesced
 * and performed by one dedicated writer.
 */
public final class DataManager implements AutoCloseable {
    private final LifeHeartsPlugin plugin;
    private final Path dataFile;
    private final ExecutorService writer;
    private YamlConfiguration config;
    private BukkitTask pendingFlush;
    private Future<?> lastWrite;
    private boolean dirty;
    private boolean closed;

    public DataManager(LifeHeartsPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = plugin.getDataFolder().toPath().resolve("data.yml");
        this.writer =
                Executors.newSingleThreadExecutor(
                        runnable -> {
                            Thread thread = new Thread(runnable, "LifeHeartsPro-data-writer");
                            thread.setDaemon(true);
                            return thread;
                        });
        load();
    }

    public void load() {
        assertServerThread();
        if (dirty) {
            flush();
        }
        try {
            Files.createDirectories(dataFile.getParent());
            if (Files.notExists(dataFile)) {
                Files.createFile(dataFile);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not prepare " + dataFile, exception);
        }
        config = YamlConfiguration.loadConfiguration(dataFile.toFile());
        dirty = false;
    }

    /** Marks the in-memory state for one coalesced asynchronous write. */
    public void save() {
        assertServerThread();
        if (closed) {
            throw new IllegalStateException("Data manager is already closed");
        }
        dirty = true;
        if (pendingFlush != null) {
            return;
        }
        long delay =
                Math.max(
                        1L,
                        Math.min(
                                200L,
                                plugin.getConfig().getLong("storage.flush-delay-ticks", 20L)));
        pendingFlush =
                plugin.getServer()
                        .getScheduler()
                        .runTaskLater(
                                plugin,
                                () -> {
                                    pendingFlush = null;
                                    enqueueSnapshot();
                                },
                                delay);
    }

    /** Flushes queued state and waits for the writer. Use during reload and shutdown only. */
    public void flush() {
        assertServerThread();
        if (pendingFlush != null) {
            pendingFlush.cancel();
            pendingFlush = null;
        }
        if (dirty) {
            enqueueSnapshot();
        }
        Future<?> write = lastWrite;
        if (write == null) {
            return;
        }
        try {
            write.get(10, TimeUnit.SECONDS);
        } catch (Exception exception) {
            throw new IllegalStateException("Timed out while saving " + dataFile, exception);
        }
    }

    public YamlConfiguration getConfig() {
        return config;
    }

    public void saveConfiguration(Path target, String snapshot) {
        assertServerThread();
        if (closed) {
            throw new IllegalStateException("Data manager is already closed");
        }
        lastWrite = writer.submit(() -> writeAtomically(target, snapshot));
    }

    private void enqueueSnapshot() {
        assertServerThread();
        if (!dirty) {
            return;
        }
        String snapshot = config.saveToString();
        dirty = false;
        lastWrite = writer.submit(() -> writeAtomically(dataFile, snapshot));
    }

    private void writeAtomically(Path target, String snapshot) {
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        byte[] bytes = snapshot.getBytes(StandardCharsets.UTF_8);
        try (FileChannel channel =
                FileChannel.open(
                        temporary,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE)) {
            channel.write(ByteBuffer.wrap(bytes));
            channel.force(true);
        } catch (IOException exception) {
            plugin.getLogger()
                    .severe(
                            "Could not write temporary file for "
                                    + target.getFileName()
                                    + ": "
                                    + exception.getMessage());
            return;
        }
        try {
            try {
                Files.move(
                        temporary,
                        target,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            plugin.getLogger()
                    .severe(
                            "Could not replace "
                                    + target.getFileName()
                                    + ": "
                                    + exception.getMessage());
        }
    }

    private void assertServerThread() {
        if (!plugin.getServer().isPrimaryThread()) {
            throw new IllegalStateException(
                    "Persistent state must be accessed on the server thread");
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        flush();
        closed = true;
        writer.shutdown();
        try {
            if (!writer.awaitTermination(10, TimeUnit.SECONDS)) {
                writer.shutdownNow();
            }
        } catch (InterruptedException exception) {
            writer.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
