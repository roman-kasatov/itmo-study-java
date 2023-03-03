package info.kgeorgiy.ja.kasatov.walk;

import java.util.Map;


public class RecursiveWalk {

    public static void main(String[] args) {
        try (TaskSolver solver = new TaskSolver(args, Hasher.HashAlgorithms.SHA256.getName())) {
            for (String request : solver) {
                Map<String, String> hashes = solver.getHasher().hashRecursively(request);
                for (Map.Entry<String, String> entry : hashes.entrySet()) {
                    solver.writeAnswer(String.format("%s %s", entry.getValue(), entry.getKey()));
                }
            }
        }
    }
}
