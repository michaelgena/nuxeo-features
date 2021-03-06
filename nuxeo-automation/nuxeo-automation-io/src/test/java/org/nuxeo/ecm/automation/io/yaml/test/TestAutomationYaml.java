/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.automation.io.yaml.test;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.ecm.automation.core.OperationChainContribution.Operation;
import org.nuxeo.ecm.automation.io.yaml.YamlWriter;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.inject.Inject;

/**
 * @since 5.9.4
 */
@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
@Deploy({ "org.nuxeo.ecm.automation.core", "org.nuxeo.ecm.automation.io" })
@LocalDeploy("org.nuxeo.ecm.automation.io:test-chains.xml")
public class TestAutomationYaml {

    @Inject
    AutomationService service;

    protected String getYamlChain(String chainId) throws Exception {
        OperationType op = service.getOperation(chainId);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        YamlWriter.toYaml(out, op.getDocumentation());
        return new String(out.toByteArray());
    }

    protected String getYamlOp(Operation op) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        YamlWriter.toYaml(out, op);
        return new String(out.toByteArray());
    }

    protected void checkEquals(String expected, String actual) throws Exception {
        // System.err.println(expected);
        assertEquals(expected, actual);
    }

    @Test
    public void testEmptyChain() throws Exception {
        String chain = getYamlChain("empty_chain");
        // chain without operations should never happen
        checkEquals("{}\n", chain);
    }

    @Test
    public void testChain() throws Exception {
        String chain = getYamlChain("chain");
        StringBuilder res = new StringBuilder();
        res.append("description: My desc\n");
        res.append("operations:\n");
        res.append("- Context.FetchDocument\n");
        res.append("- Document.Create:\n");
        res.append("    type: Note\n");
        res.append("    name: MyDoc\n");
        res.append("    properties:\n");
        res.append("      dc:description: My Doc desc\n");
        res.append("      dc:title: My Doc\n");
        checkEquals(res.toString(), chain);
    }

    @Test
    public void testChainAlt() throws Exception {
        String chain = getYamlChain("chain_props");
        StringBuilder res = new StringBuilder();
        res.append("- Context.FetchDocument\n");
        res.append("- Document.Create:\n");
        res.append("    type: Note\n");
        res.append("    name: MyDoc\n");
        res.append("    properties:\n");
        res.append("      dc:description: My Doc desc\n");
        res.append("      dc:title: My Doc\n");
        checkEquals(res.toString(), chain);
    }

    @Test
    public void testChainWithParams() throws Exception {
        String chain = getYamlChain("chain_with_params");
        StringBuilder res = new StringBuilder();
        res.append("description: This is an awesome chain!\n");
        res.append("params:\n");
        res.append("- foo:\n");
        res.append("    type: string\n");
        res.append("    values:\n");
        res.append("    - bar\n");
        res.append("- foo2:\n");
        res.append("    type: boolean\n");
        res.append("    description: yop\n");
        res.append("operations:\n");
        res.append("- Context.FetchDocument\n");
        res.append("- Document.Create:\n");
        res.append("    type: Note\n");
        res.append("    name: MyDoc\n");
        checkEquals(res.toString(), chain);
    }

    @Test
    public void testChainSample() throws Exception {
        String chain = getYamlChain("chain_complex");
        StringBuilder res = new StringBuilder();
        res.append("- Context.FetchDocument\n");
        res.append("- Document.Create:\n");
        res.append("    type: Note\n");
        res.append("    name: note\n");
        res.append("    properties:\n");
        res.append("      dc:title: MyDoc\n");
        res.append("- Document.Copy:\n");
        res.append("    target: ../../dst\n");
        res.append("    name: note_copy\n");
        res.append("- Document.SetProperty:\n");
        res.append("    xpath: dc:description\n");
        res.append("    value: mydesc\n");
        checkEquals(res.toString(), chain);
    }

    @Test
    public void testOp() throws Exception {
        OperationType op = service.getOperation("chain_props");
        String yop = getYamlOp(op.getDocumentation().getOperations()[0]);
        assertEquals("Context.FetchDocument\n", yop);
        yop = getYamlOp(op.getDocumentation().getOperations()[1]);
        StringBuilder res = new StringBuilder();
        res.append("Document.Create:\n");
        res.append("  type: Note\n");
        res.append("  name: MyDoc\n");
        res.append("  properties:\n");
        res.append("    dc:description: My Doc desc\n");
        res.append("    dc:title: My Doc\n");
        checkEquals(res.toString(), yop);
    }

}
