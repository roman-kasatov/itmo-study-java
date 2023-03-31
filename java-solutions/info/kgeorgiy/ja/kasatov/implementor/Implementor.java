package info.kgeorgiy.ja.kasatov.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;


import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.*;
import java.lang.reflect.*;
import java.net.URISyntaxException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.*;
import java.util.jar.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Creates class implementation.
 *
 * @author Roman Kasatov (kasatov.r.e@yandex.ru)
 */
public class Implementor implements Impler, JarImpler {
    /** All implemented classes will have this suffix */
    private static final String IMPL = "Impl";
    /** Default java source file extention */
    private static final String SOURCE_FILE_EXTENTION = ".java";
    /** New-line symbol */
    private static final String LINE_SEPARATOR = System.lineSeparator();
    /** Space symbol */
    private static final String SPACE = " ";
    /** Standard indentation = 4 spaces */
    private static final String INDENTATION = "    ";


    /**
     * Creates implementation for abstract class or interface.
     *
     * @param args target class and output folder
     * <p>
     * If two arguments provided:
     * runs {@link #implement(Class, Path)} with first argument as class name and second as
     * folder for output file
     * <p>
     * If three arguments provided and first is -jar:
     * runs {@link #implementJar(Class, Path)} with second argument as class name and third as
     * path to destination .jar file
     * */
    public static void main(String[] args) {
        if (args.length < 2 || Objects.isNull(args[0]) || Objects.isNull(args[1])) {
            System.out.println("Wrong arguments");
            return;
        }

        boolean generateJar = (args.length == 3 && args[0].equals("-jar"));

        String className = args[generateJar ? 1 : 0];
        Class<?> token;
        try {
            token = Class.forName(className);
        } catch (ClassNotFoundException e) {
            System.out.println("Class " + className + " not found: " + e);
            return;
        }

        Implementor implementor = new Implementor();
        try {
            if (generateJar) {
                implementor.implementJar(token, Path.of(args[2]));
            } else {
                implementor.implement(token, Path.of(args[1]));
            }
        } catch (ImplerException e) {
            System.out.println("Error while implementing: " + e);
        }
    }

