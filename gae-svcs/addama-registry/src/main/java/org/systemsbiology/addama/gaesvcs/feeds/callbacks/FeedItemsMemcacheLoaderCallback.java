package org.systemsbiology.addama.gaesvcs.feeds.callbacks;

import java.io.Serializable;

import org.json.JSONObject;
import org.systemsbiology.addama.commons.gae.dataaccess.MemcachePaginatedLoaderCallback;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;

/**
 * Return a paginated set of feed items sorted by date to return the most recent feed items first
 *  
 * @author hrovira
 */
public class FeedItemsMemcacheLoaderCallback implements MemcachePaginatedLoaderCallback {
	
	private final DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();


	public Serializable getCacheableObject(String feedUri, int limit, int offset) throws Exception {
        JSONObject json = new JSONObject();

        Key pk = KeyFactory.createKey("feeds", feedUri);
        Query query = new Query("feed-item", pk);
        query.addSort("date", Query.SortDirection.DESCENDING);

        PreparedQuery pq = datastoreService.prepare(query);
        for (Entity entity : pq.asIterable(FetchOptions.Builder.withLimit(limit).offset(offset))) {
            JSONObject item = new JSONObject();
            item.put("author", entity.getProperty("author").toString());
            item.put("text", entity.getProperty("text").toString());
            item.put("date", entity.getProperty("date").toString());
            // Handle optional fields
            if(entity.hasProperty("title")) {
                item.put("title", entity.getProperty("title").toString());            	
            }
            if(entity.hasProperty("link")) {
                item.put("link", entity.getProperty("link").toString());            	
            }
            json.append("items", item);
        }

        return json.toString();
    }
}
