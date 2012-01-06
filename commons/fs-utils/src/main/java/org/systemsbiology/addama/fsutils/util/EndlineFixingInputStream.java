package org.systemsbiology.addama.fsutils.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

/**
 * Class to fix Mac endlines, which Linux-based Perl scripts will not process properly.
 * Wrap this around any InputStream to correct line endings of things that seem to be text files.
 *
 * @author anorberg
 */
public class EndlineFixingInputStream extends InputStream {

    private BufferedInputStream bufIn;
    private PushbackInputStream pushIn;

    protected static boolean allASCII(byte[] symbols, int max) {
        //System.out.println("Checking a file");
        max = max > symbols.length ? symbols.length : max;
        for (int k = 0; k < max; ++k) {
            //System.out.print((char)symbols[k]);
            byte b = symbols[k];
            //stop on \n (0xA) anyway: if we have those, we don't need to fake it
            if (b != 9 && b != 0xD && (b > 0x7e || b < 0x20)) {
                //System.out.println(" <-- that one");
                return false;
            }
        }
        //System.out.println(" :::complete");
        return max >= 0;
    }

    public EndlineFixingInputStream(InputStream in) throws IOException {
        super();
        bufIn = new BufferedInputStream(in);
        bufIn.mark(1024);
        byte[] sample = new byte[1024];
        int n = bufIn.read(sample);
        bufIn.reset();
        if (allASCII(sample, n)) {
            pushIn = new PushbackInputStream(bufIn, 5);
        } else {
            pushIn = null;
        }
    }

    public int read() throws IOException {
        if (pushIn == null) {
            return bufIn.read();
        }
        int head = pushIn.read();
        if (head == (int) '\r') {
            int next = pushIn.read();
            //System.out.println("Unreading: " + (char)next);
            pushIn.unread(next);
            if (next != (int) '\n') {
                //System.out.println("Unreading a \\n");
                pushIn.unread('\n');
            }
        }
        return head;
    }

    public boolean markSupported() {
        return pushIn == null;
    }

    public void mark(int bufsz) {
        bufIn.mark(bufsz);
    }

    public void reset() throws IOException {
        if (pushIn != null) {
            throw new IOException("EndlineFixingInputStream does not support mark/reset when in fix mode");
        }
        bufIn.reset();
    }

    public void close() throws IOException {
        if (pushIn != null) {
            pushIn.close();
        } else {
            bufIn.close();
        }
    }

    public long skip(long n) throws IOException {
        if (pushIn != null) {
            return pushIn.skip(n);
        }
        return bufIn.skip(n);
    }

    public int available() throws IOException {
        if (pushIn != null) {
            return pushIn.available();
        }
        return bufIn.available();
    }

}
