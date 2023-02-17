package info.kgeorgiy.ja.kasatov.walk;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Walk {

    public static void main(String[] args) {

        if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
            System.out.println("Incorrect arguments");
            return;
        }

        try {
            Path inPath = Paths.get(args[0]);
            Path outPath = Paths.get(args[1]);
            try (BufferedReader reader = Files.newBufferedReader(inPath, StandardCharsets.UTF_8)) {
                try (BufferedWriter writer = Files.newBufferedWriter(outPath, StandardCharsets.UTF_8)) {
                    try {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            String hash = Hasher.hash(line);

                            try {
                                writer.write(String.format("%s %s", hash, line));
                                writer.newLine();
                            } catch (IOException e) {
                                System.out.println("Exception occurred while writing to output file: " + e);
                            }
                        }
                    } catch (IOException e) {
                        System.out.println("Exception occurred while reading input file: " + e);
                    }
                } catch (IOException | SecurityException | InvalidPathException e) {
                    System.out.println("Can't open output file: " + e);
                }
            } catch (IOException | SecurityException | InvalidPathException e) {
                System.out.println("Can't open input file: " + e);
            }
        } catch (InvalidPathException e) {
            System.out.println("Wrong arguments: " + e);
        }
    }
}
