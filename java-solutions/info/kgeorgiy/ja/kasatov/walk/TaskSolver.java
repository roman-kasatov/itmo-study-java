package info.kgeorgiy.ja.kasatov.walk;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;


public class TaskSolver implements AutoCloseable, Iterable<String> {

    private BufferedReader reader;
    private BufferedWriter writer;
    private Hasher hasher;
    private String savedNextLine;
    private final TaskSolverIterator iterator;

    public Hasher getHasher() {
        return hasher;
    }

    public void writeAnswer(String string) {
        try {
            writer.write(string);
            writer.newLine();
        } catch (IOException e) {
            System.out.println("Exception occurred while writing to output file: " + e);
        }
    }

    TaskSolver(String[] args, String hashAlgorithm) {
        iterator = new TaskSolverIterator();

        if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
            System.out.println("Wrong arguments");
            return;
        }

        try {
            hasher = new Hasher(hashAlgorithm);
        } catch (NoSuchAlgorithmException e) {
            System.out.println(String.format("Hash algorithm %s does not exist: ", hashAlgorithm) + e);
            return;
        }

        try {
            Path inPath = Paths.get(args[0]);
            try {
                Path outPath = Paths.get(args[1]);
                try {
                    if (outPath.getParent() != null) {
                        Files.createDirectories(outPath.getParent());
                    }
                    try {
                        reader = Files.newBufferedReader(inPath, StandardCharsets.UTF_8);
                        try {
                            writer = Files.newBufferedWriter(outPath, StandardCharsets.UTF_8);
                            iterator.stopFlag = false; // OK
                        } catch (IOException e) {
                            System.out.println("Can't open output file: " + e);
                        } catch (SecurityException e) {
                            System.out.println("Has no rights to open output file: " + e);
                        }
                    } catch (IOException e) {
                        System.out.println("Can't open input file: " + e);
                    } catch (SecurityException e) {
                        System.out.println("Has no rights to open input file: " + e);
                    }


                } catch (UnsupportedOperationException e) {
                    // Impossible exception while createDirectories() has no attributes
                } catch (FileAlreadyExistsException e) {
                    System.out.println("Output file can't be created because its path contains " +
                            "non-folder files: " + e);
                } catch (IOException e) {
                    System.out.println("Can't create output file: " + e);
                } catch (SecurityException e) {
                    System.out.println("Has no rights to create output file: " + e);
                }
            } catch (InvalidPathException e) {
                System.out.println("Can't parse output file path: " + e);
            }
        } catch (InvalidPathException e) {
            System.out.println("Can't parse input file path: " + e);
        }
    }

    @Override
    public void close() {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                System.out.println("Can't close input file: " + e);
            }
        }

        if (writer != null) {
            try {
                writer.close();
            } catch (IOException e) {
                System.out.println("Can't close output file: " + e);
            }
        }
    }

    private String readNextLine() {
        try {
            return reader.readLine();
        } catch (IOException e) {
            System.out.println("Exception while reading from input file: " + e);
            return null;
        }
    }

    private class TaskSolverIterator implements Iterator<String> {
        private boolean stopFlag = true;

        @Override
        public boolean hasNext() {
            if (stopFlag) {
                return false;
            }
            savedNextLine = readNextLine();
            return !(savedNextLine == null);
        }

        @Override
        public String next() {
            if (savedNextLine == null) {
                return readNextLine();
            } else {
                String ret = savedNextLine;
                savedNextLine = null;
                return ret;
            }
        }
    }

    @Override
    public Iterator<String> iterator() {
        return iterator;
    }
}
