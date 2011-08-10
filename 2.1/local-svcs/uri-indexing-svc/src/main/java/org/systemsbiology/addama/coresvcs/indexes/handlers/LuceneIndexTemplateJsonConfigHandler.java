package org.systemsbiology.addama.coresvcs.indexes.handlers;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.store.Directory;
import org.json.JSONObject;
import org.springmodules.lucene.index.core.DefaultLuceneIndexTemplate;
import org.springmodules.lucene.index.core.LuceneIndexTemplate;
import org.springmodules.lucene.index.factory.IndexFactory;
import org.springmodules.lucene.index.support.FSDirectoryFactoryBean;
import org.springmodules.lucene.index.support.SimpleIndexFactoryBean;
import org.systemsbiology.addama.jsonconfig.impls.GenericMapJsonConfigHandler;

import java.util.Map;

import static org.systemsbiology.addama.coresvcs.indexes.handlers.FSDirectory.getFSDirectory;

/**
 * @author hrovira
 */
public class LuceneIndexTemplateJsonConfigHandler extends GenericMapJsonConfigHandler<LuceneIndexTemplate> {

    public LuceneIndexTemplateJsonConfigHandler(Map<String, LuceneIndexTemplate> map) {
        super(map, "luceneStore");
    }

    public LuceneIndexTemplate getSpecific(JSONObject item) throws Exception {
        FSDirectoryFactoryBean fsDirectory = getFSDirectory(item.getString(propertyName));

        Analyzer analyzer = new SimpleAnalyzer();

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

}
