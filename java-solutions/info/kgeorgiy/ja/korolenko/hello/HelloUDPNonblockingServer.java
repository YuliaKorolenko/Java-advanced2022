package info.kgeorgiy.ja.korolenko.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HelloUDPNonblockingServer implements HelloServer {

    private Selector selector = null;
    private DatagramChannel channel;
    private final Deque<ServerResponse> responses = new ConcurrentLinkedDeque<>();
    private ExecutorService acceptExecutor;
    private ExecutorService workExecutor;

    private void read(final DatagramChannel channel, final SelectionKey key) {
        try {
            final int bufferSize;
            bufferSize = channel.getOption(StandardSocketOptions.SO_RCVBUF);
            final ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
            final SocketAddress address = channel.receive(buffer);
            workExecutor.submit(() -> {
                final ServerResponse response = new ServerResponse(buffer, address);
                responses.add(response);
                key.interestOps(SelectionKey.OP_WRITE);
                selector.wakeup();
            });
        } catch (final IOException e) {
            System.err.println(e.getMessage());
        }

    }

    private void write(final DatagramChannel channel, final SelectionKey key) {
        if (responses.isEmpty()) {
            key.interestOps(SelectionKey.OP_READ);
            return;
        }
        final ServerResponse response = responses.removeFirst();
        try {
            channel.send(response.answer, response.address);
        } catch (final IOException e) {
            System.err.println(e.getMessage());
        }
        key.interestOpsOr(SelectionKey.OP_READ);
    }

    @Override
    public void start(final int port, final int threads) {
        try {
            selector = Selector.open();
        } catch (final IOException e) {
            System.err.println("Unable to open Selector" + e.getMessage());
        }

        try {
            channel = DatagramChannel.open();
            channel.configureBlocking(false);
            channel.bind(new InetSocketAddress(port));
            channel.register(selector, SelectionKey.OP_READ);
        } catch (final IOException e) {
            System.err.println("Unable to open Channel" + e.getMessage());
        }

        acceptExecutor = Executors.newSingleThreadExecutor();
        workExecutor = Executors.newFixedThreadPool(threads + 1);
        acceptExecutor.submit(() -> {
            while (!Thread.interrupted() && !channel.socket().isClosed()) {
                try {
                    selector.select();
                } catch (final IOException e) {
                    System.err.println(e.getMessage());
                }
                for (final Iterator<SelectionKey> it = selector.selectedKeys().iterator(); it.hasNext(); ){
                    final SelectionKey key = it.next();
                    try {
                        if (key.isReadable() && key.isValid()) {
                            read(channel, key);
                        }
                        if (key.isWritable() && key.isValid()) {
                            write(channel, key);
                        }
                    } finally {
                        it.remove();
                    }
                }
            }
        });

    }

    @Override
    public void close() {
        try {
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
            if (selector != null && selector.isOpen()) {
                selector.close();
            }
        } catch (final IOException e) {
            System.err.println(e.getMessage());
        }
        acceptExecutor.shutdownNow();
        workExecutor.shutdownNow();
        responses.clear();
    }

    public static class ServerResponse {
        ByteBuffer answer;
        SocketAddress address;

        ServerResponse(final ByteBuffer buffer, final SocketAddress address) {
            this.answer = createAnswer(buffer);
            this.address = address;
        }

        private ByteBuffer createAnswer(final ByteBuffer buffer) {
            buffer.flip();
            final String answerString = "Hello, " + new String(buffer.array(), buffer.arrayOffset(), buffer.limit(), StandardCharsets.UTF_8);
            return ByteBuffer.wrap(answerString.getBytes(StandardCharsets.UTF_8));
        }
    }
    public static void main(final String[] args) {
        if (args == null || args.length != 2) {
            System.err.println("Wrong arguments");
            return;
        }
        final HelloUDPNonblockingServer server = new HelloUDPNonblockingServer();
        try {
            final int port = Integer.parseInt(args[0]);
            final int threads = Integer.parseInt(args[1]);
            server.start(port, threads);
        } catch (final NumberFormatException e){
            System.err.println("Unable");
        }
    }

}
