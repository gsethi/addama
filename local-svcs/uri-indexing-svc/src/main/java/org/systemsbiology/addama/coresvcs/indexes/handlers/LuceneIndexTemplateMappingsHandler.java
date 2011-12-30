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
import org.systemsbiology.addama.jsonconfig.Mapping;
import org.systemsbiology.addama.jsonconfig.MappingsHandler;
import org.systemsbiology.addama.jsonconfig.impls.MappingPropertyByIdContainer;

import java.util.Map;

import static org.systemsbiology.addama.coresvcs.indexes.handlers.FSDirectory.getFSDirectory;

/**
 * @author hrovira
 */
public class LuceneIndexTemplateMappingsHandler extends MappingPropertyByIdContainer<LuceneIndexTemplate> implements MappingsHandler {

    public LuceneIndexTemplateMappingsHandler(Map<String, LuceneIndexTemplate> map) {
        super(map);
    }

    public void handle(Mapping mapping) throws Exception {
        JSONObject item = mapping.JSON();

        FSDirectoryFactoryBean fsDirectory = getFSDirectory(item.getString("luceneStore"));

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

        addValue(mapping, indexTemplate);
    }

}
