package fr.uge.td3;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.Charset;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

public class ClientUpperCaseUDPTimeout {
    public static final int BUFFER_SIZE = 1024;

    private static void usage() {
        System.out.println("Usage : NetcatUDP host port charset");
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length != 3) {
            usage();
            return;
        }

        var server = new InetSocketAddress(args[0], Integer.parseInt(args[1]));
        var cs = Charset.forName(args[2]);
        var buffer = ByteBuffer.allocate(BUFFER_SIZE);
        DatagramChannel dc = DatagramChannel.open();

        var queue = new ArrayBlockingQueue<String>(20);

        Thread.ofPlatform().start(() -> {
            try {
                while(dc.isOpen()){
                    var sender = dc.receive(buffer);
                    var msg = cs.decode(buffer.flip());
                    System.out.println("Received " + buffer.remaining() + " bytes from " + sender);
                    System.out.println(msg);
                    queue.put(msg.toString());
                    buffer.clear();
                }
            } catch (IOException | InterruptedException e) {
                return;
            }
        });

        try (var scanner = new Scanner(System.in);) {
            while (scanner.hasNextLine()) {
                var line = scanner.nextLine();
                dc.send(cs.encode(line), server);
                if (queue.poll(1, TimeUnit.SECONDS) == null) {
                    System.out.println("Le serveur n'a pas répondu");
                }
            }
        }
    }
}
