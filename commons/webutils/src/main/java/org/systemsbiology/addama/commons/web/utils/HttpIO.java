package org.systemsbiology.addama.commons.web.utils;

import org.springframework.core.io.Resource;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static javax.servlet.http.HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE;
import static org.apache.commons.lang.StringUtils.*;

/**
 * @author hrovira
 */
public class HttpIO {
    public static final String CONTENT_TYPE_HEADER_KEY = "x-addama-desired-contenttype";

    public static void zip(HttpServletResponse response, String outputFileName,
                           Map<String, InputStream> inputStreamsByName) throws IOException {
        response.setHeader("Content-Disposition", "filename=\"" + outputFileName + "\"");
        response.setContentType("application/octet-stream");

        ZipOutputStream zipOS = null;
        try {
            CheckedOutputStream csum = new CheckedOutputStream(response.getOutputStream(), new CRC32());
            zipOS = new ZipOutputStream(new BufferedOutputStream(csum));

            for (Map.Entry<String, InputStream> entry : inputStreamsByName.entrySet()) {
                zipOS.putNextEntry(new ZipEntry(entry.getKey()));
                pipe(entry.getValue(), zipOS);
            }
        } finally {
            if (zipOS != null) {
                zipOS.close();
            }
        }
    }

    public static void zip(HttpServletResponse response, Resource r) throws IOException {
        String filename = r.getFilename();
        String zipfilename = substringBeforeLast(filename, ".") + ".zip";

        HashMap<String, InputStream> map = new HashMap<String, InputStream>();
        collectFiles(map, r.getFile());

        zip(response, zipfilename, map);
    }

    public static void collectFiles(Map<String, InputStream> inputStreamsByName, File... targets) throws IOException {
        if (inputStreamsByName == null || targets == null) {
            return;
        }

        for (File f : targets) {
            if (f.isDirectory()) {
                collectFiles(inputStreamsByName, f.listFiles());
            } else if (f.isFile()) {
                inputStreamsByName.put(f.getName(), new FileInputStream(f));
            }
        }
    }

    public static void pipe(InputStream inputStream, OutputStream outputStream) throws IOException {
        try {
            byte[] buffer = new byte[48000];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
        } finally {
            inputStream.close();
        }
    }

    public static void pipe_close(InputStream inputStream, OutputStream outputStream) throws IOException {
        try {
            byte[] buffer = new byte[48000];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
        } finally {
            inputStream.close();
            outputStream.close();
        }
    }

    public static String getMimeType(HttpServletRequest request, Resource r) {
        if (r == null) {
            return null;
        }
        return getMimeType(request, r.getFilename());
    }

    public static String getMimeType(HttpServletRequest request, File f) {
        if (f == null) {
            return null;
        }
        return getMimeType(request, f.getName());
    }

    public static String getMimeType(HttpServletRequest request, String filename) {
        if (isEmpty(filename)) {
            return null;
        }
        return request.getSession().getServletContext().getMimeType(filename);
    }

    public static void setContentType(HttpServletRequest request, HttpServletResponse response, String resourceName) {
        ServletContext servletContext = request.getSession().getServletContext();
        response.setContentType(servletContext.getMimeType(resourceName));
    }

    public static void clientRedirect(HttpServletResponse response, String location) {
        response.setStatus(SC_REQUEST_ENTITY_TOO_LARGE);
        response.setHeader("Location", location);
    }

    public static String getDesiredContentType(HttpServletRequest request, String defaultType) {
        String headerValue = request.getHeader(CONTENT_TYPE_HEADER_KEY);
        if (!isEmpty(headerValue)) {
            return headerValue;
        }

        String paramValue = request.getParameter(CONTENT_TYPE_HEADER_KEY);
        if (!isEmpty(paramValue)) {
            return paramValue;
        }

        return defaultType;
    }

    public static String getCleanUri(HttpServletRequest request) {
        String uri = substringAfter(request.getRequestURI(), request.getContextPath());
        uri = cleanSpaces(uri);
        return chomp(uri, "/");
    }

    public static String getCleanUri(HttpServletRequest request, String suffix) {
        String uri = getCleanUri(request);
        if (isEmpty(suffix)) {
            return uri;
        }
        return substringBeforeLast(uri, suffix);
    }

    public static String getURI(HttpServletRequest request) {
        if (request.getAttribute("x-addama-standalone-svc") != null) {
            return chomp(request.getRequestURI(), "/");
        }
        return chomp(substringAfter(request.getRequestURI(), request.getContextPath()), "/");
    }

    public static String getSpacedURI(HttpServletRequest request) {
        return cleanSpaces(getURI(request));
    }

    /**
     * Cleans encoding for spaces
     *
     * @param path - to clean
     * @return string - cleaned
     */
    public static String cleanSpaces(String path) {
        if (!isEmpty(path)) {
            path = replace(path, "%20", " ");
            path = replaceChars(path, '+', ' ');
        }
        return path;
    }

    public static String asPackage(URL url) {
        StringBuilder builder = new StringBuilder();

        String host = url.getHost();
        if (!isEmpty(host)) {
            builder.append(reverseDelimited(host, '.'));
        }

        String path = url.getPath();
        if (!isEmpty(path)) {
            builder.append(replace(path, "/", "."));
        }
        return chomp(builder.toString(), ".");
    }
}
