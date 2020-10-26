package com.github.blutorange.maven.plugin.closurecompiler.test;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Locale;

final class ChainedPrintStream extends PrintStream {
  private final PrintStream p1;
  private final PrintStream p2;

  public ChainedPrintStream(PrintStream p1, PrintStream p2) {
    super(new NoOpOutputStream());
    this.p1 = p1;
    this.p2 = p2;
  }

  @Override
  public void write(int b) {
    p1.write(b);
    p2.write(b);
  }

  @Override
  public void write(byte[] buf, int off, int len) {
    p1.write(buf, off, len);
    p2.write(buf, off, len);
  }

  @Override
  public void print(boolean b) {
    p1.print(b);
    p2.print(b);
  }

  @Override
  public void print(char c) {
    p1.print(c);
    p2.print(c);
  }

  @Override
  public void print(int i) {
    p1.print(i);
    p2.print(i);
  }

  @Override
  public void print(long l) {
    p1.print(l);
    p2.print(l);
  }

  @Override
  public void print(float f) {
    p1.print(f);
    p2.print(f);
  }

  @Override
  public void print(double d) {
    p1.print(d);
    p2.print(d);
  }

  @Override
  public void print(char[] s) {
    p1.print(s);
    p2.print(s);
  }

  @Override
  public void print(String s) {
    p1.print(s);
    p2.print(s);
  }

  @Override
  public void print(Object obj) {
    p1.print(obj);
    p2.print(obj);
  }

  @Override
  public void println() {
    p1.println();
    p2.println();
  }

  @Override
  public void println(boolean x) {
    p1.println(x);
    p2.println(x);
  }

  @Override
  public void println(char x) {
    p1.println(x);
    p2.println(x);
  }

  @Override
  public void println(int x) {
    p1.println(x);
    p2.println(x);
  }

  @Override
  public void println(long x) {
    p1.println(x);
    p2.println(x);
  }

  @Override
  public void println(float x) {
    p1.println(x);
    p2.println(x);
  }

  @Override
  public void println(double x) {
    p1.println(x);
    p2.println(x);
  }

  @Override
  public void println(char[] x) {
    p1.println(x);
    p2.println(x);
  }

  @Override
  public void println(String x) {
    p1.println(x);
    p2.println(x);
  }

  @Override
  public void println(Object x) {
    p1.println(x);
    p2.println(x);
  }

  @Override
  public PrintStream printf(String format, Object... args) {
    p1.printf(format, args);
    return p2.printf(format, args);
  }

  @Override
  public PrintStream printf(Locale l, String format, Object... args) {
    p1.printf(l, format, args);
    return p2.printf(l, format, args);
  }

  @Override
  public PrintStream format(String format, Object... args) {
    p1.format(format, args);
    return p2.format(format, args);
  }

  @Override
  public PrintStream format(Locale l, String format, Object... args) {
    p1.format(l, format, args);
    return p2.format(l, format, args);
  }

  @Override
  public PrintStream append(CharSequence csq) {
    p1.append(csq);
    return p2.append(csq);
  }

  @Override
  public PrintStream append(CharSequence csq, int start, int end) {
    p1.append(csq, start, end);
    return p2.append(csq, start, end);
  }

  @Override
  public PrintStream append(char c) {
    p1.append(c);
    return p2.append(c);
  }

  @Override
  public void flush() {
    p1.flush();
    p2.flush();
  }

  @Override
  public void close() {
    p1.close();
    p2.close();
  }

  @Override
  public boolean checkError() {
    p1.checkError();
    return p2.checkError();
  }

  @Override
  public void setError() {
    p1.checkError();
    p2.checkError();
  }

  @Override
  protected void clearError() {
  }

  @Override
  public void write(byte[] b) throws IOException {
    p1.write(b);
    p2.write(b);
  }
}