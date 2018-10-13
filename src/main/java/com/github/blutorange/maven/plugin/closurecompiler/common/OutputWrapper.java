package com.github.blutorange.maven.plugin.closurecompiler.common;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.text.StringEscapeUtils;

/**
 * <p>
 * Emulates the behavior of the closure compile command line flag `output_wrapper`:
 * </p>
 * <blockquote> Interpolate output into this string at the place denoted by the marker token %output%. Use marker token
 * %output|jsstring% to do js string escaping on the output. </blockquote>
 * @author madgaksha
 */
public class OutputWrapper implements UnaryOperator<String> {

  private final static Pattern PATTERN = Pattern.compile("%output%|%output\\|jsstring%");;
  private final static String TYPE_OUTPUT = "%output%";
  private final static String TYPE_OUTPUT_JSSTRING = "%output|jsstring%";

  private final String pattern;
  private final List<Token> tokens;

  public OutputWrapper(String pattern) {
    this.pattern = pattern;
    this.tokens = parse(pattern);
  }

  public String getPattern() {
    return pattern;
  }

  @Override
  public String apply(String source) {
    return tokens.stream().map(token -> token.apply(source)).collect(Collectors.joining());
  }

  private static List<Token> parse(String pattern) {
    List<Token> tokens = new ArrayList<>();
    int pos = 0;
    Matcher m = PATTERN.matcher(pattern);
    while (m.find()) {
      // Add literal text before match
      if (pos < m.start()) {
        tokens.add(new TokenLiteral(pattern.substring(pos, m.start())));
      }
      // Add matches token
      String type = pattern.substring(m.start(), m.end());
      if (type.equals(TYPE_OUTPUT)) {
        tokens.add(new TokenOutput());
      }
      else if (type.equals(TYPE_OUTPUT_JSSTRING)) {
        tokens.add(new TokenOutputJsstring());
      }
      else {
        throw new RuntimeException("Unknown token type: " + type);
      }
      pos = m.end();
    }
    if (pos < pattern.length()) {
      tokens.add(new TokenLiteral(pattern.substring(pos, pattern.length())));
    }
    return tokens;
  }

  private static interface Token extends UnaryOperator<String> {
  }

  private static class TokenLiteral implements Token {
    private final String text;

    public TokenLiteral(String text) {
      this.text = text;
    }

    @Override
    public String apply(String source) {
      return text;
    }
  }

  private static class TokenOutput implements Token {
    public TokenOutput() {}

    @Override
    public String apply(String source) {
      return source;
    }
  }

  private static class TokenOutputJsstring implements Token {
    public TokenOutputJsstring() {}

    @Override
    public String apply(String source) {
      return StringEscapeUtils.escapeEcmaScript(source);
    }
  }
}
