package org.systemsbiology.addama.fsutils.rest;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.Assert.*;
import static org.systemsbiology.addama.fsutils.rest.HttpRepositories.getRepositoryUri;
import static org.systemsbiology.addama.fsutils.rest.HttpRepositories.getResourcePath;
import static org.systemsbiology.addama.fsutils.rest.UriScheme.path;

/**
 * @author hrovira
 */
public class HttpRepositoriesTest {
    private MockHttpServletRequest request;

    @Before
    public void setup() {
        request = new MockHttpServletRequest();
    }

    @Test
    public void getRepositoryUri_simple() {
        request.setRequestURI("/x/y/z/path/w/t/r");

        String repositoryUri = getRepositoryUri(request, path);
        assertNotNull(repositoryUri);
        assertEquals("/x/y/z", repositoryUri);
    }

    @Test
    public void getRepositoryUri_noUriScheme() {
        request.setRequestURI("/x/y/z/w/t/r");

        String repositoryUri = getRepositoryUri(request, path);
        assertNotNull(repositoryUri);
        assertEquals("/x/y/z/w/t/r", repositoryUri);
    }

    @Test
    public void getResourcePath_simple() {
        request.setRequestURI("/x/y/z/path/w/t/r");

        String resourcePath = getResourcePath(request, path);
        assertNotNull(resourcePath);
        assertEquals("/w/t/r", resourcePath);
    }

    @Test
    public void getResourcePath_noUriScheme() {
        request.setRequestURI("/x/y/z/w/t/r");

        assertNull(getResourcePath(request, path));
    }

    @Test
    public void getResourcePath_suffixed() {
        request.setRequestURI("/x/y/z/path/w/t/r/dir");

        String resourcePath = getResourcePath(request, path, "/dir");
        assertNotNull(resourcePath);
        assertEquals("/w/t/r", resourcePath);
    }

    @Test
    public void getResourcePath_nosuffix() {
        request.setRequestURI("/x/y/z/path/w/t/r");

        String resourcePath = getResourcePath(request, path, "/dir");
        assertNotNull(resourcePath);
        assertEquals("/w/t/r", resourcePath);
    }

}
