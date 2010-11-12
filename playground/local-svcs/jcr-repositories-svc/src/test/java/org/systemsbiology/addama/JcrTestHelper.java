package org.systemsbiology.addama;

import javax.jcr.Node;
import javax.jcr.Session;

import org.json.JSONObject;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springmodules.jcr.JcrTemplate;
import org.springmodules.jcr.SessionFactory;
import org.springmodules.jcr.SessionFactoryUtils;
import org.systemsbiology.addama.jcr.callbacks.GetNodeAtPathJcrCallback;
import org.systemsbiology.addama.rest.transforms.CreateOrUpdateNode;

/**
 * This test helper makes use of an in-memory Transient JCR. Each test case will
 * start with an empty JCR.
 * 
 * See src/test/resources/org/systemsbiology/addama/JcrTest-context.xml for
 * spring configuration specific to this helper.
 * 
 * See src/test/resources/jackrabbit-testrepo.xml for spring configuration for
 * an in-memory JCR that can be used by any test suite
 * 
 * @author deflaux
 */
public class JcrTestHelper {
	private SessionFactory sf;
	private Session session;
	private JcrTemplate jcrTemplate = null;

	/**
	 * @return the jcrTemplate
	 */
	public JcrTemplate getJcrTemplate() {
		return jcrTemplate;
	}

	/**
	 * @param jcrTemplate - the jcrTemplate to set
	 */
	public void setJcrTemplate(JcrTemplate jcrTemplate) {
		this.jcrTemplate = jcrTemplate;
	}

	/**
	 * Helper method to provide a way to ensure that the JCR session remains
	 * open for the (e.g. for the duration of a test case).
	 * 
	 * @throws java.lang.Exception
	 * @see releaseSession
	 */
	public void obtainSession() throws Exception {
		sf = jcrTemplate.getSessionFactory();
		session = SessionFactoryUtils.getSession(sf, true);
		TransactionSynchronizationManager.bindResource(sf, sf
				.getSessionHolder(session));
	}

	/**
	 * Helper method to release the JCR session.
	 * 
	 * @throws java.lang.Exception
	 * @see obtainSession
	 */
	public void releaseSession() throws Exception {
		TransactionSynchronizationManager.unbindResource(sf);
		SessionFactoryUtils.releaseSession(session, sf);
	}

	/**
	 * Construct a MockHttpServletRequest with our JcrTemplate attached the way
	 * the Addama controllers expect it to be
	 * 
	 * @return MockHttpServletRequest
	 */
	public MockHttpServletRequest getMockHttpServletRequest() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute("JCR_TEMPLATE", jcrTemplate);
		return request;
	}

	/**
	 * Helper method to create or update a JCR node
	 * 
	 * @param path
	 *            - the path to the node
	 * @param json
	 *            - json holding node properties to add/modify in the node
	 * @throws java.lang.Exception
	 */
	public void createOrUpdateNode(String path, String json) throws Exception {

		// Dev Node: make a new CreateOrUpdateNode object each time since it
		// caches the current time
		CreateOrUpdateNode createNode = new CreateOrUpdateNode();

		Node node = (Node) jcrTemplate.execute(new GetNodeAtPathJcrCallback(
				path));
		JSONObject jsonCreate = new JSONObject(json);
		createNode.doUpdate(node, jsonCreate);
		jcrTemplate.save();
	}
}
