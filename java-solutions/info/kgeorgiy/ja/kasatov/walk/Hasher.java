package info.kgeorgiy.ja.kasatov.walk;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class Hasher {
    final private static int HASH_SIZE = 64;

    final private static String nullHash = "0".repeat(HASH_SIZE);

    public static String calculateHash(Path path) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            try (InputStream reader = Files.newInputStream(path)) {
                try {
                    int read;
                    byte[] buffer = new byte[1 << 16];
                    while ((read = reader.read(buffer)) >= 0) {
                        md.update(buffer, 0, read);
                    }

                    // JDK17
                    return HexFormat.of().formatHex(md.digest());

                } catch (IOException e) {
                    return nullHash;
                }
            } catch (IOException | IllegalArgumentException | UnsupportedOperationException |
                     SecurityException e) {
                return nullHash;
            }

        } catch (NoSuchAlgorithmException | NullPointerException e) {
            // Every implementation of the Java platform is required to support
            // the following standard MessageDigest algorithms... (SHA-256 among them)
            return nullHash;
        }
    }

    public static Map<String, String> hashRecursively(String pathString) {
        try (Stream<Path> pathStream = Files.walk(Paths.get(pathString))) {
            return pathStream
                    .filter(f -> {
                        if (f == null) {
                            return false;
                        }
                        try {
                            return !Files.isDirectory(f);
                        } catch (SecurityException e) {
                            return false;
                        }
                    })
                    .collect(Collectors.toMap(Path::toString, Hasher::calculateHash));
        } catch (InvalidPathException | IOException | SecurityException e) {
            // File on 'path' is unreachable
            return Map.of(pathString, nullHash);
        }
    }
}
