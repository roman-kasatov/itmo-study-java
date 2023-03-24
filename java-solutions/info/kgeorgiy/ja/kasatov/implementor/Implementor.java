package info.kgeorgiy.ja.kasatov.implementor;


import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;


import java.io.*;
import java.lang.reflect.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class Implementor implements Impler {
    private static final String IMPL = "Impl";
    private static final String FILE_SUFFIX = IMPL + ".java";
    private static final String LINE_SEPARATOR = System.lineSeparator();
    private static final String VOID_BODY = "{}"; // :NOTE: code format

    @Override
    public void implement(final Class<?> token, final Path root) throws ImplerException {
        final int modifiers = token.getModifiers();
        if (Modifier.isFinal(modifiers) || token.equals(Enum.class)) {
            throw new ImplerException("Can't implement final class " + token.getCanonicalName());
        }

        if (Modifier.isPrivate(modifiers)) {
            throw new ImplerException("Can't implement private class " + token.getCanonicalName());
        }

        final Path filePath = root.resolve(
                Path.of(token.getPackageName().replace(".", File.separator),
                        token.getSimpleName() + FILE_SUFFIX)
        );
        ensureDirectory(filePath);

        try (final BufferedWriter writer = Files.newBufferedWriter(filePath)) {
            writer.write(generateCode(token));
        } catch (final IOException e) {
            throw new ImplerException(
                    String.format("Can't write to output file %s: ", filePath) + e);
        }

    }

    private static void ensureDirectory(final Path path) throws ImplerException {
        final Path root = path.getParent();
        if (root != null) {
            try {
                Files.createDirectories(root);
            } catch (final IOException e) {
                // :NOTE: ??
                throw new ImplerException("Can't create directory for output file: " + e);
            }
        }
    }

    private String generateCode(final Class<?> token) throws ImplerException {
        return ("".equals(token.getPackageName())
                ? ""
                : statement("package " + token.getPackageName())
        ) + generateClass(token);
    }

    private String generateClass(final Class<?> token) throws ImplerException {
        return "public class " + token.getSimpleName()  + IMPL +
                (token.isInterface() ? " implements " : " extends ") + token.getCanonicalName() +
                curlyBrackets(
                        generateConstructors(token) + generateMethods(token)
                );
    }

    private String generateConstructors(final Class<?> token) throws ImplerException {
        if (token.isInterface()) return "";
        final String constructors = Arrays.stream(token.getDeclaredConstructors())
                .filter(c -> !Modifier.isPrivate(c.getModifiers()))
                .map(this::generateConstructor)
                .collect(Collectors.joining(LINE_SEPARATOR));
        // :NOTE: too late
        if (constructors.equals("")) {
            throw new ImplerException("No default constructors available in " + token.getCanonicalName());
        }
        return constructors;
    }

    private String generateConstructor(final Constructor<?> constructor) {
        return "public " + constructor.getDeclaringClass().getSimpleName() + IMPL +
            generateParameters(constructor.getParameters()) +
            generateExceptions(constructor.getExceptionTypes()) +
            curlyBrackets(
                Arrays.stream(constructor.getParameters())
                        .map(Parameter::getName)
                        .collect(Collectors.joining(",", "super(", ");")) +
                        LINE_SEPARATOR
            );
    }

    private static String generateExceptions(final Class<?>[] exceptions) {
        return exceptions.length == 0 ? "" : Arrays.stream(exceptions)
                .map(Class::getCanonicalName)
                .collect(Collectors.joining(", ", "throws ", " "));
    }

    private static String generateParameters(final Parameter[] parameters) {
        return Arrays.stream(parameters)
                .map(p -> p.getType().getCanonicalName() + " " + p.getName())
                .collect(Collectors.joining(", ", "(", ") "));
    }

    private static String curlyBrackets(final String str) {
        return "{" + LINE_SEPARATOR + str + "}" + LINE_SEPARATOR;
    }

    private String generateMethods(final Class<?> token) {
        return getAllMethods(token).stream()
                .map(MethodAsList::toString)
                .collect(Collectors.joining(LINE_SEPARATOR));
    }

    private static class MethodAsList { // :NOTE: naming
        String name;
        Parameter[] parameters;
        Class<?>[] exceptions;
        Class<?> returnType;
        int modifiers;

        public MethodAsList(final Method method) {
            name = method.getName();
            parameters = method.getParameters();
            returnType = method.getReturnType();
            exceptions = method.getExceptionTypes();
            modifiers = method.getModifiers();
        }

        public int getModifiers() {
            return modifiers;
        }

        @Override
        public String toString() {
            return "public " + (returnType == null ? "" : returnType.getCanonicalName() + " ") +
                    name + generateParameters(parameters) +
                    generateExceptions(exceptions) +
                    generateBody(returnType);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final MethodAsList that = (MethodAsList) o;
            return Objects.equals(name, that.name) && parametersEquals(that.parameters);
        }

        private boolean parametersEquals(final Parameter[] parametersThat) {
            if (parameters.length != parametersThat.length) return false;
            for (int i = 0; i < parameters.length; i++) {
                if (parameters[i].getType() != parametersThat[i].getType()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, returnType);
        }
    }

    private List<MethodAsList> getAllMethods(final Class<?> token) {
        // :NOTE: recursion
        return Stream.generate(new Supplier<Stream<MethodAsList>>() {
            Class<?> token;

            public Supplier<Stream<MethodAsList>> init(final Class<?> token) {
                this.token = token;
                return this;
            }

            @Override
            public Stream<MethodAsList> get() {
                if (Objects.isNull(token)) {
                    return null;
                }
                final Stream<MethodAsList> ret = Stream.concat(
                        Arrays.stream(token.getMethods()),
                        Arrays.stream(token.getDeclaredMethods())
                        ).map(MethodAsList::new);
                token = token.getSuperclass();
                return ret;
            }
        }.init(token))
                .takeWhile(Objects::nonNull)
                .flatMap(Function.identity())
                .filter(m -> Modifier.isAbstract(m.getModifiers()) && !Modifier.isPrivate(m.getModifiers()))
                .distinct()
                .toList();
    }

    private static String statement(final String str) {
        return str + ";" + LINE_SEPARATOR;
    }

    private static String generateBody(final Class<?> returnType) {
        final String value;
        if (returnType == null || returnType.equals(void.class)) {
            return VOID_BODY + LINE_SEPARATOR;
        }
        if (returnType.isPrimitive()) {
            value = returnType.equals(boolean.class) ? "false" : "0";
        } else {
            value = "null";
        }
        return curlyBrackets(statement("return " + value));
    }
}
