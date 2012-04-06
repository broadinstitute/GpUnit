package org.genepattern.gpunit;

public class TestFileObj {
    private int numRows = -1;
    private int numCols = -1;
    private String diff = null;

    public int getNumRows() {
        return numRows;
    }
    public void setNumRows(int numRows) {
        this.numRows = numRows;
    }
    public int getNumCols() {
        return numCols;
    }
    public void setNumCols(int numCols) {
        this.numCols = numCols;
    }
    public String getDiff() {
        return diff;
    }
    public void setDiff(String diff) {
        this.diff = diff;
    }
}