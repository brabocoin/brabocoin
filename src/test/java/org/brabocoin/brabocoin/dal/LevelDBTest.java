package org.brabocoin.brabocoin.dal;

import com.google.protobuf.ByteString;
import org.brabocoin.brabocoin.exceptions.DatabaseException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

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
            database.put(ByteString.copyFromUtf8("testkey"), ByteString.copyFromUtf8("testvalue"));
        } catch (final DatabaseException e) {
            fail(e.getMessage());
        }
    }

    @Test
    void get() {
        final String key = "testkey";
        final String value = "testvalue";
        try {
            database.put(ByteString.copyFromUtf8(key), ByteString.copyFromUtf8(value));
            final ByteString dbValue = database.get(ByteString.copyFromUtf8(key));
            assertEquals(value, dbValue.toStringUtf8());
        } catch (final DatabaseException e) {
            fail(e.getMessage());
        }
    }

    @Test
    void delete() {
        final String key = "testkey";
        final String value = "testvalue";
        try {
            final ByteString byteKey = ByteString.copyFromUtf8(key);
            database.put(byteKey, ByteString.copyFromUtf8(value));
            database.delete(byteKey);

            final ByteString dbValue = database.get(ByteString.copyFromUtf8(key));
            assertNull(dbValue);
        } catch (final DatabaseException e) {
            fail(e.getMessage());
        }
    }
}