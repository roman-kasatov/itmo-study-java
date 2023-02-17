package info.kgeorgiy.ja.kasatov.walk;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;


public class RecursiveWalk {

    public static void main(String[] args) {

        if (args.length != 2 || args[0] == null || args[1] == null) {
            System.out.println("Incorrect arguments");
            return;
        }

        try (
                BufferedReader reader = Files.newBufferedReader(Paths.get(args[0]), StandardCharsets.UTF_8);
                BufferedWriter writer = Files.newBufferedWriter(Paths.get(args[1]), StandardCharsets.UTF_8)
        ) {
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    try {
                        Path path = Paths.get(line);

                        Map<String, String> map = Hasher.hashRecursively(path);

                        try {
                            for (Map.Entry<String, String> entry : map.entrySet()) {
                                writer.write(String.format("%s %s", entry.getValue(), entry.getKey()));
                                writer.newLine();
                            }
                        } catch (IOException e) {
                            System.out.println("Exception occurred while writing to output file: " + e);
                        }
                    } catch (InvalidPathException e) {
                        // wrong file path
                    }
                }

            } catch (IOException e) {
                System.out.println("Exception occurred while reading input file: " + e);
            }

        } catch (IOException | SecurityException | InvalidPathException e) {
            System.out.println("Cant open input/output file: " + e);
        }
    }
}
