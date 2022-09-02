package info.kgeorgiy.ja.korolenko.walk;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class Walk {

    static final String zeroString="0".repeat(40);

    public static String getHash(final Path path, final MessageDigest messageDigest) {
        try (final InputStream r = Files.newInputStream(path)) {
            final byte[] buffer = new byte[4 * 1024];
            int read = 0;

            while ((read = r.read(buffer)) > 0) {
                messageDigest.update(buffer, 0, read);
            }
            final String sha1 = HexFormat.of().formatHex(messageDigest.digest());
            return sha1;
        } catch (final IOException e) {
            return zeroString;
        }
    }

    public static void main(final String[] args) throws IOException {

        if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
            return;
        }

        final Path inPath;
        final Path outPath;
        try {
            inPath = Path.of(args[0]);
        } catch (final InvalidPathException e) {
            // :NOTE: разделить input output
            System.err.println(e.getMessage() + "Can't read path of input file");
            return;
        }

        try {
            outPath = Path.of(args[1]);
        } catch (final InvalidPathException e) {
            System.err.println(e.getMessage() + "Can't read path of output file");
            return;
        }

        // :NOTE: второе условие не нужно
        //FileAlreadyExistsException
        if (outPath.getParent() != null && !(Files.exists(outPath.getParent()))) {
            Files.createDirectories(outPath.getParent());
        }


        if (!(Files.exists(inPath))) {
            System.err.println("Input file doesn't exist");
            return;
        }


        try (
                final BufferedReader bufferedReader = Files.newBufferedReader(inPath);
                final BufferedWriter bufferedWriter = Files.newBufferedWriter(outPath)
        ) {

            try {
                final MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    try {
                        final Path path = Paths.get(line);
                        final String hash = getHash(path, messageDigest);
                        bufferedWriter.write(hash + " " + line + System.lineSeparator());
                    } catch (final InvalidPathException e) {
                        bufferedWriter.write(zeroString + " " + line + System.lineSeparator());
                    }
                }
            } catch (final NoSuchAlgorithmException e) {
                System.err.println("Sha-1 is not a valid message digest algorithm");
            }

        } catch (final InvalidPathException | IOException e) {
            System.err.println(e.getMessage());
        }


    }
}
