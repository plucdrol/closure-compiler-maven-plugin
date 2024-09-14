package com.github.blutorange.maven.plugin.closurecompiler.plugin;

import com.github.blutorange.maven.plugin.closurecompiler.common.FileHelper;
import java.io.File;
import java.util.List;
import org.apache.commons.collections4.ListUtils;

public class FileSet {
    private List<String> includes;
    private List<String> excludes;

    public List<String> getIncludes() {
        return ListUtils.emptyIfNull(includes);
    }

    public List<String> getExcludes() {
        return ListUtils.emptyIfNull(excludes);
    }

    public void setIncludes(List<String> includes) {
        this.includes = includes;
    }

    public void setExcludes(List<String> excludes) {
        this.excludes = excludes;
    }

    public List<File> getFiles(File baseDir) {
        return FileHelper.getIncludedFiles(baseDir, getIncludes(), getExcludes());
    }
}
