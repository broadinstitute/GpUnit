/*
 * Copyright 2008-2012 The Broad Institute.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.genepattern.gpunit.diff;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

//import org.broadinstitute.io.IOUtilThreadSafe;
import org.broadinstitute.io.matrix.DatasetHandler;
import org.broadinstitute.io.matrix.DatasetParser;
import org.broadinstitute.io.matrix.ParseException;

/**
 * A copy of GctParser, modified to be thread-safe.
 * 
 * @author Peter Carr
 */
public class GctParserThreadSafe implements DatasetParser {
    private final static String VERSION = "#1.2";
    private LineNumberReader reader;
    private int rows, columns;
    private DatasetHandler handler;
    private List<String> suffixes = Collections.unmodifiableList(Arrays.asList(new String[] { "gct" }));

    public GctParserThreadSafe() {
    }

    public boolean canDecode(InputStream is) throws IOException {
        reader = new LineNumberReader(new InputStreamReader(is));
        try {
            readHeader();
            return true;
        } 
        catch (ParseException pe) {
            return false;
        }
    }

    /**
     * Parse a gct document. The application can use this method to instruct the gct reader to begin parsing an gct
     * document from any valid input source. Applications may not invoke this method while a parse is in proggcts (they
     * should create a new GctParser instead ). Once a parse is complete, an application may reuse the same GctParser
     * object, possibly with a different input source. During the parse, the GctParser will provide information about
     * the gct document through the registered event handler. This method is synchronous: it will not return until
     * parsing has ended. If a client application wants to terminate parsing early, it should throw an exception.
     * 
     * @param is
     *            The input stream
     * @throws org.broadinstitute.io.matrix.ParseException
     *             - Any parse exception, possibly wrapping another exception.
     * @throws IOException
     *             - An IO exception from the parser, possibly from a byte stream or character stream supplied by the
     *             application.
     */
    public void parse(InputStream is) throws ParseException, IOException {
        reader = new LineNumberReader(new InputStreamReader(is));
        readHeader();
        readData();
    }

    void readData() throws ParseException, IOException {
        int rowIndex = 0;
        int expectedColumns = columns + 2;
        for (String s = reader.readLine(); s != null; s = reader.readLine(), rowIndex++) {
            if (rowIndex >= rows) {
                if (s.trim().equals("")) {// ignore blank lines at the end of
                    // the file
                    rowIndex--;
                    continue;
                }
                int rowsRead = rowIndex + 1;
                throw new ParseException( "More data rows than expected on line " 
                    + reader.getLineNumber() + ". Read " + rowsRead + ", expected " + rows + ".");
            }
            String[] tokens = IOUtilThreadSafe.Singleton.instance().split(s);
            if (tokens.length != expectedColumns) {
                throw new ParseException(tokens.length + " " + getColumnString(tokens.length)
                    + " on line " + reader.getLineNumber() + ". Expected " + expectedColumns + ".");
            }
            String rowName = tokens[0];
            handler.beginRow(rowIndex);
            handler.rowName(rowIndex, rowName);
            handler.rowMetaData(rowIndex, 0, tokens[1]);

            for (int columnIndex = 0; columnIndex < columns; columnIndex++) {
                final String token = tokens[columnIndex + 2];
                try {
                    double value = IOUtilThreadSafe.Singleton.instance().parseDoubleNaN(token);
                    handler.data(rowIndex, columnIndex, value);
                } 
                catch (NumberFormatException nfe) {
                    throw new  ParseException("Data at line number " + reader.getLineNumber()
                        + " and column " + columnIndex + " is not a number."
                        + " token='"+token+"'");
                }
            }
        }
        if (rowIndex != rows) {
            throw new  ParseException("Missing data rows. Read " + rowIndex + " "
		        + getRowString(rowIndex) + ", expected " + rows);
        }
    }

    void readHeader() throws  ParseException, IOException {
        String versionLine = reader.readLine();
        if (versionLine == null || !versionLine.trim().equals(VERSION)) {
            throw new org.broadinstitute.io.matrix.ParseException("Unknown version on line 1.");
        }
        String dimensionsLine = reader.readLine().trim();// 2nd header line:

        // <numRows> <tab>
        // <numCols>
        String[] dimensions = IOUtilThreadSafe.Singleton.instance().split(dimensionsLine);
        if (dimensions.length != 2) {
            throw new ParseException("Line number " + reader.getLineNumber()
                    + " should contain the number of rows and the number of columns separated by a tab.");
        }
        try {
            rows = Integer.parseInt(dimensions[0]);
            columns = Integer.parseInt(dimensions[1]);
        } 
        catch (NumberFormatException nfe) {
            throw new  ParseException("Line number " + reader.getLineNumber()
                    + " should contain the number of rows and the number of columns separated by a tab.");
        }
        if (rows <= 0 || columns <= 0) {
            throw new ParseException("Number of rows and columns must be greater than 0.");
        }
        if (handler != null) {
            handler.init(rows, columns, new String[] { "description" }, new String[] { "description" }, new String[] {});
        }
        String columnNamesLine = reader.readLine();
        String[] columnNames = IOUtilThreadSafe.Singleton.instance().split(columnNamesLine);
        // columnNames[0] = 'Name'
        // columnNames[1] ='Description'
        int expectedColumns = columns + 2;
        if (columnNames.length != expectedColumns) {
            throw new ParseException("Expected " + (expectedColumns - 2)
                    + " column names, but read " + (columnNames.length - 2) + " column names on line " + reader.getLineNumber()
                    + ".");
        }
        for (int j = 0; j < columns; j++) {
            String columnName = columnNames[j + 2];
            if (handler != null) {
                handler.columnName(j, columnName);
            }
        }
    }

    public void setHandler(DatasetHandler handler) {
        this.handler = handler;
    }

    protected String getRowString(int rows) {
	if (rows == 1) {
	    return "row";
	}
	return "rows";
    }

    protected String getColumnString(int cols) {
	if (cols == 1) {
	    return "column";
	}
	return "columns";
    }

    public String getFormatName() {
	return "gct";
    }

    public List<String> getFileSuffixes() {
	return suffixes;
    }
}
