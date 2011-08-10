package org.systemsbiology.addama.coresvcs.indexes.handlers;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.SimpleAnalyzer;
import org.apache.lucene.store.Directory;
import org.json.JSONObject;
import org.springmodules.lucene.index.support.FSDirectoryFactoryBean;
import org.springmodules.lucene.search.core.DefaultLuceneSearchTemplate;
import org.springmodules.lucene.search.core.LuceneSearchTemplate;
import org.springmodules.lucene.search.factory.SimpleSearcherFactory;
import org.systemsbiology.addama.jsonconfig.impls.GenericMapJsonConfigHandler;

import java.util.Map;

import static org.systemsbiology.addama.coresvcs.indexes.handlers.FSDirectory.getFSDirectory;

/**
 * @author hrovira
 */
public class LuceneSearchTemplateJsonConfigHandler extends GenericMapJsonConfigHandler<LuceneSearchTemplate> {

    public LuceneSearchTemplateJsonConfigHandler(Map<String, LuceneSearchTemplate> map) {
        super(map, "luceneStore");
    }

    @Override
    public LuceneSearchTemplate getSpecific(JSONObject item) throws Exception {
        String luceneStore = item.getString("luceneStore");
        FSDirectoryFactoryBean fsDirectory = getFSDirectory(luceneStore);
        Analyzer analyzer = new SimpleAnalyzer();

        SimpleSearcherFactory searcherFactory = new SimpleSearcherFactory();
        searcherFactory.setDirectory((Directory) fsDirectory.getObject());

        DefaultLuceneSearchTemplate searchTemplate = new DefaultLuceneSearchTemplate();
        searchTemplate.setSearcherFactory(searcherFactory);
        searchTemplate.setAnalyzer(analyzer);
        searchTemplate.afterPropertiesSet();
        return searchTemplate;
    }

}
