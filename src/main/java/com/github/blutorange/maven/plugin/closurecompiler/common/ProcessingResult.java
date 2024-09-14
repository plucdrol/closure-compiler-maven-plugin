package com.github.blutorange.maven.plugin.closurecompiler.common;

public class ProcessingResult {
    private boolean wasSkipped;

    public ProcessingResult() {}

    public ProcessingResult setWasSkipped(boolean wasSkipped) {
        this.wasSkipped = wasSkipped;
        return this;
    }

    public boolean isWasSkipped() {
        return wasSkipped;
    }
}
