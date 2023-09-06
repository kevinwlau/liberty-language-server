/*******************************************************************************
* Copyright (c) 2022 IBM Corporation and others.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v. 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0.
*
* SPDX-License-Identifier: EPL-2.0
*******************************************************************************/
package io.openliberty.tools.langserver.hover;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.net.URISyntaxException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.TextDocumentItem;
import org.junit.Test;

import io.openliberty.tools.langserver.LibertyLanguageServer;

public class BootstrapPropertyHoverTest extends AbstractHoverTest {
    
    @Test
    public void testHoverOnPropertyName() throws Exception {
        String propertyEntry = "wlp.install.dir=/some/dir";
        CompletableFuture<Hover> hover = getHover(propertyEntry, 5);

        assertEquals("The directory where the Open Liberty runtime is installed.", hover.get().getContents().getRight().getValue());
    }

    @Test
    public void testHoverOnEquivalentProperty() throws Exception {
        String propertyEntry = "com.ibm.ws.logging.message.format=simple";
        CompletableFuture<Hover> hover = getHover(propertyEntry, 5);

        assertEquals("This setting specifies the required format for the messages.log file. Valid values are `simple` or `json` format. By default, messageFormat is set to `simple`.", hover.get().getContents().getRight().getValue());
    }

    private CompletableFuture<Hover> getHover(String propertyEntry, int position) throws URISyntaxException, InterruptedException, ExecutionException {
        String filename = "bootstrap.properties";
        File file = new File(resourcesDir, filename);
        String fileURI = file.toURI().toString();

        LibertyLanguageServer lls = initializeLanguageServer(filename, new TextDocumentItem(fileURI, LibertyLanguageServer.LANGUAGE_ID, 0, propertyEntry));
        return getHover(lls, position, fileURI);
    }
}
