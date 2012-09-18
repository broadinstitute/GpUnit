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
 */package org.genepattern.gpunit.diff;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.NumberFormat;
import java.util.List;
import java.util.regex.Pattern;

import org.broadinstitute.io.matrix.DatasetCreator;
import org.broadinstitute.io.matrix.DatasetParser;
import org.broadinstitute.io.matrix.DefaultDatasetCreator;
import org.broadinstitute.io.matrix.ParseException;
//import org.broadinstitute.io.matrix.gct.GctParserThreadSafe;
import org.broadinstitute.io.matrix.res.ResParser;
import org.broadinstitute.io.matrix.stanford.CdtParser;
import org.broadinstitute.matrix.DefaultDataset;

/**
 * A copy of IOUtil, modified to be thread-safe.
 * 
 * @author Peter Carr
 */
public class IOUtilThreadSafe {
    static public class Singleton {
        static final IOUtilThreadSafe INSTANCE = new IOUtilThreadSafe();
        public static IOUtilThreadSafe instance() {
            return INSTANCE;
        }
    }

    //
    // the NumberFormat class is not ThreadSafe, specifically, calls to setMaximumFractionDigits does not apply uniformal to all 
    // threads which access this static variable. 
    // the workaround is to use ThreadLocal.
    // See more info here: http://stackoverflow.com/questions/1285279/java-synchronization-issue-with-numberformat
    //
    private ThreadLocal<NumberFormat> numberFormatHolder = new ThreadLocal<NumberFormat>() {
        public NumberFormat initialValue() {
            NumberFormat nf = NumberFormat.getInstance();
            nf.setMaximumFractionDigits(2);
            return nf;
        }
    };
    
    //apply ThreadLocal to pattern variable. Not sure if I need it.
    private ThreadLocal<Pattern> patternHolder = new ThreadLocal<Pattern>() {
        public Pattern initialValue() {
            Pattern pattern = Pattern.compile("\t");
            return pattern;
        }
    };

    //force singleton pattern
    private IOUtilThreadSafe() {
    }

    /**
     * Gets a data reader that can read the document at the given pathname or <code>null</code> if no reader is found.
     * 
     * @param pathname, A pathname string
     * @return The reader or null, if no reader exists for the file extension.
     */
    public DatasetParser getDatasetParser(String pathname) {
        String ext = getExtension(pathname);
        if ("gct".equalsIgnoreCase(ext)) {
            return new GctParserThreadSafe();
        }
        else if ("res".equalsIgnoreCase(ext)) {
            return new ResParser();
        }
        // "pcl", "cdt", "txt"
        else if ("cdt".equalsIgnoreCase(ext) || "pcl".equalsIgnoreCase(ext) || "txt".equalsIgnoreCase(ext)) {
            return new CdtParser();
        }
        else {
            //TODO: throw exception
            return null;
        }
    }

    /**
     * Gets the extension (in lowercase) for the specified file name.
     * 
     * @param name ,The file name.
     * @return The file extension, e.g. txt, gct or <tt>null</tt> if no extension exists;
     */
    public String getExtension(String name) {
        name = new File(name).getName();
        int dotIndex = name.lastIndexOf(".");
        String suffix = null;
        if (dotIndex > 0) {
            suffix = name.substring(dotIndex + 1, name.length());
        }
        if (suffix == null) {
            return null;
        }
        if (suffix.equals("txt") || suffix.equals("xls")) { // see if file is in the form name.gct.txt or
            // name.gct.xls.
            String newPath = name.substring(0, dotIndex);
            int secondDotIndex = newPath.lastIndexOf('.');
            if (secondDotIndex != -1) {// see if file has another suffix
                String secondSuffix = newPath.substring(secondDotIndex + 1, newPath.length());
                if (secondSuffix.equalsIgnoreCase("res") || secondSuffix.equalsIgnoreCase("gct")
                        || secondSuffix.equalsIgnoreCase("cn") || secondSuffix.equalsIgnoreCase("sin")
                        || secondSuffix.equalsIgnoreCase("cls") || secondSuffix.equalsIgnoreCase("gin")) {
                    return secondSuffix.toLowerCase();
                }
            }
        } 
        else if (suffix.equals("zip")) { 
            // read zip compressed files.
            String newPath = name.substring(0, dotIndex);
            return getExtension(newPath);
        }
        return suffix.toLowerCase();
    }

