package fr.uge.td3;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import static java.nio.file.StandardOpenOption.*;

public class ClientUpperCaseUDPFile {
    private final static Charset UTF8 = StandardCharsets.UTF_8;
    private final static int BUFFER_SIZE = 1024;


    private static void usage() {
        System.out.println("Usage : ClientUpperCaseUDPFile in-filename out-filename timeout host port ");
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length != 5) {
            usage();
            return;
        }

        var inFilename = args[0];
        var outFilename = args[1];
        var timeout = Integer.parseInt(args[2]);
        var server = new InetSocketAddress(args[3], Integer.parseInt(args[4]));

        // Read all lines of inFilename opened in UTF-8
        var lines = Files.readAllLines(Path.of(inFilename), UTF8);
        var upperCaseLines = new ArrayList<String>();

        DatagramChannel dc = DatagramChannel.open();
        var queue = new ArrayBlockingQueue<String>(20);

        Thread.ofPlatform().start(() -> {
            try {
                var buffer = ByteBuffer.allocate(BUFFER_SIZE);
                while(dc.isOpen()){
                    var sender = dc.receive(buffer);
                    var msg = UTF8.decode(buffer.flip());
                    System.out.println("Received " + buffer.remaining() + " bytes from " + sender);
                    System.out.println(msg);
                    queue.put(msg.toString());
                    buffer.clear();
                }
            } catch (IOException | InterruptedException e) {
                return;
            }
        });

        for (var line: lines) {
            String rep;
            do {
                dc.send(UTF8.encode(line), server);
                rep = queue.poll(timeout, TimeUnit.MILLISECONDS);
            }while ( rep == null);
            upperCaseLines.add(rep);
        }

        // Write upperCaseLines to outFilename in UTF-8
        Files.write(Path.of(outFilename), upperCaseLines, UTF8, CREATE, WRITE, TRUNCATE_EXISTING);
    }
}