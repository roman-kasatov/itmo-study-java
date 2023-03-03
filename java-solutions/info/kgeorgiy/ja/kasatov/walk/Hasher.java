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
    private final static int HEX_IN_BYTE = 2;

    // :NOTE: enum? DONE
    enum HashAlgorithms {
        SHA256 {
            public String toString() {
                return "SHA-256";
            }
        }
    }


    private final MessageDigest messageDigest;
    private final String nullHash;

    public Hasher(String algorithm) throws NoSuchAlgorithmException {
        messageDigest = MessageDigest.getInstance(algorithm);
        nullHash = "0".repeat(messageDigest.getDigestLength() * HEX_IN_BYTE);
    }

    public String calculateHash(Path path) {
        messageDigest.reset(); // does nothing
        try (InputStream reader = Files.newInputStream(path)) {
                int read;
                byte[] buffer = new byte[1 << 16];
                while ((read = reader.read(buffer)) >= 0) {
                    messageDigest.update(buffer, 0, read);
                }

                // JDK17
                return HexFormat.of().formatHex(messageDigest.digest());

        } catch (IOException | IllegalArgumentException | UnsupportedOperationException |
                 SecurityException e) {
            return nullHash;
        }
    }

    public Map<String, String> hashRecursively(String pathString) {
        try (Stream<Path> pathStream = Files.walk(Paths.get(pathString))) {
            return pathStream
                    .filter(f -> {
                        try {
                            return !Files.isDirectory(f);
                        } catch (SecurityException e) {
                            return false;
                        }
                    })
                    .collect(Collectors.toMap(Path::toString, this::calculateHash));
        } catch (InvalidPathException | IOException | SecurityException e) {
            // File on 'path' is unreachable
            return Map.of(pathString, nullHash);
        }
    }

    public String hash(String pathString) {
        try {
            Path path = Paths.get(pathString);
            return calculateHash(path);
        } catch (InvalidPathException e) {
            return  nullHash;
        }
    }
}
