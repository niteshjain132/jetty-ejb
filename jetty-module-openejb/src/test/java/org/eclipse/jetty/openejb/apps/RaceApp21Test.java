package org.eclipse.jetty.openejb.apps;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.UnknownHostException;

import org.eclipse.jetty.openejb.JettyOpenEJBModule;
import org.eclipse.jetty.openejb.logging.LoggingUtil;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.toolchain.test.FS;
import org.eclipse.jetty.toolchain.test.IO;
import org.eclipse.jetty.toolchain.test.MavenTestingUtils;
import org.eclipse.jetty.toolchain.test.SimpleRequest;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class RaceApp21Test
{
    private static final Logger LOG;
    static
    {
        LoggingUtil.config();
        LOG = Log.getLogger(RaceApp21Test.class);
    }

    private static Server server;
    private static URI serverURI;

    @BeforeClass
    public static void startServer() throws Exception
    {
        // Prepare Files & Dirs

        File tmpDir = MavenTestingUtils.getTargetTestingDir("raceapp21");
        FS.ensureEmpty(tmpDir);

        File appsDir = new File(tmpDir,"ejbapps-t");
        File workDir = new File(tmpDir,"work-t");

        FS.ensureEmpty(appsDir);
        FS.ensureEmpty(workDir);

        File earFile = new File(appsDir,"raceApp.ear");

        File srcFile = MavenTestingUtils.getProjectFile("src/test/apps/raceApp-2.1.ear");
        IO.copyFile(srcFile,earFile);
        LOG.info("EarFile: {}",earFile);

        // Define Home / Base
        System.setProperty("jetty.home",tmpDir.getAbsolutePath());
        System.setProperty("jetty.base",tmpDir.getAbsolutePath());

        // Set Up Server

        server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(0); // let OS pick
        server.addConnector(connector);

        ContextHandlerCollection contexts = new ContextHandlerCollection();
        server.setHandler(contexts);

        JettyOpenEJBModule openejb = new JettyOpenEJBModule(server,contexts);
        openejb.setAppScanDirectory(appsDir);
        openejb.setAppWorkSubDirectory(workDir.getName());
        server.addBean(openejb);

        server.start();
        if (LOG.isDebugEnabled())
        {
            LOG.debug("dump: {}",server.dump());
        }

        assertThat("openejb.isStarted",openejb.isStarted(),is(true));

        String host = connector.getHost();
        if (host == null)
        {
            host = "localhost";
        }
        int port = connector.getLocalPort();
        serverURI = new URI(String.format("http://%s:%d/",host,port));

    }

    @AfterClass
    public static void stopServer() throws Exception
    {
        if (server != null)
        {
            server.stop();
        }
    }

    @Test
    public void getDumpJndi() throws UnknownHostException, IOException
    {
        String resp = new SimpleRequest(serverURI).getString("/webTester/tester/jndi-dump/java:");
        assertThat("response",resp,containsString("java:global/raceApp/ejbTeam/TeamInfoEJB"));
    }

    @Test
    public void getFindTeamInfo() throws UnknownHostException, IOException
    {
        String resp = new SimpleRequest(serverURI).getString("/webTester/tester/find-team-info/");
        assertThat("response",resp,containsString("TeamInfoHome (narrowed) => proxy=jetty.demo.ejb.TeamInfo;deployment=TeamInfoEJB"));
    }

    @Test
    public void getTeamInfo() throws UnknownHostException, IOException
    {
        String resp = new SimpleRequest(serverURI).getString("/webTester/tester/team-info/cherry");
        assertThat("response",resp,containsString("cherry"));
    }

    @Test
    public void saveAllResults() throws UnknownHostException, IOException
    {
        File outputDir = MavenTestingUtils.getTargetTestingDir("webtester-reports");
        FS.ensureDirExists(outputDir);
        String reports[][] = new String[][] {
                // [path to report], [output report filename]
                { "/webTester/tester/jndi-dump/java:", "jetty-jndi-dump-java.html" }, //
                { "/webTester/tester/jndi-dump-native/java:", "jetty-jndi-dump-native-java.txt" }, //
                { "/webTester/tester/find-team-info/", "jetty-find-team-info.txt" }, //
                { "/webTester/tester/team-info/cherry", "jetty-team-info-chery.txt" }, //
        };
        for (String[] req : reports)
        {
            String resp = new SimpleRequest(serverURI).getString(req[0]);
            File outputFile = new File(outputDir,req[1]);
            try (StringReader reader = new StringReader(resp); FileWriter writer = new FileWriter(outputFile,false))
            {
                IO.copy(reader,writer);
                LOG.info("Wrote file: " + outputFile);
            }
        }
    }

    @Test
    public void testJNDI() throws UnknownHostException, IOException
    {
        String resp = new SimpleRequest(serverURI).getString("/webTester/tester/jndi-lookup/java:");
        assertThat("response",resp,containsString("java:global/raceApp/ejbTeam/TeamInfoEJB"));
    }
}
