/*
**    Copyright (C) 2003-2010 Institute for Systems Biology
**                            Seattle, Washington, USA.
**
**    This library is free software; you can redistribute it and/or
**    modify it under the terms of the GNU Lesser General Public
**    License as published by the Free Software Foundation; either
**    version 2.1 of the License, or (at your option) any later version.
**
**    This library is distributed in the hope that it will be useful,
**    but WITHOUT ANY WARRANTY; without even the implied warranty of
**    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
**    Lesser General Public License for more details.
**
**    You should have received a copy of the GNU Lesser General Public
**    License along with this library; if not, write to the Free Software
**    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA
*/
package org.systemsbiology.addama.gaesvcs.refgenome.pojos;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author hrovira
 */
public class ChromUriBeanTest {
    private MockHttpServletRequest request;

    @Before
    public void setUp() {
        request = new MockHttpServletRequest();
        request.setServletPath("/refgenome");
    }

    @Test
    public void fullRange() {
        request.setRequestURI("/refgenome/build/chromosome/100/200");

        ChromUriBean bean = new ChromUriBean(request);
        assertEquals("build", bean.getBuild());
        assertEquals("chromosome", bean.getChromosome());
        assertEquals(new Long(100), bean.getStart());
        assertEquals(new Long(200), bean.getEnd());
    }

    @Test
    public void fullRangeWithOperation() {
        request.setRequestURI("/refgenome/build/chromosome/100/200/operation");

        ChromUriBean bean = new ChromUriBean(request);
        assertEquals("build", bean.getBuild());
        assertEquals("chromosome", bean.getChromosome());
        assertEquals(new Long(100), bean.getStart());
        assertEquals(new Long(200), bean.getEnd());
    }

    @Test
    public void noEnd() {
        request.setRequestURI("/refgenome/build/chromosome/100");
        ChromUriBean bean = new ChromUriBean(request);
        assertEquals("build", bean.getBuild());
        assertEquals("chromosome", bean.getChromosome());
        assertEquals(new Long(100), bean.getStart());
        assertNull(bean.getEnd());
    }

    @Test
    public void noRange() {
        request.setRequestURI("/refgenome/build/chromosome");
        ChromUriBean bean = new ChromUriBean(request);
        assertEquals("build", bean.getBuild());
        assertEquals("chromosome", bean.getChromosome());
        assertNull(bean.getStart());
        assertNull(bean.getEnd());
    }

    @Test
    public void noChromosome() {
        request.setRequestURI("/refgenome/build");
        ChromUriBean bean = new ChromUriBean(request);
        assertEquals("build", bean.getBuild());
        assertNull(bean.getChromosome());
        assertNull(bean.getStart());
        assertNull(bean.getEnd());
    }

    @Test
    public void noBuild() {
        request.setRequestURI("/refgenome");
        ChromUriBean bean = new ChromUriBean(request);
        assertNull(bean.getBuild());
        assertNull(bean.getChromosome());
        assertNull(bean.getStart());
        assertNull(bean.getEnd());
    }
}
