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
package org.systemsbiology.addama.jcr.util;

import org.apache.commons.lang.StringUtils;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.multipart.MultipartFile;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.servlet.ServletContext;
import java.io.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
public class AddamaFileAppender implements ServletContextAware {
    private static final Logger log = Logger.getLogger(AddamaFileAppender.class.getName());

    private ServletContext servletContext;

    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    public Node appendFile(Node node, MultipartFile multipartFile) throws IOException, RepositoryException {
        if (multipartFile == null) return null;

        String fileName = multipartFile.getOriginalFilename();
        if (StringUtils.isEmpty(fileName)) return null;

        Node fileNode = node.addNode(fileName);
        fileNode.addMixin("mix:referenceable");
        fileNode.setProperty("addama-type", "addama-file");
        fileNode.setProperty("jcr:mimeType", servletContext.getMimeType(fileName));
        fileNode.setProperty("jcr:encoding", "");
        fileNode.setProperty("jcr:data", multipartFile.getInputStream());
        fileNode.setProperty("jcr:lastModified", Calendar.getInstance());
        fileNode.setProperty("file_size", multipartFile.getSize());
        return fileNode;
    }

    public Node overrideFile(Node node, MultipartFile multipartFile) throws IOException, RepositoryException {
        if (multipartFile == null) return null;

        String fileName = multipartFile.getOriginalFilename();
        if (StringUtils.isEmpty(fileName)) return null;

        if (node.hasNode(fileName)) {
            Node fileNode = node.getNode(fileName);
            fileNode.setProperty("jcr:mimeType", servletContext.getMimeType(fileName));
            fileNode.setProperty("jcr:encoding", "");
            fileNode.setProperty("jcr:data", multipartFile.getInputStream());
            fileNode.setProperty("jcr:lastModified", Calendar.getInstance());
            fileNode.setProperty("file_size", multipartFile.getSize());
            return fileNode;
        }

        return appendFile(node, multipartFile);
    }

    public Node appendToFileCreateIfNeeded(Node node, String file, String... appendContents) throws RepositoryException, IOException {
        InputStream[] inputStreams = getInputStreams(appendContents);
        return appendToFileCreateIfNeeded(node, file, inputStreams);
    }

    public Node appendToFileCreateIfNeeded(Node node, String file, MultipartFile... files) throws RepositoryException, IOException {
        InputStream[] inputStreams = getInputStreams(files);
        return appendToFileCreateIfNeeded(node, file, inputStreams);
    }

    public Node appendInputStream(Node node, InputStream inputStream, String fileName) throws RepositoryException {
        Node fileNode = node.addNode(fileName);
        fileNode.setProperty("addama-type", "addama-file");
        fileNode.addMixin("mix:referenceable");
        fileNode.setProperty("jcr:mimeType", "application/octet-stream");
        fileNode.setProperty("jcr:encoding", "");
        fileNode.setProperty("jcr:data", inputStream);
        fileNode.setProperty("jcr:lastModified", Calendar.getInstance());
        return fileNode;
    }

    /*
    * Private Methods
    */
    private Node appendToFileCreateIfNeeded(Node node, String file, InputStream[] appendStreams) throws RepositoryException, IOException {
        PipedInputStream pipedInputStream = new PipedInputStream();
        InputStreamAppender streamAppender = new InputStreamAppender(new PipedOutputStream(pipedInputStream));

        try {
            if (node.hasNode(file)) {
                Node fileNode = node.getNode(file);
                Node contentNode = fileNode;
                if (fileNode.hasNode("jcr:content")) {
                    contentNode = fileNode.getNode("jcr:content");
                }

                if (contentNode.hasProperty("jcr:data")) {
                    Property dataProp = contentNode.getProperty("jcr:data");
                    streamAppender.append(dataProp.getStream());
                }

                streamAppender.append(appendStreams);

                log.info("before stream appender started: existing file");
                new Thread(streamAppender).start();

                contentNode.setProperty("jcr:data", pipedInputStream);

                return fileNode;
            } else {
                streamAppender.append(appendStreams);

                log.info("before stream appender started: new file");
                new Thread(streamAppender).start();

                return appendInputStream(node, pipedInputStream, file);
            }
        } catch (RepositoryException ex) {
            streamAppender.finishStreaming();
            throw ex;
        } finally {
            streamAppender.waitForStreamToFinish();
        }
    }

    private InputStream[] getInputStreams(String... appendContents) {
        ArrayList<InputStream> inputStreams = new ArrayList<InputStream>();
        for (String content : appendContents) {
            if (!StringUtils.isEmpty(content)) {
                inputStreams.add(new ByteArrayInputStream(content.getBytes()));
            }
        }
        return inputStreams.toArray(new InputStream[inputStreams.size()]);
    }

    private InputStream[] getInputStreams(MultipartFile... files) throws IOException {
        InputStream[] inputStreams = new InputStream[files.length];
        for (int i = 0; i < files.length; i++) {
            inputStreams[i] = files[i].getInputStream();
        }
        return inputStreams;
    }

    protected String getContentType(MultipartFile multipartFile, String fileName) throws IOException {
        String contentType = servletContext.getMimeType(fileName);
        if (!StringUtils.isEmpty(contentType)) {
            return contentType;
        }

        contentType = multipartFile.getContentType();
        if (fileName.endsWith(".tif")) return "image/tiff";
        if (fileName.endsWith(".tiff")) return "image/tiff";
        if (fileName.endsWith(".jpg")) return "image/jpeg";
        if (fileName.endsWith(".jpeg")) return "image/jpeg";
        if (fileName.endsWith(".htm")) return "text/html";

        if (StringUtils.isEmpty(contentType)) return "application/octet-stream";
        return multipartFile.getContentType();
    }
}