    /**
     * Parses text to produce a double.
     * 
     * @param s, A <code>String</code>.
     * @return A <code>double</code> parsed from the string.
     * @exception NumberFormatException, if the beginning of the specified string cannot be parsed.
     */
    public double parseDouble(String s) throws NumberFormatException {
        try {
            //return numberFormat.parse(s).doubleValue();
            return numberFormatHolder.get().parse(s).doubleValue();
        } 
        catch (java.text.ParseException e) {
            throw new NumberFormatException(e.getMessage());
        }
    }

    /**
     * Parses text to produce a double.
     * 
     * @param s, A <code>String</code>.
     * @return A <code>double</code> parsed from the string.
     * @exception NumberFormatException, if the beginning of the specified string cannot be parsed.
     */
    public double parseDoubleNaN(String s) throws NumberFormatException {
        try {
            if ("".equals(s) || "NA".equals(s) || s.equals("NaN")) {
                return Double.NaN;
            }
            return numberFormatHolder.get().parse(s).doubleValue();
        } 
        catch (java.text.ParseException e) {
            throw new NumberFormatException(e.getMessage());
        }
    }

    /**
     * Parses text to produce an integer.
     * 
     * @param s, A <code>String</code>.
     * @return An <code>integer</code> parsed from the string.
     * @exception NumberFormatException, if the beginning of the specified string cannot be parsed.
     */
    public int parseInt(String s) throws NumberFormatException {
        try {
            return numberFormatHolder.get().parse(s).intValue();
        } 
        catch (java.text.ParseException e) {
            throw new NumberFormatException(e.getMessage());
        }
    }

    /**
     * Parses text to produce a long.
     * 
     * @param s, A <code>String</code>.
     * @return A <code>long</code> parsed from the string.
     * @exception NumberFormatException, if the beginning of the specified string cannot be parsed.
     */
    public long parseLong(String s) throws NumberFormatException {
        try {
            return numberFormatHolder.get().parse(s).longValue();
        } 
        catch (java.text.ParseException e) {
            throw new NumberFormatException(e.getMessage());
        }
    }

    /**
     * Reads the data at the given pathname and returns a new <tt>DefaultDataset</tt> instance.
     * 
     * @param pathname
     *            The file pathname
     * @return An <tt>DefaultDataset</tt> instance
     * @throws IOException
     *             If an error occurs while reading from the file
     * @throws ParseException
     *             If there is a problem with the data
     */
    public DefaultDataset readDataset(String pathname) throws IOException, ParseException {
        InputStream is = null;
        
        try {
            is = new BufferedInputStream(new FileInputStream( new File(pathname) ) );
        
            DatasetParser parser = getDatasetParser(pathname);
            DatasetCreator<DefaultDataset> handler = new DefaultDatasetCreator();
            parser.setHandler(handler);
            parser.parse(is);

            DefaultDataset defaultDataset = handler.create();
            return defaultDataset;
        }
        finally {
            if (is != null) {
                is.close();
            }
        }
    }

    /**
     * Splits the given string using a tab as the delimitter
     * 
     * @param s
     *            The string to split.
     * @return The tokens.
     */
    public String[] split(String s) {
        return patternHolder.get().split(s, 0);
    }

    /**
     * Converts an int array to a string.
     * 
     * @param array
     *            The int array.
     * @param sep
     *            String to separate items in the array.
     * @return A string representation of the array.
     */
    public String toString(int[] array, String sep) {
        StringBuffer sb = new StringBuffer();
        if (array == null) {
            return "";
        }
        for (int i = 0, size = array.length; i < size; i++) {
            if (i > 0) {
                sb.append(sep);
            }
            sb.append(String.valueOf(array[i]));
        }
        return sb.toString();
    }

    /**
     * Converts a list to a string.
     * 
     * @param list
     *            The list.
     * @param sep
     *            String to separate items in the list.
     * @return A string representation of the list.
     */
    public String toString(List<?> list, String sep) {
        StringBuffer sb = new StringBuffer();
        if (list == null) {
            return "";
        }
        for (int i = 0, size = list.size(); i < size; i++) {
            if (i > 0) {
                sb.append(sep);
            }
            sb.append(String.valueOf(list.get(i)));
        }
        return sb.toString();
    }

    /**
     * Writes the stack trace of a <code>Throwable</code> to a string.
     * 
     * @param t
     *            The throwable.
     * @return The stack trace as a string.
     */
    public String toString(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        pw.close();
        return sw.toString();
    }

}