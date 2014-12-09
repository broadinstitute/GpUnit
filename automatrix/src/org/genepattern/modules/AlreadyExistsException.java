package org.genepattern.modules;

/**
 * Created by IntelliJ IDEA.
 * User: nazaire
 * Date: Feb 15, 2013
 * Time: 8:32:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class AlreadyExistsException extends Exception
{
    public AlreadyExistsException()
    {
        super();
    }

    public AlreadyExistsException(String message)
    {
        super(message);
    }
}
