package info.kgeorgiy.ja.korolenko.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HelloUDPServer implements HelloServer {

    private DatagramSocket socket;
    private ExecutorService acceptExecutor;
    private ExecutorService workExecutor;
    private int bufferSize;

    @Override
    public void start(final int port, final int threads) {
        if (socket != null) {
            return;
        }
        try {
            socket = new DatagramSocket(port);
        } catch (final SocketException e) {
            System.err.println("SocketException : " + e.getMessage());
            return;
        }
        try {
            bufferSize = socket.getReceiveBufferSize();
        } catch (final SocketException e) {
            System.err.println("Socket Exception.  Failed : getReceiveBufferSize() " + e.getMessage());
        }

        acceptExecutor = Executors.newSingleThreadExecutor();
        workExecutor = Executors.newFixedThreadPool(threads);
        acceptExecutor.submit(() -> {
                    while (!socket.isClosed() && !Thread.currentThread().isInterrupted()) {
                        try {
                            final DatagramPacket data = new DatagramPacket(new byte[bufferSize], bufferSize);
                            socket.receive(data);
                            workExecutor.submit(() -> {
                                final String answer = "Hello, " + new String(data.getData(), data.getOffset(), data.getLength(), StandardCharsets.UTF_8);
                                final byte[] message = answer.getBytes(StandardCharsets.UTF_8);
                                final DatagramPacket respond = new DatagramPacket(message, message.length, data.getSocketAddress());
                                respond.setData(answer.getBytes(StandardCharsets.UTF_8));
                                try {
                                    socket.send(respond);
                                } catch (final IOException e) {
                                    System.err.println("IOException: Unable to send respond" + e.getMessage());
                                }
                            });
                        } catch (final IOException e) {
                            System.err.println("Unable to receive :  " + e.getMessage());
                        }
                    }
                }
        );
    }

    @Override
    public void close() {
        if (socket != null) {
            socket.close();
        }
        final boolean errorAccept = shutDownAndAwaitTerminator(acceptExecutor);
        final boolean errorWork = shutDownAndAwaitTerminator(workExecutor);
        if (errorAccept || errorWork) {
            Thread.currentThread().interrupt();
        }
    }


    private boolean shutDownAndAwaitTerminator(final ExecutorService executorService) {
        boolean isError = false;
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS))
                    System.err.println("Did not terminate");
            }
        } catch (final InterruptedException ie) {
            executorService.shutdownNow();
            isError = true;
        }
        return isError;
    }

    public static void main(final String[] args) {
        if (args == null || args.length != 2) {
            System.err.println("Wrong arguments");
            return;
        }
        try {
            final int port = Integer.parseInt(args[0]);
            final int threads = Integer.parseInt(args[1]);
            final HelloUDPServer server = new HelloUDPServer();
            server.start(port, threads);
            server.close();
        } catch (final NumberFormatException e){
            System.err.println("Wrong number format of arguments: " + e.getMessage());
        }
    }
}
