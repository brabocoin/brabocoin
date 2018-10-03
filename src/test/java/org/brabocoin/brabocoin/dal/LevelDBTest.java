package org.brabocoin.brabocoin.dal;

import com.google.protobuf.ByteString;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.logging.Level;

import static org.junit.jupiter.api.Assertions.*;

class LevelDBTest {
    static LevelDB database;
    static String path = "src/test/resources/leveldbtest";
    static File file;

    @BeforeAll
    static void setUp() throws IOException {
        // Setup an empty database
        file = new File(path);
        database = new LevelDB(file);

        database.open();
    }

    @AfterAll
    static void tearDown() throws IOException {
        database.close();

        Files.walkFileTree(Paths.get(path), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    @Test
    void put() {
        try {
            database.put(ByteString.copyFromUtf8("testkey").toByteArray(), ByteString.copyFromUtf8("testvalue").toByteArray());
        } catch (final DatabaseException e) {
            fail(e.getMessage());
        }
    }

    @Test
    void get() {
        final String key = "testkey";
        final String value = "testvalue";
        try {
            database.put(ByteString.copyFromUtf8(key).toByteArray(), ByteString.copyFromUtf8(value).toByteArray());
            final byte[] dbValue = database.get(ByteString.copyFromUtf8(key).toByteArray());
            assertEquals(value, ByteString.copyFrom(dbValue).toStringUtf8());
        } catch (final DatabaseException e) {
            fail(e.getMessage());
        }
    }

    @Test
    void delete() {
        final String key = "testkey";
        final String value = "testvalue";
        try {
            final byte[] byteKey = ByteString.copyFromUtf8(key).toByteArray();
            database.put(byteKey, ByteString.copyFromUtf8(value).toByteArray());
            database.delete(byteKey);

            final byte[] dbValue = database.get(ByteString.copyFromUtf8(key).toByteArray());
            assertEquals(null, dbValue);
        } catch (final DatabaseException e) {
            fail(e.getMessage());
        }
    }
}