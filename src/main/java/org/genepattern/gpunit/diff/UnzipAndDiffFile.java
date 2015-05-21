package org.genepattern.gpunit.diff;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.junit.Assert;

import java.io.*;
import java.util.Enumeration;

/**
 * This diff command takes a job result that is a zip file and then does a diff against a single file contained in it.
 * Created by nazaire on 3/20/14.
 */
public class UnzipAndDiffFile extends AbstractDiffTest
{

    @Override
    public void diff(String serverURL)
    {
        System.out.println("This is my unzip and diff test class");

        //extract file with same name aas expected file from the actual zip file
        InputStream actualContent = null;
        BufferedReader actualReader = null;
        BufferedReader expectedReader = null;
        try
        {
            String expectedFileName = expected.getName();
            ZipFile zipFile = new ZipFile(actual);

            Enumeration<ZipArchiveEntry> zipEntries = zipFile.getEntries();
            ZipArchiveEntry actualZipEntry = null;
            while(zipEntries.hasMoreElements())
            {
                ZipArchiveEntry zipEntry = zipEntries.nextElement();
                String zipEntryName = zipEntry.getName();
                //get name with the directory prefix
                if(zipEntryName.endsWith("/" + expectedFileName) || zipEntryName.equals(expectedFileName))
                {
                    actualZipEntry = zipEntry;
                }

            }
            actualContent = zipFile.getInputStream(actualZipEntry);

            actualReader = new BufferedReader(new InputStreamReader(actualContent));

            expectedReader = new BufferedReader(new FileReader(expected));

            String actualLine = null;
            String expectedLine = null;

            while((actualLine = actualReader.readLine()) != null)
            {
                expectedLine = expectedReader.readLine();
                if(!actualLine.equals(expectedLine))
                {
                    Assert.fail(jobId + "Files differ: expected=" + expectedLine + " actual=" + actualLine);
                }
            }
            System.out.println("Check point 3");

            //check if the expected file is not longer than the actual file
            expectedLine = expectedReader.readLine();
            if(expectedLine != null)
            {
                Assert.fail(jobId + "File differ: expected file longer then actual file. Found \"" + expectedLine + "\"");
            }
        }
        catch(IOException io)
        {
            io.printStackTrace();
            System.err.println(io.getMessage());
            Assert.fail(jobId + "An error occurred while performing the diff");
        }
        finally
        {
            if(actualContent != null)
            {
                try{actualContent.close();} catch(IOException i){}
            }
            if(actualReader != null)
            {
                try{actualReader.close();} catch(IOException i){}
            }
            if(expectedReader != null)
            {
                try{expectedReader.close();} catch(IOException i){}
            }
        }

        System.out.println("Check point 4");
    }
}
