package info.kgeorgiy.ja.korolenko.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class HelloUDPClient implements HelloClient {
    private ExecutorService workExecutor;
    private final int TIME_OUT = 30;

    @Override
    public void run(final String host, final int port, final String prefix, final int threads, final int requests) {
        workExecutor = Executors.newFixedThreadPool(threads);
        final SocketAddress address = new InetSocketAddress(host, port);
        IntStream.range(0, threads).
                forEach( threadNumber -> workExecutor.submit(() -> clientRequest(threadNumber, requests, prefix, address)));
        workExecutor.shutdown();
        try {
            workExecutor.awaitTermination((long) TIME_OUT*threads, TimeUnit.SECONDS);
        } catch (final InterruptedException e) {}
    }

    private void clientRequest(final int threadNumber, final int requests, final String prefix, final SocketAddress address) {
        try (final DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(TIME_OUT);

            int requestNumber = 0;
            boolean isFirstTry = true;
            DatagramPacket respond = null;
            String request = "";
            final byte[] bufResponse = new byte[socket.getReceiveBufferSize()];

            while (!Thread.currentThread().isInterrupted() && requestNumber < requests) {

                if (isFirstTry) {
                    request = prefix + threadNumber + "_" + requestNumber;
                    final byte[] message = request.getBytes(StandardCharsets.UTF_8);
                    respond = new DatagramPacket(message, 0, message.length, address);
                    isFirstTry = false;
                }

                try {
                    final DatagramPacket receive = new DatagramPacket(bufResponse, bufResponse.length);
                    socket.send(respond);
                    socket.receive(receive);

                    final String receiveString = new String(receive.getData(), receive.getOffset(), receive.getLength(), StandardCharsets.UTF_8);
                    if (receiveString.endsWith(request)) {
                        System.out.println(receiveString);
                        requestNumber++;
                        isFirstTry = true;
                    }
                } catch (final IOException e) {
                    System.err.println("Unable to send or receive " + e.getMessage());
                }
            }
        } catch (final SocketException e) {
            System.err.println("Unable to create socket: " + e.getMessage());
        }
    }

    public static void main(final String[] args) {
        if (args == null || args.length != 5) {
            System.err.println("Wrong numbers");
            return;
        }
        try {
            final String host = args[0];
            final int port = Integer.parseInt(args[1]);
            final String prefix = args[2];
            final int threads = Integer.parseInt(args[3]);
            final int requests = Integer.parseInt(args[4]);
            new HelloUDPClient().run(host, port, prefix, threads, requests);
        } catch (final NumberFormatException e){
            System.err.println("Wrong number format of arguments: " + e.getMessage());
        }

    }

}
