package info.kgeorgiy.ja.kasatov.walk;


public class Walk {

    public static void main(String[] args) {
        try (TaskSolver solver = new TaskSolver(args, Hasher.HashAlgorithms.SHA256.getName())) {
            for (String request : solver) {
                String hash = solver.getHasher().hash(request);
                solver.writeAnswer(String.format("%s %s", hash, request));
            }
        }
    }
}
