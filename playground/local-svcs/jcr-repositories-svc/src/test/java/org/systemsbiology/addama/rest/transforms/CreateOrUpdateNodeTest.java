package org.systemsbiology.addama.rest.transforms;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.systemsbiology.addama.rest.AddamaKeywords.created_at;
import static org.systemsbiology.addama.rest.AddamaKeywords.last_modified_at;

import javax.jcr.Node;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springmodules.jcr.JcrTemplate;
import org.systemsbiology.addama.JcrTestHelper;
import org.systemsbiology.addama.jcr.callbacks.GetNodeAtPathJcrCallback;

/**
 * This test suite was created to add a test for a bug where the last modified
 * field of an Addama node was not getting set upon update.
 * 
 * See src/test/resources/org/systemsbiology/addama/rest/transforms/
 * CreateOrUpdateNodeTest-context.xml for spring configuration specific to this
 * test.
 * 
 * See src/test/resources/jackrabbit-testrepo.xml for spring configuration for
 * an in-memory JCR that can be used by any test suite
 * 
 * @author deflaux
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class CreateOrUpdateNodeTest {

	@Autowired
	private JcrTestHelper helper = null;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		// Ensure that the JCR session remains open for the duration
		// of the test case
		helper.obtainSession();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		helper.releaseSession();
	}

	/**
	 * Test method for
	 * {@link org.systemsbiology.addama.rest.transforms.CreateOrUpdateNode#doCreate(javax.jcr.Node, org.json.JSONObject)}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testLastModified() throws Exception {

		// Dev note: this could be refactored to use
		// hellper.createOrUpdateNode(path, json) but you really want to see the
		// API being tested in the actual unit test code so I did not do that.

		JcrTemplate jcrTemplate = helper.getJcrTemplate();
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
