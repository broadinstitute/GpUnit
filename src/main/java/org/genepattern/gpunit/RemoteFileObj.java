package org.genepattern.gpunit;

/**
 * Class representing a remote file on the server which will be the target of
 * remote assertions/diffs.
 *  
 * @author cnorman
 */

public class RemoteFileObj extends TestFileObj {
    @Override
    public void setNumRows(int numRows) throws GpUnitException {
        throw new GpUnitException("Remote server num rows not implemented");
    }
    @Override
    public void setNumCols(int numCols) throws GpUnitException {
        throw new GpUnitException("Remote server num cols not implemented");
    }

}