    /**
     * Generates implementation for abstract class or interface  {@code token}.
     *
     * @param token class to create implementation for.
     * @param root directory where implementation will be created.
     *
     * @throws ImplerException if code can't be generated with {@link #generateCode}
     * or if impossible to write down output file.
     * */
    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {

        Path filePath = root.resolve(
                Path.of(token.getPackageName().replace(".", File.separator),
                        token.getSimpleName() + IMPL + SOURCE_FILE_EXTENTION)
        );
        ensureDirectory(filePath);

        String code = generateCode(token); // passing ImplerException

        try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
            writer.write(code);
        } catch (IOException e) {
            throw new ImplerException(
                    String.format("Can't write to output file %s: ", filePath) + e);
        }
    }

    /**
     * Creates parent directory for {@code path}.
     * @param path File for which the directory created.
     * @throws ImplerException if this directory can't be created.
     * */
    private void ensureDirectory(Path path) throws ImplerException {
        Path root = path.getParent();
        if (root != null) {
            try {
                Files.createDirectories(root);
            } catch (FileAlreadyExistsException e) {
                throw new ImplerException("Can't create directory for output file, " +
                        "such file already exists: " + e);
            } catch (SecurityException e) {
                throw new ImplerException("Can't create directory for output file, " +
                        "due to lack of rights: " + e);
            } catch (IOException e) {
                throw new ImplerException("Can't create directory for output file: " + e);
            }
        }
    }

    /**
     * Generates code of implementation for abstract class or interface by its token.
     *
     * @param token Target class token
     * @return {@code String} with implementation of classNameImpl which
     * extends or implements className class where className is name of {@code token}
     * @throws ImplerException if {@code token} is final or equals {@code Enum.class} or
     * if {@code token} is private.
     * <br>
     * Also passes exception of {@link #generateClass(Class)}
     * */
    private static String generateCode(Class<?> token) throws ImplerException {
        int modifiers = token.getModifiers();
        if (Modifier.isFinal(modifiers) || token.equals(Enum.class)) {
            throw new ImplerException("Can't implement final class " + token.getCanonicalName());
        }

        if (Modifier.isPrivate(modifiers)) {
            throw new ImplerException("Can't implement private class " + token.getCanonicalName());
        }

        return generatePackage(token) + generateClass(token);
    }

    /**
     * Generates source code for class by its token.
     * @param token Target class token
     * @return String with generated code
     * @throws ImplerException If a part of class can't be generated with
     * {@link #generateConstructors(Class)} or {@link #generateMethods(Class)}
     */
    private static String generateClass(Class<?> token) throws ImplerException {
        return "public class " + token.getSimpleName()  + IMPL +
                (token.isInterface() ? " implements " : " extends ") + token.getCanonicalName() + SPACE +
                curlyBrackets(
                        LINE_SEPARATOR + generateConstructors(token) + generateMethods(token)
                );
    }

    /**
     * Generates string with package name like in source code of class.
     * @param token Target class
     * @return empty string for classes in default package and "package PACKAGE_NAME;" for others
     */
    private static String generatePackage(Class<?> token) {
        return (token.getPackageName().equals("")
                ? ""
                : statement("package " + token.getPackageName())
        ) + LINE_SEPARATOR + LINE_SEPARATOR;
    }

    /**
     * Generates code with implementation of all public constructors in class.
     * @param token target class
     * @return empty string if class is interface otherwise a trivial implementation of constructors
     * @throws ImplerException if class is not an interface and has no default constructor or if
     * {@link Class#getDeclaredConstructors()} on {@code token} throws {@link SecurityException}
     * @see #generateMethods
     */
    private static String generateConstructors(Class<?> token) throws ImplerException {
        if (token.isInterface()) return "";

        List<Constructor<?>> constructors = Arrays.stream(token.getDeclaredConstructors())
                .filter(c -> !Modifier.isPrivate(c.getModifiers())).toList();

        if (constructors.size() == 0) {
            throw new ImplerException("No default constructors available in " + token.getCanonicalName());
        }

        return constructors.stream()
                .map(Implementor::generateConstructor)
                .collect(Collectors.joining(LINE_SEPARATOR + LINE_SEPARATOR)) + LINE_SEPARATOR;
    }

    /**
     * Generates implementation of constructor.
     * @param constructor target constructor
     * @return string with correctly formatted (including indentations) trivial implementation of {@code constructor}
     */
    private static String generateConstructor(Constructor<?> constructor) {
        return INDENTATION + "public " + constructor.getDeclaringClass().getSimpleName() + IMPL +
            generateParameters(constructor.getParameters()) +
            generateExceptions(constructor.getExceptionTypes()) +
            curlyBrackets(
                    INDENTATION + INDENTATION +
                Arrays.stream(constructor.getParameters())
                        .map(Parameter::getName)
                        .collect(Collectors.joining(", ", "super(", ");")) +
                        LINE_SEPARATOR + INDENTATION
            );
    }

    /**
     * Builds string with names of {@code exceptions}.
     * @param exceptions list of exceptions to organize
     * @return string with comma separated exception names, empty one for empty {@code exceptions}
     */
    private static String generateExceptions(Class<?>[] exceptions) {
        return exceptions.length == 0 ? "" : Arrays.stream(exceptions)
                .map(Class::getCanonicalName)
                .collect(Collectors.joining(", ", "throws ", SPACE));
    }

    /**
     * Builds string with names of types of {@code parameters}.
     * @param parameters list of parameters to organize
     * @return string with comma separated names of parameter types inside parenthesis
     */
    private static String generateParameters(Parameter[] parameters) {
        return Arrays.stream(parameters)
                .map(p -> p.getType().getCanonicalName() + SPACE + p.getName())
                .collect(Collectors.joining(", ", "(", ")")) + SPACE;
    }

    /**
     * Surrounds a string with curvy brackets.
     * @param str a string to place in brackets
     * @return {@code str} inside brackets with LINE_SEPARATOR after opening ope
     */
    private static String curlyBrackets(String str) {
        return "{" + LINE_SEPARATOR + str +"}";
    }

    /**
     * Generates code with implementation of all abstract methods in class.
     * @param token target class
     * @return code with trivial methods implementation
     * @see #generateConstructors(Class)
     */
    private static String generateMethods(Class<?> token) {
        return getAllAbstractMethods(token).stream()
                .map(MethodImprint::toString)
                .collect(Collectors.joining(LINE_SEPARATOR));
    }

    /**
     * Implements <var>.jar</var> file with compiled class by class token.
     * If there is problem with writing to .jar a message with error will be printed
     * without any exceptions thrown.
     *
     * @param token target class token.
     * @param jarFile target <var>.jar</var> file.
     * @throws ImplerException if can't create temporary file in current working directory,
     * passes exception from {@link #implement(Class, Path)}, if generated code can't be compiled
     */
    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");

        ensureDirectory(jarFile);
        Path tempPath;
        try {
            tempPath = Files.createTempDirectory(jarFile.getParent(), "temp");
            // tempPath = Files.createTempDirectory(Path.of("."), "temp");
        } catch (SecurityException e) {
            System.out.println("Can't create temporary files due to lack of rights: " + e);
            return;
        } catch (IOException e) {
            System.out.println("Can't create temporary files: " + e);
            return;
        }
        try {
            Implementor implementor = new Implementor();
            implementor.implement(token, tempPath); // passing ImplerException

            compileFile(token, tempPath.resolve(
                    Path.of(token.getPackageName().replace(".", File.separator),
                            token.getSimpleName() + IMPL + SOURCE_FILE_EXTENTION)).toString());

            try (JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(jarFile), manifest)) {
                jarOutputStream.putNextEntry(new JarEntry(getInJarPath(token)));
                try {
                    Files.copy(tempPath.resolve(
                            Path.of(token.getPackageName().replace(".", File.separator),
                                    token.getSimpleName() + IMPL + ".class")), jarOutputStream);
                } catch (IOException e) {
                    System.out.println("Can't write file into .jar: " + e);
                } catch (SecurityException e) {
                    System.out.println("Can't write file into .jar due to lack of rights: " + e);
                }
                jarOutputStream.closeEntry();
            } catch (IOException e) {
                System.out.println("Can't open or create target jar file");
            }
        } catch (ImplerException e) {
            throw e; // passing exception from implement and compileFile
        } finally {
            try {
                deleteFolder(tempPath);
            } catch (SecurityException e) {
                System.out.println("Can't get access to temporary files to delete them " + e);
            } catch (IOException e) {
                System.out.println("Can't delete temporary files: " + e);
            }
        }
    }

    /**
     * Removes directory recursively.
     * @param path target directory
     * @throws IOException passed from {@link Files#walk(Path, FileVisitOption...)}
     * @throws SecurityException passed from {@link Files#walk(Path, FileVisitOption...)}
     * or if this exception occurs in {@link File#delete()} for temporary file
     */
    private static void deleteFolder(Path path) throws IOException, SecurityException {
        try (Stream<Path> walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    /**
     * Gives path of file in <var>.jar</var> file.
     * @param token target class
     * @return path as string with delimiter /
     */
    private static String getInJarPath(Class<?> token) {
        return Path.of(token.getPackageName().replace(".", "/"),
            token.getSimpleName() + IMPL + ".class")
                .toString().replace("\\", "/");
    }

    /**
     * Compiles one java class
     * {@code compileFiles} by Georgiy Korneev was used as reference.
     * @param token class to compile - needed to resolve dependencies
     * @param file path to source code
     * @throws ImplerException if there is no java compiler or if an exception occurred
     * in {@link Class#getProtectionDomain()} for {@code token} or if {@link CodeSource#getLocation()}
     * can't be parsed to URI for {@code token} or if {@link JavaCompiler} returned bad exitCode
     */
    public static void compileFile(Class<?> token, final String file) throws ImplerException {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (Objects.isNull(compiler)) {
            throw new ImplerException("Could not find java compiler, include tools.jar to classpath");
        }
        String[] args;
        try {
            CodeSource codeSource = token.getProtectionDomain().getCodeSource();
            if (Objects.isNull(codeSource)) {
                args = new String[] {file};
            } else {
                String classpath = Path.of(codeSource.getLocation().toURI()).toString();
                args = new String[] {"-cp", classpath, file};
            }
        } catch (SecurityException e) {
            throw new ImplerException("Can't get ProtectionDomain to get classpath: " + e);
        } catch (URISyntaxException e) {
            throw new ImplerException("ProtectionDomain of compiled class has incorrect URL: " + e);
        }

        final int exitCode = compiler.run(null, null, null, args);
        if (exitCode != 0) {
            throw new ImplerException("Compilation wasn't successful");
        }
    }

    /**
     * Class for more convenient {@link Method} comparison by {@link Method#getName()}
     * and parameters {@link Method#getParameters()} types.
     */
    private static class MethodImprint {
        /** Method's name */
        String name;
        /** Method's parameters */
        Parameter[] parameters;
        /** Types of method's exceptions */
        Class<?>[] exceptions;
        /** Method's {@code returnType} */
        Class<?> returnType;

        /**
         * Retrieves some fields from {@code method} to store in new {@link MethodImprint} class.
         * @param method method
         */
        public MethodImprint(Method method) {
            name = method.getName();
            parameters = method.getParameters();
            returnType = method.getReturnType();
            exceptions = method.getExceptionTypes();
        }

        public String toString() {
            return "public " + (returnType == null ? "" : returnType.getCanonicalName() + SPACE) +
                    name + generateParameters(parameters) +
                    generateExceptions(exceptions) +
                    generateBody(returnType);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MethodImprint that = (MethodImprint) o;
            return Objects.equals(name, that.name) && parametersEquals(that.parameters);
        }

        /**
         * Checks if all types of {@link MethodImprint#parameters} of this class equals
         * to {@code parametersThat}
         * @param parametersThat parameters to compare
         * @return true if equals and false otherwise
         */
        private boolean parametersEquals(Parameter[] parametersThat) {
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
            return Objects.hash(name);
        }
    }

    /**
     * Gives all abstract methods from class.
     * All methods are distinct in sense of {@link MethodImprint} equality.
     * @param token class to retrieve methods from
     * @return set of {@link MethodImprint} of abstract methods in {@code token}
     */
    private static Set<MethodImprint> getAllAbstractMethods(Class<?> token) {
        Set<MethodImprint> abstractMethods = new HashSet<>();
        Set<MethodImprint> implementedMethods = new HashSet<>();

        while (token != null) {
            Stream.concat(
                    Arrays.stream(token.getMethods()),
                    Arrays.stream(token.getDeclaredMethods())
            ).forEach(method -> {
                if (Modifier.isPrivate(method.getModifiers())) {
                    return;
                }
                MethodImprint methodImprint = new MethodImprint(method);
                if (Modifier.isAbstract(method.getModifiers()) && !implementedMethods.contains(methodImprint)) {
                    abstractMethods.add(methodImprint);
                } else {
                    implementedMethods.add(methodImprint);
                }
            });
            token = token.getSuperclass();
        }
        return abstractMethods;
    }

    /**
     * Places semicolon and {@link #LINE_SEPARATOR} after {@code str}.
     * @param str input string
     * @return statement-like string
     */
    private static String statement(String str) {
        return str + ";" + LINE_SEPARATOR;
    }

    /**
     * Generates trivial code implementation of method body.
     * @param returnType return type of method this implementation for
     * @return void curvy brackets for void {@code returnType} and "return <i>default value</i>"
     * statement inside curvy brackets for others
     */
    private static String generateBody(Class<?> returnType) {
        String value;
        if (returnType == null || returnType.equals(void.class)) {
            return curlyBrackets("") + LINE_SEPARATOR;
        }
        if (returnType.isPrimitive()) {
            value = returnType.equals(boolean.class) ? "false" : "0";
        } else {
            value = "null";
        }
        return curlyBrackets(statement("return " + value)) + LINE_SEPARATOR;
    }
}
