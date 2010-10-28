package org.systemsbiology.addama.rest.transforms;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.systemsbiology.addama.rest.AddamaKeywords.created_at;
import static org.systemsbiology.addama.rest.AddamaKeywords.last_modified_at;

import java.util.logging.Logger;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springmodules.jcr.JcrTemplate;
import org.springmodules.jcr.SessionFactory;
import org.springmodules.jcr.SessionFactoryUtils;
import org.systemsbiology.addama.jcr.callbacks.GetNodeAtPathJcrCallback;
import org.systemsbiology.addama.rest.JcrCrudController;

/**
 * This test suite makes use of an in-memory JCR.  Note that the JCR
 * lives for the duration of the suite, so subsequent tests will see
 * changes to the JCR made by prior tests.  For test isolation, merely
 * place the tests in separate files.
 *
 * See
 * src/test/resources/org/systemsbiology/addama/rest/transforms/CreateOrUpdateNodeTest-context.xml
 * for spring configuration specific to this test.
 *
 * See src/test/resources/jackrabbit-testrepo.xml for spring
 * configuration for an in-memory JCR that can be used by any test
 * suite
 *
 * @author deflaux
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class CreateOrUpdateNodeTest {
    private static final Logger log =
        Logger.getLogger(CreateOrUpdateNodeTest.class.getName());
    private SessionFactory sf;
    private Session session;

    @Autowired
    private JcrTemplate jcrTemplate = null;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // Ensure that the JCR session remains open for the duration
        // of the test case
        sf = jcrTemplate.getSessionFactory();
        session = SessionFactoryUtils.getSession(sf, true);
        TransactionSynchronizationManager.bindResource(sf,
                sf.getSessionHolder(session));

        // TODO: figure out how to check whether the JCR repo is
        // "empty" and perform this check before the first test case.
        // We assume none would edit the test spring configuration to
        // point to a production JCR, but it never hurts to have a
        // healthy amount of paranoia
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        TransactionSynchronizationManager.unbindResource(sf);
        SessionFactoryUtils.releaseSession(session, sf);
    }

    /**
     * Test method for {@link org.systemsbiology.addama.rest.transforms.CreateOrUpdateNode#doCreate(javax.jcr.Node, org.json.JSONObject)}.
     * @throws Exception
     */
    @Test
    public void testLastModified() throws Exception {

        Node node = (Node) jcrTemplate.execute(new GetNodeAtPathJcrCallback("/"));

        JSONObject jsonCreate = new JSONObject();
        jsonCreate.put("uri", "/addama/applications/edgelimma");
        jsonCreate.put("proxy", "http://jefferson:9081/cgi-bin");
        jsonCreate.put("excludeBaseUri", true);

        CreateOrUpdateNode createNode = new CreateOrUpdateNode();
        createNode.doCreate(node, jsonCreate);
        jcrTemplate.save();

        assertTrue(node.hasProperty(created_at.word()));
        assertFalse(node.hasProperty(last_modified_at.word()));

        JSONObject jsonUpdate = new JSONObject();
        jsonUpdate.put("foo", "bar");

        createNode.doCreate(node, jsonUpdate);
        jcrTemplate.save();

        assertTrue(node.hasProperty(created_at.word()));
        assertTrue(node.hasProperty(last_modified_at.word()));
    }
}
