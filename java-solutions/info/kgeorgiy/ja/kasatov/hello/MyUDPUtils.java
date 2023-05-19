package info.kgeorgiy.ja.kasatov.hello;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class MyUDPUtils {
    public static void closeThreadPool(ExecutorService threadsPool, String blockName) {
        try {
            threadsPool.shutdown();
            if (!threadsPool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS)) {
                System.err.println(blockName + " threads weren't terminated correctly");
            }
        } catch (InterruptedException e) {
            System.err.println(blockName + " threads were not awaited: " + e.getMessage());
        }
    }

    public static boolean sendPacket(DatagramSocket socket, DatagramPacket packet, String blockName) {
        try {
            socket.send(packet);
            return true;
        } catch (IOException e) {
            System.err.println("Can't send packet from " + blockName + ": " + e.getMessage());
            return false;
        }
    }
}
