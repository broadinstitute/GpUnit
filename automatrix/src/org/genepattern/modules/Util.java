package org.genepattern.modules;

import java.io.File;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: nazaire
 * Date: Feb 14, 2013
 * Time: 4:53:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class Util
{
    public static boolean delete(File file)
    {
        try
        {
            if(file.isDirectory()){

                //directory is empty, then delete it
                if(file.list().length==0){

                   file.delete();
                   System.out.println("Directory is deleted : "
                                                     + file.getAbsolutePath());

                }else{

                   //list all the directory contents
                   String files[] = file.list();

                   for (String temp : files) {
                      //construct the file structure
                      File fileDelete = new File(file, temp);

                      //recursive delete
                     delete(fileDelete);
                   }

                   //check the directory again, if empty then delete it
                   if(file.list().length==0){
                     file.delete();
                     System.out.println("Directory is deleted : "
                                                      + file.getAbsolutePath());
                   }
                }

            }else{
                //if file, then delete it
                file.delete();
                System.out.println("File is deleted : " + file.getAbsolutePath());
            }
        }
        catch(Exception e)
        {
            return false;
        }

        return true;
    }
}
