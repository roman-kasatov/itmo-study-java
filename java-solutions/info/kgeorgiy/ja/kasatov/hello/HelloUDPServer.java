package info.kgeorgiy.ja.kasatov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HelloUDPServer implements HelloServer {
    private static final int SOCKET_TIMEOUT = 100;
    private ExecutorService threadsPool;
    private DatagramSocket socket;
    private Thread mainThread;

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
        try {
            String request = new String(packet.getData(), 0, packet.getLength());
            String answer = "Hello, " + request;
            packet.setData(answer.getBytes());
            socket.send(packet);
        } catch (IOException e) {
            System.err.println("Can't send answer to client: " + e.getMessage());
        }
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
        try {
            if (!threadsPool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS)) {
                System.err.println("Server subthreads pool wasn't terminated");
            }
        } catch (InterruptedException e) {
            System.err.println("Server subthreads weren't awaited: " + e.getMessage());
        }
        socket.close();
    }
}
