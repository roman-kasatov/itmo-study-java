package info.kgeorgiy.ja.kasatov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HelloUDPServer implements HelloServer {
    private static final int SOCKET_TIMEOUT = 100;
    private ExecutorService threadsPool;
    private DatagramSocket socket;
    private Thread mainThread;

    /**
     * Runs {@link HelloUDPServer#start(int, int)} with arguments.
     * @param args must contain: <br>
     *              номер порта, по которому будут приниматься запросы; <br>
     *              число рабочих потоков, которые будут обрабатывать запросы.
     */
    public static void main(String[] args) {
        if (args == null || Arrays.stream(args).anyMatch(Objects::isNull) || args.length != 2) {
            System.err.println("Incorrect arguments");
            return;
        }
        try (HelloUDPServer server = new HelloUDPServer()) {
            server.start(
                    Integer.parseInt(args[0]),
                    Integer.parseInt(args[1])
            );
        }
    }

    @Override
    public void start(int port, int threads) {
        this.threadsPool = Executors.newFixedThreadPool(threads);
        mainThread = new Thread(new MainThread(port));
        mainThread.start();
    }

    private class MainThread implements Runnable {
        private final int port;
        public MainThread(int port) {
            this.port = port;
        }

        @Override
        public void run() {
            try {
                socket = new DatagramSocket(port);
                socket.setSoTimeout(SOCKET_TIMEOUT);
                while (!Thread.interrupted()) {
                    byte[] buff = new byte[socket.getReceiveBufferSize()];
                    DatagramPacket packet = new DatagramPacket(buff, buff.length);
                    try {
                        socket.receive(packet);
                        threadsPool.submit(() -> answer(packet));
                    } catch (SocketTimeoutException e) {
                        // Waiting for clients
                    } catch (IOException e) {
                        System.err.println("Exception occurred while receiving datagram from client: "
                                + e.getMessage());
                    }
                }
                Thread.currentThread().interrupt();
            } catch (SocketException e) {
                System.err.println("Can't configure socket on server: " + e.getMessage());
            }
        }
    }

    private void answer(DatagramPacket packet) {
        String request = new String(packet.getData(), packet.getOffset(), packet.getLength());
        String answer = "Hello, " + request;
        packet.setData(answer.getBytes());
        MyUDPUtils.sendPacket(socket, packet, "Server");
    }

    @Override
    public void close() {
        mainThread.interrupt();
        try {
            mainThread.join();
        } catch (InterruptedException e) {
            System.err.println("Server main thread wasn't terminated correctly: " + e.getMessage());
        }
        threadsPool.shutdownNow();
        MyUDPUtils.closeThreadPool(threadsPool, "Server");
        socket.close();
    }
}
