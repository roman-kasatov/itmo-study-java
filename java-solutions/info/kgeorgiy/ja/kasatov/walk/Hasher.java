package info.kgeorgiy.ja.kasatov.walk;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class Hasher {
    final static String nullHash = "0".repeat(40);

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

    public static Map<String, String> hashRecursively(Path path) {
        try (Stream<Path> pathStream = Files.walk(path)) {
            return pathStream
            //        .filter(Objects::nonNull)
                    .filter(f -> !Files.isDirectory(f))
                    .collect(Collectors.toMap(Path::toString, Hasher::calculateHash));
        } catch (IOException | SecurityException e) {
            // File on 'path' is unreachable
            return new HashMap<>();
        }
    }
}
