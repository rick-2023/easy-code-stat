package com.your.plugin.statistics;

public class FileStatistics {
    private String extension;
    private int count;
    private long size;
    private long sizeMin;
    private long sizeMax;
    private long sizeAvg;
    private int totalLines;
    private int codeLines;
    private int commentLines;
    private int blankLines;
    private int linesMin;
    private int linesMax;
    private int linesAvg;

    public FileStatistics(String extension) {
        this.extension = extension;
        this.sizeMin = Long.MAX_VALUE;
        this.linesMin = Integer.MAX_VALUE;
    }

    // Getters and setters
    public String getExtension() { return extension; }
    public int getCount() { return count; }
    public long getSize() { return size; }
    public long getSizeMin() { return sizeMin; }
    public long getSizeMax() { return sizeMax; }
    public long getSizeAvg() { return sizeAvg; }
    public int getLines() { return totalLines; }
    public int getLinesMin() { return linesMin; }
    public int getLinesMax() { return linesMax; }
    public int getLinesAvg() { return linesAvg; }
    public int getCodeLines() { return codeLines; }
    public int getCommentLines() { return commentLines; }
    public int getBlankLines() { return blankLines; }

    public void addFile(long fileSize, int totalLines, int codeLines, 
                       int commentLines, int blankLines) {
        count++;
        size += fileSize;
        this.totalLines += totalLines;
        this.codeLines += codeLines;
        this.commentLines += commentLines;
        this.blankLines += blankLines;
        
        sizeMin = Math.min(sizeMin, fileSize);
        sizeMax = Math.max(sizeMax, fileSize);
        sizeAvg = size / count;
        
        linesMin = Math.min(linesMin, totalLines);
        linesMax = Math.max(linesMax, totalLines);
        linesAvg = this.totalLines / count;
    }
} 