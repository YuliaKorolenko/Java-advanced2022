package info.kgeorgiy.ja.korolenko.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class HelloUDPNonblockingClient implements HelloClient {

    private final int TIME_OUT = 30;

    private void write(final SelectionKey key, final String prefix) {
        final DatagramChannel channel = (DatagramChannel) key.channel();
        final Information information = (Information) key.attachment();

        final ByteBuffer buffer = information.buffer;
        buffer.clear();
        final String request = information.createString(prefix);
        buffer.put(request.getBytes(StandardCharsets.UTF_8));
        buffer.flip();
        try {
            channel.write(buffer);
        } catch (final IOException e) {
            System.err.println("Unable to write"+e.getMessage());
        }
        key.interestOps(SelectionKey.OP_READ);
    }

    private void read(final SelectionKey key, final String prefix) {
        final DatagramChannel channel = (DatagramChannel) key.channel();
        final Information information = (Information) key.attachment();

        try {
            final ByteBuffer buffer = information.buffer;
            buffer.clear();
            channel.receive(buffer);
            final String response = StandardCharsets.UTF_8.
                    decode(information.buffer.flip()).toString();
            if (response.contains(information.createString(prefix))) {
                information.addRequestNumber();
            }
            key.interestOps(SelectionKey.OP_WRITE);
            if (information.isLast()) {
                channel.close();
            }
        } catch (final IOException e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    public void run(final String host, final int port, final String prefix, final int threads, final int requests) {
        //делаем кучу каналов
        final List<DatagramChannel> channels = new ArrayList<>();
        try (final Selector selector = Selector.open()) {

            final InetSocketAddress serverAddress = new InetSocketAddress(host, port);
            for (int i = 0; i < threads; i++) {
                final DatagramChannel channel = DatagramChannel.open();
                channel.configureBlocking(false);
                channel.connect(serverAddress);
                final int bufferSize = channel.getOption(StandardSocketOptions.SO_RCVBUF);
                final ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
                channel.register(selector, SelectionKey.OP_WRITE, new Information(buffer, i, requests));
                channels.add(channel);
            }

            while (!Thread.interrupted() && !selector.keys().isEmpty()) {
                selector.select(TIME_OUT);
                if (selector.selectedKeys().isEmpty()) {
                    for (final SelectionKey key : selector.keys()) {
                        key.interestOpsOr(SelectionKey.OP_WRITE);
                    }
                    continue;
                }

                for (final Iterator<SelectionKey> it = selector.selectedKeys().iterator(); it.hasNext(); ) {
                    final SelectionKey key = it.next();
                    try {
                        if (key.isReadable() && key.isValid()) {
                            read(key, prefix);
                        }
                        if (key.isValid() && key.isWritable()) {
                            write(key, prefix);
                        }
                    } finally {
                        it.remove();
                    }
                }

            }

        } catch (final IOException e) {
            System.err.println("Unable to open selector"+e.getMessage());
        } finally {
            for (final DatagramChannel channel : channels) {
                try {
                    channel.close();
                } catch (final IOException e) {
                    System.err.println("Unable to close channel"+e.getMessage());
                }
            }
        }
    }

    private static class Information {
        final ByteBuffer buffer;
        int requestNumber;
        final int threadNumber;
        final int maxRequest;

        Information(final ByteBuffer buffer, final int threadNumber, final int countRequest) {
            this.buffer = buffer;
            this.requestNumber = 0;
            this.threadNumber = threadNumber;
            this.maxRequest = countRequest;
        }

        private void addRequestNumber() {
            this.requestNumber++;
        }

        private boolean isLast() {
            return this.requestNumber >= this.maxRequest;
        }

        private String createString(final String prefix) {
            return prefix + this.threadNumber + "_" + this.requestNumber;
        }

    }

    public static void main(String[] args) {
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
            new HelloUDPNonblockingClient().run(host, port, prefix, threads, requests);
        } catch (final NumberFormatException e){
            System.err.println("Wrong number format of arguments: " + e.getMessage());
        }

    }
}
