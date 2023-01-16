package fr.uge.td1;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.file.Path;

import static java.nio.file.StandardOpenOption.READ;

public class DecoderOnTheFly {

    private final int inputBufferCapacity;
    private final int outputBufferCapacity;
    private final CharsetDecoder charsetDecoder;

    private final ByteBuffer byteBuffer;
    private final CharBuffer charBuffer;

    private static void usage() {
        System.out.println("Usage: DecoderOnTheFly charset filename inputBufferCapacity");
    }

    public DecoderOnTheFly(Charset charset, int inputBufferCapacity) {
        if (inputBufferCapacity < Math.ceil(charset.newEncoder().maxBytesPerChar())) {
            throw new IllegalArgumentException(
                    "The input buffer must be able to contain at least largest encoded character for this charset");
        }
        this.charsetDecoder = charset.newDecoder();
        this.inputBufferCapacity = inputBufferCapacity;
        // largest size needed for the output buffer, for this we need maxCharsPerByte
        this.outputBufferCapacity = (int) Math.ceil(inputBufferCapacity * charsetDecoder.maxCharsPerByte());
        byteBuffer = ByteBuffer.allocate(inputBufferCapacity);
        charBuffer = CharBuffer.allocate(outputBufferCapacity);
    }

    public String stringFromFile(Path path) throws IOException {
        try (FileChannel fc = FileChannel.open(path, READ)) {
            while(byteBuffer.hasRemaining()) {
                fc.read(byteBuffer);
                byteBuffer.flip();
                CoderResult cr = charsetDecoder.decode(byteBuffer, charBuffer, false);
                if (cr.isError()) {
                    if (cr.isMalformed()) {
                        throw new IllegalArgumentException("Malformed input encountered");
                    }
                    if (cr.isUnmappable()) {
                        throw new IllegalArgumentException("Unmappable character encountered");
                    }
                }
            }
            charBuffer.flip();
            return charBuffer.toString();
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            usage();
            return;
        }
        var charset = Charset.forName(args[0]);
        var inputBufferCapacity = Integer.parseInt(args[2]);
        var decoderOnTheFly = new DecoderOnTheFly(charset, inputBufferCapacity);

        var path = Path.of(args[1]);
        System.out.println(decoderOnTheFly.stringFromFile(path));
    }
}