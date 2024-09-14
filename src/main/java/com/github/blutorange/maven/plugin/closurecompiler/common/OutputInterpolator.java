package com.github.blutorange.maven.plugin.closurecompiler.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

/**
 * Emulates the behavior of the closure compile command line flag `output_wrapper`:
 *
 * <blockquote>
 *
 * Interpolate output into this string at the place denoted by the marker token %output%. Use marker token
 * %output|jsstring% to do js string escaping on the output.
 *
 * </blockquote>
 *
 * @author madgaksha
 */
public class OutputInterpolator implements UnaryOperator<String> {

    private static final Pattern PATTERN = Pattern.compile("%output%|%output\\|jsstring%");
    ;
    private static final String TYPE_OUTPUT = "%output%";
    private static final String TYPE_OUTPUT_JSSTRING = "%output|jsstring%";

    private static final Token TOKEN_OUTPUT = new TokenOutput();
    private static final Token TOKEN_OUTPUT_JSSTRING = new TokenOutputJsstring();

    private final String pattern;
    private final List<Token> tokens;

    private OutputInterpolator(String pattern, List<Token> tokens) {
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

    public static OutputInterpolator forIdentity() {
        List<Token> tokens = Collections.singletonList(TOKEN_OUTPUT);
        return new OutputInterpolator(TYPE_OUTPUT, tokens);
    }

    public static OutputInterpolator forPattern(String pattern) {
        List<Token> tokens = parse(pattern);
        return new OutputInterpolator(pattern, tokens);
    }

    private static List<Token> parse(String pattern) {
        List<Token> tokens = new ArrayList<>();
        int pos = 0;
        int outputCount = 0;
        Matcher m = PATTERN.matcher(pattern);
        while (m.find()) {
            // Add literal text before match
            if (pos < m.start()) {
                tokens.add(new TokenLiteral(pattern.substring(pos, m.start())));
            }
            // Add matches token
            String type = pattern.substring(m.start(), m.end());
            if (type.equals(TYPE_OUTPUT)) {
                outputCount += 1;
                tokens.add(TOKEN_OUTPUT);
            } else if (type.equals(TYPE_OUTPUT_JSSTRING)) {
                outputCount += 1;
                tokens.add(TOKEN_OUTPUT_JSSTRING);
            } else {
                throw new RuntimeException("Unknown token type: " + type);
            }
            pos = m.end();
        }
        if (pos < pattern.length()) {
            tokens.add(new TokenLiteral(pattern.substring(pos, pattern.length())));
        }
        if (outputCount > 1) {
            throw new IllegalArgumentException(
                    "Invalid pattern, must contain at most one [%output%] or [%output|jsstring%].");
        }
        return tokens;
    }

    private static interface Token extends UnaryOperator<String> {
        String toSource();
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

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((text == null) ? 0 : text.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            TokenLiteral other = (TokenLiteral) obj;
            if (text == null) {
                if (other.text != null) return false;
            } else if (!text.equals(other.text)) return false;
            return true;
        }

        @Override
        public String toSource() {
            return text;
        }
    }

    private static class TokenOutput implements Token {
        public TokenOutput() {}

        @Override
        public String apply(String source) {
            return source;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            return true;
        }

        @Override
        public String toSource() {
            return TYPE_OUTPUT;
        }
    }

    private static class TokenOutputJsstring implements Token {
        public TokenOutputJsstring() {}

        @Override
        public String apply(String source) {
            return StringEscapeUtils.escapeEcmaScript(source);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            return true;
        }

        @Override
        public String toSource() {
            return TYPE_OUTPUT_JSSTRING;
        }
    }

    public String getWrapperPrefix() {
        if (tokens.isEmpty()) {
            return StringUtils.EMPTY;
        }
        int index = -1;
        for (int i = 0, j = tokens.size(); i < j; ++i) {
            if (TOKEN_OUTPUT.equals(tokens.get(i)) || TOKEN_OUTPUT_JSSTRING.equals(tokens.get(i))) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            index = tokens.size() - 1;
        }
        if (index == 0) {
            return StringUtils.EMPTY;
        }
        StringBuilder sb = new StringBuilder();
        tokens.stream().limit(index).map(Token::toSource).forEach(sb::append);
        ;
        return sb.toString();
    }
}
