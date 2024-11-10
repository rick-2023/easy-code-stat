package com.your.plugin.statistics;

import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

public class ClassStatistics {
    private final String className;
    private final Map<String, Integer> methodLines = new HashMap<>();
    private final Set<String> unusedMethods = new HashSet<>();
    private int fieldCount;

    public ClassStatistics(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }

    public void addMethod(String methodName, int lines, boolean isUnused) {
        methodLines.put(methodName, lines);
        if (isUnused) {
            unusedMethods.add(methodName);
        }
    }

    public Map<String, Integer> getMethodLines() {
        return methodLines;
    }

    public Set<String> getUnusedMethods() {
        return unusedMethods;
    }

    public int getUnusedMethodCount() {
        return unusedMethods.size();
    }

    public void setFieldCount(int count) {
        this.fieldCount = count;
    }

    public int getFieldCount() {
        return fieldCount;
    }

    public int getTotalMethodLines() {
        return methodLines.values().stream().mapToInt(Integer::intValue).sum();
    }

    public int getMethodCount() {
        return methodLines.size();
    }
} 