package org.systemsbiology.addama.coresvcs.indexes.handlers;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.store.Directory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.core.io.FileSystemResource;
import org.springmodules.lucene.index.core.DefaultLuceneIndexTemplate;
import org.springmodules.lucene.index.core.LuceneIndexTemplate;
import org.springmodules.lucene.index.factory.IndexFactory;
import org.springmodules.lucene.index.support.FSDirectoryFactoryBean;
import org.springmodules.lucene.index.support.SimpleIndexFactoryBean;
import org.springmodules.lucene.search.core.DefaultLuceneSearchTemplate;
import org.springmodules.lucene.search.core.LuceneSearchTemplate;
import org.springmodules.lucene.search.factory.SimpleSearcherFactory;
import org.systemsbiology.addama.registry.JsonConfigHandler;

import java.io.File;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author hrovira
 */
public class LuceneTemplatesJsonConfigHandler implements JsonConfigHandler {
    private static final Logger log = Logger.getLogger(LuceneTemplatesJsonConfigHandler.class.getName());

    private final Map<String, LuceneIndexTemplate> indexTemplatesByUri;
    private final Map<String, LuceneSearchTemplate> searchTemplatesByUri;

    public LuceneTemplatesJsonConfigHandler(Map<String, LuceneIndexTemplate> indexMap, Map<String, LuceneSearchTemplate> searchMap) {
        this.indexTemplatesByUri = indexMap;
        this.searchTemplatesByUri = searchMap;
    }

    public void handle(JSONObject configuration) throws Exception {
        if (configuration.has("locals")) {
            JSONArray locals = configuration.getJSONArray("locals");
            for (int i = 0; i < locals.length(); i++) {
                JSONObject local = locals.getJSONObject(i);

                String uri = local.getString("uri");
                String luceneStore = local.getString("luceneStore");

                FSDirectoryFactoryBean fsDirectory = getFSDirectory(luceneStore);
                Analyzer analyzer = new SimpleAnalyzer();

                this.indexTemplatesByUri.put(uri, getIndexTemplate(fsDirectory, analyzer));
                this.searchTemplatesByUri.put(uri, getSearchTemplate(fsDirectory, analyzer));
            }
        }
    }

    /*
     * Private Methods
     */

    private FSDirectoryFactoryBean getFSDirectory(String location) throws Exception {
        File f = new File(location);
        if (!f.exists()) {
            if (f.mkdirs()) {
                log.info("creating directory for index store at: " + location);
            }
        }

        FSDirectoryFactoryBean fsDirectory = new FSDirectoryFactoryBean();
        fsDirectory.setLocation(new FileSystemResource(location));
        fsDirectory.afterPropertiesSet();
        return fsDirectory;
    }

    private LuceneIndexTemplate getIndexTemplate(FSDirectoryFactoryBean fsDirectory, Analyzer analyzer) throws Exception {
        SimpleIndexFactoryBean indexFactory = new SimpleIndexFactoryBean();
        indexFactory.setDirectory((Directory) fsDirectory.getObject());
        indexFactory.setAnalyzer(analyzer);
        indexFactory.setCreate(true);
        indexFactory.afterPropertiesSet();

        DefaultLuceneIndexTemplate indexTemplate = new DefaultLuceneIndexTemplate();
        indexTemplate.setIndexFactory((IndexFactory) indexFactory.getObject());
        indexTemplate.setAnalyzer(analyzer);
        indexTemplate.afterPropertiesSet();
        return indexTemplate;
    }

    private LuceneSearchTemplate getSearchTemplate(FSDirectoryFactoryBean fsDirectory, Analyzer analyzer) throws Exception {
        SimpleSearcherFactory searcherFactory = new SimpleSearcherFactory();
        searcherFactory.setDirectory((Directory) fsDirectory.getObject());

        DefaultLuceneSearchTemplate searchTemplate = new DefaultLuceneSearchTemplate();
        searchTemplate.setSearcherFactory(searcherFactory);
        searchTemplate.setAnalyzer(analyzer);
        searchTemplate.afterPropertiesSet();
        return searchTemplate;
    }
}
