package com.github.blutorange.maven.plugin.closurecompiler.test;

import java.io.IOException;
import java.io.OutputStream;

public class NoOpOutputStream extends OutputStream {
    @Override
    public void write(int b) throws IOException {}
}
