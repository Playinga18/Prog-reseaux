package fr.uge.td4;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static java.nio.file.StandardOpenOption.*;

public class ClientIdUpperCaseUDPOneByOne {

    private static Logger logger = Logger.getLogger(ClientIdUpperCaseUDPOneByOne.class.getName());
    private static final Charset UTF8 = StandardCharsets.UTF_8;
    private static final int BUFFER_SIZE = 1024;

    private record Response(long id, String message) {
    };

    private final String inFilename;
    private final String outFilename;
    private final long timeout;
    private final InetSocketAddress server;
    private final DatagramChannel dc;
    private final SynchronousQueue<Response> queue = new SynchronousQueue<>();

    public static void usage() {
        System.out.println("Usage : ClientIdUpperCaseUDPOneByOne in-filename out-filename timeout host port ");
    }

    public ClientIdUpperCaseUDPOneByOne(String inFilename, String outFilename, long timeout, InetSocketAddress server)
            throws IOException {
        this.inFilename = Objects.requireNonNull(inFilename);
        this.outFilename = Objects.requireNonNull(outFilename);
        this.timeout = timeout;
        this.server = server;
        this.dc = DatagramChannel.open();
        dc.bind(null);
    }

    private void listenerThreadRun() {
        try {
            var buffer = ByteBuffer.allocate(BUFFER_SIZE);
            while(dc.isOpen()){
                var sender = dc.receive(buffer);
                buffer.flip();
                var id = buffer.getLong();
                var msg = UTF8.decode(buffer);
                var rep = new Response(id,  msg.toString());
                System.out.println("Received " + buffer.remaining() + " bytes from " + sender);
                System.out.println(rep.message);
                queue.put(rep);
                buffer.clear();
            }
        } catch (IOException | InterruptedException e) {
            return;
        }
    }

    public void launch() throws IOException, InterruptedException {
        try {

            var listenerThread = Thread.ofPlatform().start(this::listenerThreadRun);

            // Read all lines of inFilename opened in UTF-8
            var lines = Files.readAllLines(Path.of(inFilename), UTF8);
            var upperCaseLines = new ArrayList<String>();
            var bufferSend = ByteBuffer.allocate(BUFFER_SIZE);
            var actualTimeout = this.timeout;

            for (var i = 0; i < lines.size(); i++) {
                Response rep;
                bufferSend.putLong(i);
                bufferSend.put(UTF8.encode(lines.get(i)));
                do {
                    dc.send(bufferSend.flip(), server);
                    rep = queue.poll(timeout, TimeUnit.MILLISECONDS);
                    if (rep == null ) {
                        actualTimeout = this.timeout;
                    }else if(rep.id != i) {
                        System.currentTimeMillis();
                    }
                }while ( rep == null || rep.id != i);
                upperCaseLines.add(rep.message);
                bufferSend.clear();
            }

            listenerThread.interrupt();
            Files.write(Paths.get(outFilename), upperCaseLines, UTF8, CREATE, WRITE, TRUNCATE_EXISTING);
        } finally {
            dc.close();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length != 5) {
            usage();
            return;
        }

        var inFilename = args[0];
        var outFilename = args[1];
        var timeout = Long.parseLong(args[2]);
        var server = new InetSocketAddress(args[3], Integer.parseInt(args[4]));

        // Create client with the parameters and launch it
        new ClientIdUpperCaseUDPOneByOne(inFilename, outFilename, timeout, server).launch();
    }
}
