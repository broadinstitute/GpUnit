package org.genepattern.gpunit.debug;

import static org.junit.Assert.fail;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.genepattern.gpunit.BatchProperties;
import org.genepattern.gpunit.GpUnitException;
import org.junit.Ignore;

@Ignore
public class ModuleTest {
    /** helper class to avoid check for MalformedURLException */
    public static final URL initLocalUrl() {
        try {
            return new URL("http://127.0.0.1:8080/gp/");
        } 
        catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static final URL LOCAL_URL=initLocalUrl();

    /**
     * Initialize gpClient for running tests on the default local GenePattern Server.
     * Example usage:
     * <pre>
     *     initGpClient("test-user:test-password");
     * </pre>
     * 
     * @param usernamePassword - the username and password separated by the ':' character.
     */
    public static BatchProperties initGpClient(final String usernamePassword) throws GpUnitException  {
        return initGpClient(usernamePassword, LOCAL_URL);
    }

    /**
     * Initialize gpClient for running tests. 
     * Example usage:
     * <pre>
         initGpClient("test-user:test-password", "https://gp.example.com:8080/gp");
     * </pre>
     * @param usernamePassword - the username:password, separated by the ':' character.
     * @param gpUrl - the server on which to run the tests
     */
    public static BatchProperties initGpClient(final String usernamePassword, final String gpUrl) throws GpUnitException {
        try {
            return initGpClient(usernamePassword, new URL(gpUrl));
        } 
        catch (MalformedURLException e) {
            fail("Error initializing gpUrl: "+e.getLocalizedMessage());
            return null;
        }
    }

    /**
     * Initialize gpClient for running tests.
     * @param usernamePassword - the username:password, separated by the ':' character
     * @param url - the server on which to run the tests
     */
    public static BatchProperties initGpClient(final String usernamePassword, final URL url) throws GpUnitException {
        final String[] userArgs=usernamePassword.split(":", 2);
        final String username=userArgs[0];
        final String password;
        if (userArgs.length>1) {
            password=userArgs[1];
        }
        else {
            password=null;
        }
        return initGpClient(username, password, url);
    }

    /**
     * Initialize gpClient for running tests.
     * @param username
     * @param password
     * @param url - the server on which to run the tests
     */
    public static BatchProperties initGpClient(final String username, final String password, final URL url) throws GpUnitException {
        final BatchProperties.Builder b = new BatchProperties.Builder() 
            .scheme(url.getProtocol())
            .host(url.getHost())
            .username(username)
            .password(password)
            // gpunit.delete.jobs=false
            .deleteJobs(false)
            .saveDownloads(true)
            .testTimeout(-1);
        int port = url.getPort();
        if (port != 80 && port > 0) {
            b.port(port);
        }
        return b.build();
    }

    /**
     * Run a single test as the default user on the default local server
     * @param testFile
     * @throws GpUnitException
     */
    public static void doModuleTest(final File testFile) throws GpUnitException {
        doModuleTest(initGpClient("test:test"), testFile);
    }

    /**
     * Run a single test on the default local server
     * @param usernamePassword
     * @param testFile
     * @throws GpUnitException
     */
    public static void doModuleTest(final String usernamePassword, final File testFile) throws GpUnitException {
        doModuleTest(initGpClient(usernamePassword, LOCAL_URL), testFile);
    }

    /**
     * Run a single test on the given server
     * @param usernamePassword
     * @param gpUrl - the server on which to run the test
     * @param testFile
     * @throws MalformedURLException
     * @throws GpUnitException
     */
    public static void doModuleTest(final String usernamePassword, final String gpUrl, final File testFile) throws GpUnitException {
        doModuleTest(initGpClient(usernamePassword, gpUrl), testFile);
    }

    /**
     * Run a single test on the server defined by the gpClient BatchProperties
     * @param gpClient
     * @param testFile
     * @throws GpUnitException
     */
    public static void doModuleTest(final BatchProperties gpClient, final File testFile) throws GpUnitException {
        new RestModuleTest(gpClient, testFile).doModuleTest();
    }

}
