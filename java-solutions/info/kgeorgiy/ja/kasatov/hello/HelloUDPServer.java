package info.kgeorgiy.ja.kasatov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HelloUDPServer implements HelloServer {
    private ExecutorService threadsPool;
    private DatagramSocket socket;
    Thread mainThread;

    @Override
    public void start(int port, int threads) {
        this.threadsPool = Executors.newFixedThreadPool(threads);
        mainThread = new Thread(new MainThread(port));
        mainThread.start();
    }

    private class MainThread implements Runnable {
        private static final int BUFF_SIZE = 128;
        private final int port;
        public MainThread(int port) {
            this.port = port;
        }

        @Override
        public void run() {
            try {
                socket = new DatagramSocket(port);
                socket.setSoTimeout(100);
                while (!Thread.interrupted()) {
                    DatagramPacket packet = new DatagramPacket(new byte[BUFF_SIZE], BUFF_SIZE);
                    try {
                        socket.receive(packet);
                        threadsPool.submit(() -> answer(packet));
                    } catch (IOException e) {
                        // Waiting for clients
                    }
                }
                Thread.currentThread().interrupt();
            } catch (SocketException e) {
                System.err.println("Can't configure socket on server");
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
            System.err.println("Can't send answer to client");
        }
    }

    @Override
    public void close() {
        mainThread.interrupt();
        try {
            mainThread.join();
        } catch (InterruptedException e) {
            System.err.println("Server main thread wasn't terminated correctly");
        }
        threadsPool.shutdownNow();
        try {
            if ( !threadsPool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS)) {
                System.err.println("Server subthreads pool wasn't terminated");
            }
        } catch (InterruptedException e) {
            System.err.println("Server subthreads weren't awaited");
        }
        socket.close();
    }
}
