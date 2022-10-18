package io.openliberty.tools.test;

import static org.eclipse.lemminx.XMLAssert.r;

import java.util.List;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;

import org.eclipse.lemminx.XMLAssert;
import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lsp4j.CompletionItem;

import org.eclipse.lsp4j.WorkspaceFolder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import io.openliberty.tools.langserver.lemminx.services.LibertyProjectsManager;
import io.openliberty.tools.langserver.lemminx.util.LibertyUtils;

import static org.eclipse.lemminx.XMLAssert.*;

public class LibertyWorkspaceIT {
    static String newLine = System.lineSeparator();

    @AfterAll
    public static void tearDown() {
        LibertyProjectsManager.getInstance().cleanInstance();
        assert(LibertyProjectsManager.getInstance().getLibertyWorkspaceFolders().isEmpty());
    }

    @Test
    public void testWorkspace() throws BadLocationException, IOException, URISyntaxException {
        File testFolder = new File(System.getProperty("user.dir"));
        File serverXmlFile = new File(testFolder, "src/main/liberty/config/server.xml");

        //Configure Liberty workspace for testing
        WorkspaceFolder testWorkspace = new WorkspaceFolder(testFolder.toURI().toString());
        List<WorkspaceFolder> testWorkspaceFolders = new ArrayList<WorkspaceFolder>();
        testWorkspaceFolders.add(testWorkspace);
        LibertyProjectsManager.getInstance().setWorkspaceFolders(testWorkspaceFolders);

        String schemaFileName = "wlp-18.0.0.1.xsd";
        File schemaFile = new File(LibertyUtils.getTempDir(LibertyProjectsManager.getInstance().getWorkspaceFolder(serverXmlFile.toURI().toString())), schemaFileName);
        String serverGenXSDURI = schemaFile.toPath().toUri().toString().replace("///", "/");

        String serverXML = String.join(newLine, //
                        "<server description=\"Sample Liberty server\">", //
                        "       <feature|Manager>", //
                        "               <feature>jaxrs-2.1</feature>", //
                        "       </featureManager>", //
                        "</server>" //
        );

        XMLAssert.assertHover(serverXML, serverXmlFile.toURI().toString(), "Defines how the server loads features." + //
                        System.lineSeparator() + System.lineSeparator() + //
                        "Source: [" + schemaFileName + "](" + serverGenXSDURI + ")", //
                        r(1, 8, 1, 22));
    }

    @Test
    public void testGetFeatures() throws BadLocationException {
        File testFolder = new File(System.getProperty("user.dir"));
        File serverXmlFile = new File(testFolder, "src/main/liberty/config/server.xml");

        //Configure Liberty workspace for testing
        WorkspaceFolder testWorkspace = new WorkspaceFolder(testFolder.toURI().toString());
        List<WorkspaceFolder> testWorkspaceFolders = new ArrayList<WorkspaceFolder>();
        testWorkspaceFolders.add(testWorkspace);
        LibertyProjectsManager.getInstance().setWorkspaceFolders(testWorkspaceFolders);

        String featureListName = "featurelist-wlp-18.0.0.1.xml";
        File featurelistFile = new File(LibertyUtils.getTempDir(LibertyProjectsManager.getInstance().getWorkspaceFolder(serverXmlFile.toURI().toString())), featureListName);

                String serverXML = String.join(newLine, //
                                "<server description=\"Sample Liberty server\">", //
                                "       <featureManager>", //
                                "               <feature>|</feature>", //
                                "               <feature>servlet-3.1</feature>", //
                                "       </featureManager>", //
                                "</server>" //
                );

        CompletionItem batchCompletion = c("batch-1.0", "batch-1.0");

        // this is using wlp-18.0.0.1 which did not have any features.json in Maven Central
        // this causes the featurelist xml file to get generated in the .libertyls folder
        final int TOTAL_ITEMS = 58; // total number of available completion items

        XMLAssert.testCompletionFor(serverXML, null, serverXmlFile.toURI().toString(), TOTAL_ITEMS, batchCompletion);
                
        org.junit.jupiter.api.Assertions.assertTrue(featurelistFile.exists(), "Did not find generated featurelist file: "+featureListName);

    }
}
