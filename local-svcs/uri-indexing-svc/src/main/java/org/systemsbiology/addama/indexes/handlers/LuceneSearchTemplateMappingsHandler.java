package org.systemsbiology.addama.indexes.handlers;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.store.Directory;
import org.json.JSONObject;
import org.springmodules.lucene.index.support.FSDirectoryFactoryBean;
import org.springmodules.lucene.search.core.DefaultLuceneSearchTemplate;
import org.springmodules.lucene.search.core.LuceneSearchTemplate;
import org.springmodules.lucene.search.factory.SimpleSearcherFactory;
import org.systemsbiology.addama.jsonconfig.Mapping;
import org.systemsbiology.addama.jsonconfig.MappingsHandler;
import org.systemsbiology.addama.jsonconfig.impls.MappingPropertyByIdContainer;

import java.util.Map;

import static org.systemsbiology.addama.indexes.handlers.FSDirectory.getFSDirectory;

/**
 * @author hrovira
 */
public class LuceneSearchTemplateMappingsHandler extends MappingPropertyByIdContainer<LuceneSearchTemplate> implements MappingsHandler {

    public LuceneSearchTemplateMappingsHandler(Map<String, LuceneSearchTemplate> map) {
        super(map);
    }

    public void handle(Mapping mapping) throws Exception {
        JSONObject item = mapping.JSON();

        FSDirectoryFactoryBean fsDirectory = getFSDirectory(item.getString("luceneStore"));
        Analyzer analyzer = new SimpleAnalyzer();

        SimpleSearcherFactory searcherFactory = new SimpleSearcherFactory();
        searcherFactory.setDirectory((Directory) fsDirectory.getObject());

        DefaultLuceneSearchTemplate searchTemplate = new DefaultLuceneSearchTemplate();
        searchTemplate.setSearcherFactory(searcherFactory);
        searchTemplate.setAnalyzer(analyzer);
        searchTemplate.afterPropertiesSet();

        addValue(mapping, searchTemplate);
    }

}
