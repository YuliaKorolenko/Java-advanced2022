package info.kgeorgiy.ja.korolenko.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;

import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class {@code Implementor} create token implementation, compile class, create jar file with token implementation.
 */

public class Implementor implements JarImpler {
    /**
     * INDENT string
     */
    private final static String INDENT = "    ";
    /**
     * defolt line separator in current OS
     */
    private final static String SEPARATOR = System.lineSeparator();

    /**
     * Creates string : "throws" + exception names, separated by comma, if method doesn't have exceptions {@link String} is empty.
     *
     * @param method class {@link Method}.
     * @return name exceptions {@link String}.
     */
    private static String getExceptions(final Method method) {
        return method.getExceptionTypes().length > 0
                ? " throws " + Arrays.stream(method.getExceptionTypes()).map(Class::getCanonicalName)
                .collect(Collectors.joining(","))
                : "";
    }

    /**
     * Create string : parameters type + parameters name, separated by comma.
     *
     * @param parameters class {@link Parameter[]} method parameters (method.getParameters()).
     * @return name parameters {@link String}.
     */
    private static String getParameters(final Parameter[] parameters) {
        return Arrays.stream(parameters)
                .map(s -> s.getType().getCanonicalName() + " " + s.getName())
                .collect(Collectors.joining(" , "));
    }

    /**
     * Create string : "return" + type name of method + ";" .
     *
     * @param type {@link Class} type method (method.getReturnType()).
     * @return method return {@link String}.
     */
    private static String returnType(final Class<?> type) {
        final String answer;
        if (boolean.class.equals(type)) {
            answer = "false";
        } else if (type.equals(void.class)) {
            answer = "";
        } else if (type.isPrimitive()) {
            answer = "0";
        } else {
            answer = "null";
        }
        return INDENT + "return " + answer + ";" + SEPARATOR;
    }

    /**
     * Create string method : type + name + parameters + exceptions.
     *
     * @param token {@link Class}.
     * @return class methods {@link String}.
     */
    private static String writeMethods(final Class<?> token) {
        final StringBuilder allMethods = new StringBuilder();
        for (final Method method : Arrays.stream(token.getMethods()).filter(method -> Modifier.isAbstract(method.getModifiers())).collect(Collectors.toCollection(HashSet::new))) {
            allMethods.append(INDENT).append(" public ").append(method.getReturnType().getCanonicalName()).append(" ").append(method.getName())
                    .append("(").append(getParameters(method.getParameters())).append(")").append(getExceptions(method))
                    .append("{").append(SEPARATOR)
                    .append(returnType(method.getReturnType()))
                    .append("}").append(SEPARATOR);
        }
        return allMethods.toString();
    }

    /**
     * Do unicode string.
     *
     * @param classString {@link String} to unicode.
     * @return unicode {@link String}.
     */
    private String unicode(final String classString) {
        final StringBuilder unicodeString = new StringBuilder();
        for (final char c : classString.toCharArray()) {
            if (c >= 128) {
                unicodeString.append(String.format("\\u%04X", (int) c));
            } else {
                unicodeString.append(c);
            }
        }
        return unicodeString.toString();
    }

    /**
     * Create class string : package + class name + class methods.
     *
     * @param token of {@link Class}.
     * @return {@link Class} {@link String}.
     */
    public String writeClass(final Class<?> token) {
        final StringBuilder clazz = new StringBuilder();
        if (!token.getPackageName().isEmpty()) {
            clazz.append("package ").append(token.getPackageName()).append(";").append(SEPARATOR);
        }
        clazz.append("public class ").append(token.getSimpleName()).append("Impl")
                .append(" implements ").append(token.getCanonicalName());
        clazz.append("{").append(SEPARATOR).append(writeMethods(token)).append("}").append(SEPARATOR);
        return clazz.toString();
    }

    /**
     * Create class with in using information from the token in root directory.
     *
     * @param token type token to create implementation for.
     * @param root  root directory.
     * @throws ImplerException wrong arguments ( token or root ).
     */
    @Override
    public void implement(final Class<?> token, Path root) throws ImplerException {
        if (!token.isInterface() || Modifier.isPrivate(token.getModifiers())) {
            throw new ImplerException("Wrong token");
        }

        root = root.resolve(token.getPackageName().replace('.', File.separatorChar))
                .resolve(token.getSimpleName() + "Impl.java");

        if (root.getParent() != null) {
            try {
                Files.createDirectories(root.getParent());
            } catch (final IOException e) {
                throw new ImplerException(e + "Unable to create parent's directory");
            }
        }

        try (final BufferedWriter writer = Files.newBufferedWriter(root)) {
            writer.write(unicode(writeClass(token)));
        } catch (final IOException e) {
            throw new ImplerException("Unable to write in file", e);
        }
    }

