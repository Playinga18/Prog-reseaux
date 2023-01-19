package fr.uge.td3;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.Charset;
import java.util.Scanner;

public class NetcatUDP {
    public static final int BUFFER_SIZE = 1024;

    private static void usage() {
        System.out.println("Usage : NetcatUDP host port charset");
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            usage();
            return;
        }

        var server = new InetSocketAddress(args[0], Integer.parseInt(args[1]));
        var cs = Charset.forName(args[2]);
        var buffer = ByteBuffer.allocate(BUFFER_SIZE);
        DatagramChannel dc = DatagramChannel.open();

        try (var scanner = new Scanner(System.in);) {
            while (scanner.hasNextLine()) {
                var line = scanner.nextLine();
                var res = cs.encode(line);
                dc.send(res, server);
                buffer.clear();
                var sender = dc.receive(buffer);
                buffer.flip();
                System.out.println("Received " + buffer.remaining() + " bytes from " + sender);
                System.out.println(cs.decode(buffer));

            }
        }
    }
}
