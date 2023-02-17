package info.kgeorgiy.ja.kasatov.walk;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import info.kgeorgiy.ja.kasatov.walk.Hasher;


public class RecursiveWalk {

    public static void main(String[] args) {

        try (
                BufferedReader reader = Files.newBufferedReader(Paths.get(args[0]), StandardCharsets.UTF_8);
                BufferedWriter writer = Files.newBufferedWriter(Paths.get(args[1]), StandardCharsets.UTF_8)
        ) {
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    Path path = Paths.get(line);

                    Map<String, String> map = Hasher.hashRecursively(path);

                    try {
                        for (Map.Entry<String, String> entry : map.entrySet()) {
                            writer.write(String.format("%s %s", entry.getValue(), entry.getKey()));
                            writer.newLine();
                        }
                    } catch (IOException e){
                        System.out.println("Exception occurred while writing to output file: " + e);
                    }
                }

            } catch (IOException e) {
                System.out.println("Exception occurred while reading input file: " + e);
            }

        } catch (IOException e) {
            System.out.println("Cant open input/output file: " + e);
        }
    }
}
