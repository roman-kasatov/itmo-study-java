package info.kgeorgiy.ja.kasatov.walk;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.NoSuchElementException;


public class TaskSolver implements AutoCloseable, Iterable<String> {

    private BufferedReader reader;
    private BufferedWriter writer;
    private Hasher hasher;
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

        // :NOTE: большая вложенность DONE

        Path inPath, outPath;
        try {
            inPath = Paths.get(args[0]);
        } catch (InvalidPathException e) {
            System.out.println("Can't parse input file path: " + e);
            return;
        }
        try {
            outPath = Paths.get(args[1]);
        } catch (InvalidPathException e) {
            System.out.println("Can't parse output file path: " + e);
            return;
        }

        // Make directory for output file
        if (outPath.getParent() != null) {
            boolean outDirectoryFlag = false;
            try {
                Files.createDirectories(outPath.getParent());
                outDirectoryFlag = true;
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
            // :NOTE: можно просто в каждом catch сделать return
            if (!outDirectoryFlag) return;
        }

        // get reader and writer
        boolean readerFlag = false, writerFlag = false;
        try {
            reader = Files.newBufferedReader(inPath, StandardCharsets.UTF_8);
            readerFlag = true;
        } catch (IOException e) {
            System.out.println("Can't open input file: " + e);
        } catch (SecurityException e) {
            System.out.println("Has no rights to open input file: " + e);
        }
        try {
            writer = Files.newBufferedWriter(outPath, StandardCharsets.UTF_8);
            writerFlag = true;
        } catch (IOException e) {
            System.out.println("Can't open output file: " + e);
        } catch (SecurityException e) {
            System.out.println("Has no rights to open output file: " + e);
        }
        if (!readerFlag || !writerFlag) {
            return;
        }

        iterator.start(); // OK
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
        private String savedNextLine;

        @Override
        public boolean hasNext() {
            if (stopFlag) {
                return false;
            } else if (savedNextLine == null) {
                savedNextLine = readNextLine();
            }
            return !(savedNextLine == null);
        }

        // :NOTE: NoSuchElementException
        @Override
        public String next() {
            if (hasNext()) {
                String ret = savedNextLine;
                savedNextLine = null;
                return ret;
            } else {
                throw new NoSuchElementException();
            }
        }

        private void start() {
            stopFlag = false;
        }
        // setStopFlag() DONE
    }

    @Override
    public Iterator<String> iterator() {
        return iterator;
    }
}