    /**
     * Create implementation of token, if pass {token}.
     * Create .jar with implementation of token, if pass {"-jar"} {token} {root directory}.
     *
     * @param args command line arguments.
     */
    public static void main(final String[] args) {
        try {
            if (args != null && args.length == 1) {
                new Implementor().implement(Class.forName(args[0]), Path.of("."));
            }
            if (args != null && args.length == 3 && "-jar".equals(args[0]) && args[1] != null && args[2] != null) {
                new Implementor().implementJar(Class.forName(args[1]), Path.of(args[2]));
            } else {
                System.out.println("Wrong arguments");
            }
        } catch (final ImplerException e) {
            System.out.println("Unable to create Implementor: " + e.getMessage());
        } catch (final ClassNotFoundException e) {
            System.out.println("ClassNotFoundException : can't find class: " + e.getMessage());
        }
    }

    /**
     * Compile implementation of {@link Class}.
     *
     * @param token type token to create implementation for.
     * @param root  root directory.
     * @throws ImplerException exception if impossible to compile file .
     */
    private static void compile(final Class<?> token, final Path root) throws ImplerException {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        // :NOTE: Пакет по умолчанию
        String simpleName = token.getSimpleName() + "Impl";
        if (!token.getPackageName().isEmpty()) {
            simpleName = "." + simpleName;
        }
        final Path filePath = root.resolve((token.getPackageName() + simpleName)
                .replace(".", File.separator) + ".java").toAbsolutePath();

        if (compiler == null) {
            throw new ImplerException("Could not find java compiler, include tools.jar to classpath");
        }

        String classpath = root + File.pathSeparator;
        try {
            classpath = classpath +
                    Path.of(token.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (final URISyntaxException e) {
            throw new ImplerException(e.getMessage());
        }
        final String[] args = Stream.of(filePath.toString(), "-cp", classpath).toArray(String[]::new);

        final int exitCode = compiler.run(null, null, null, args);
        if (exitCode != 0) {
            throw new ImplerException("Compiler exit code");
        }
    }

    /**
     * Create Jar file with token implementation in root directory for jar file.
     *
     * @param token   type token to create implementation for.
     * @param jarFile root directory for jar file.
     * @param tempDir temporary directory for token implementation.
     * @throws ImplerException can't find temporary directory .
     */
    private void createJar(final Class<?> token, final Path jarFile, final Path tempDir) throws ImplerException {
        final Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        try (final JarOutputStream out = new JarOutputStream(Files.newOutputStream(jarFile), manifest)) {
            final String name = token.getPackageName().replace(".", "/") + "/" + token.getSimpleName() + "Impl.class";
            try {
                out.putNextEntry(new JarEntry(name));
            } catch (final IOException e) {
                throw new ImplerException("Failed put entry in jar file", e);
            }
            final Path classPath = tempDir.resolve(token.getPackageName().replace('.', File.separatorChar))
                    .resolve(token.getSimpleName() + "Impl.class");
            try {
                Files.copy(classPath, out);
            } catch (final IOException e) {
                throw new ImplerException("Failed write class in jar file", e);
            }
        } catch (final FileNotFoundException e) {
            throw new ImplerException("FileNotFound : cannot find temporary directory " + e.getMessage());
        } catch (final IOException e) {
            throw new ImplerException("Failed create jar file", e);
        }
    }

    /**
     * Create token implementation in jar file.
     *
     * @param token   type token to create implementation for.
     * @param jarFile target <var>.jar</var> file.
     * @throws ImplerException throw if impossible create temporary directory.
     */
    @Override
    public void implementJar(final Class<?> token, final Path jarFile) throws ImplerException {
        final Path parentPath = jarFile.getParent();
        if (parentPath == null) {
            throw new ImplerException("Wrong path of jar file");
        }
        final Path classPath;
        try {
            classPath = Files.createTempDirectory(parentPath, "temp");
        } catch (final IOException e) {
            throw new ImplerException("Can't create TempDir" + e.getMessage());
        }

        implement(token, classPath);
        compile(token, classPath);
        createJar(token, jarFile, classPath);
    }
}
