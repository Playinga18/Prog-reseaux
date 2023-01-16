package fr.uge.td1;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;

public class ReadStandardInputWithEncoding {

    private static final int BUFFER_SIZE = 1024;

    private static void usage() {
        System.out.println("Usage: ReadStandardInputWithEncoding charset");
    }

    private static String stringFromStandardInput(Charset cs) throws IOException {
        ReadableByteChannel in = Channels.newChannel(System.in);
        var buffer = ByteBuffer.allocate(BUFFER_SIZE);
        var cmp = 2;
        do {
            if (!buffer.hasRemaining()) {
                var bb = ByteBuffer.allocate(BUFFER_SIZE*cmp);
                cmp ++;
                bb.put(buffer.flip());
                buffer = bb;
            }
        }while(in.read(buffer) != -1);

        buffer.flip();
        return cs.decode(buffer).toString();
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            usage();
            return;
        }
        Charset cs = Charset.forName(args[0]);
        System.out.print(stringFromStandardInput(cs));
    }
}