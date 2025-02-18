/*******************************************************************************
* Copyright (c) 2020, 2022 IBM Corporation and others.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v. 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0.
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     IBM Corporation - initial API and implementation
*******************************************************************************/
package io.openliberty.tools.langserver.lemminx;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.lemminx.commons.BadLocationException;
import org.eclipse.lemminx.dom.DOMDocument;
import org.eclipse.lemminx.dom.DOMElement;
import org.eclipse.lemminx.dom.DOMNode;
import org.eclipse.lemminx.services.extensions.CompletionParticipantAdapter;
import org.eclipse.lemminx.services.extensions.ICompletionRequest;
import org.eclipse.lemminx.services.extensions.ICompletionResponse;
import org.eclipse.lemminx.utils.XMLPositionUtility;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.InsertReplaceEdit;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import io.openliberty.tools.langserver.lemminx.models.feature.Feature;
import io.openliberty.tools.langserver.lemminx.services.FeatureService;
import io.openliberty.tools.langserver.lemminx.services.SettingsService;
import io.openliberty.tools.langserver.lemminx.util.LibertyConstants;
import io.openliberty.tools.langserver.lemminx.util.LibertyUtils;

public class LibertyCompletionParticipant extends CompletionParticipantAdapter {

    @Override
    public void onXMLContent(ICompletionRequest request, ICompletionResponse response, CancelChecker cancelChecker)
            throws IOException, BadLocationException {
        if (!LibertyUtils.isServerXMLFile(request.getXMLDocument()))
            return;    

        LibertyUtils.getVersion(request.getXMLDocument());

        DOMElement parentElement = request.getParentElement();
        if (parentElement == null || parentElement.getTagName() == null)
            return;

        // if the parent element of cursor is a <feature>
        // provide the liberty features as completion options
        if (parentElement.getTagName().equals(LibertyConstants.FEATURE_ELEMENT)) {
            List<String> existingFeatures = new ArrayList<>();
            // collect existing features
            if (parentElement.getParentNode() != null
                    && parentElement.getParentNode().getNodeName().equals(LibertyConstants.FEATURE_MANAGER_ELEMENT)) {
                existingFeatures = collectExistingFeatures(parentElement.getParentNode());
            }
            List<CompletionItem> featureCompletionItems = buildCompletionItems(parentElement, request.getXMLDocument(),
                    existingFeatures);
            featureCompletionItems.stream().forEach(item -> response.addCompletionItem(item));
        }
    }

    private CompletionItem buildFeatureCompletionItem(Feature feature, DOMElement featureElement,
            DOMDocument document) {
        String featureName = feature.getWlpInformation().getShortName();

        // Build a text edit to replace whatever is inside <feature></feature>
        // with the completion result
        Range range = XMLPositionUtility.createRange(featureElement.getStartTagCloseOffset() + 1,
                featureElement.getEndTagOpenOffset(), document);
        Either<TextEdit, InsertReplaceEdit> edit = Either.forLeft(new TextEdit(range, featureName));

        // Build the completion item to return to the client
        CompletionItem item = new CompletionItem();
        item.setTextEdit(edit);
        item.setLabel(featureName);
        item.setDocumentation(Either.forLeft(feature.getShortDescription()));
        return item;
    }

    private List<CompletionItem> buildCompletionItems(DOMElement featureElement, DOMDocument domDocument,
            List<String> existingFeatures) {

        String libertyVersion = LibertyUtils.getVersion(domDocument);

        final int requestDelay = SettingsService.getInstance().getRequestDelay();
        List<Feature> features = FeatureService.getInstance().getFeatures(libertyVersion, requestDelay, domDocument.getDocumentURI());

        // filter out features that are already specified in the featureManager block
        List<CompletionItem> uniqueFeatureCompletionItems = features.stream()
                .filter(feature -> !existingFeatures.contains(feature.getWlpInformation().getShortName()))
                .map(feat -> buildFeatureCompletionItem(feat, featureElement, domDocument)).collect(Collectors.toList());

        return uniqueFeatureCompletionItems;
    }

    private List<String> collectExistingFeatures(DOMNode featureManager) {
        List<String> includedFeatures = new ArrayList<>();
        List<DOMNode> features = featureManager.getChildren();
        for (DOMNode featureNode : features) {
            DOMNode featureTextNode = (DOMNode) featureNode.getChildNodes().item(0);
            // skip nodes that do not have any text value (ie. comments)
            if (featureNode.getNodeName().equals(LibertyConstants.FEATURE_ELEMENT) && featureTextNode != null) {
                String featureName = featureTextNode.getTextContent();
                includedFeatures.add(featureName);
            }
        }
        return includedFeatures;
    }
}
