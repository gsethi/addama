package org.systemsbiology.addama.coresvcs.gae.services;

import com.google.appengine.api.datastore.*;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import org.apache.commons.lang.StringUtils;
import org.systemsbiology.addama.commons.gae.dataaccess.DatastoreServiceTemplate;
import org.systemsbiology.addama.commons.gae.dataaccess.callbacks.DeleteEntityTransactionCallback;
import org.systemsbiology.addama.commons.gae.dataaccess.callbacks.PutEntityTransactionCallback;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: aeakin
 * Date: Oct 5, 2010
 * Time: 9:09:39 AM
 * To change this template use File | Settings | File Templates.
 */
public class WhiteLists {
     private static final Logger log = Logger.getLogger(WhiteLists.class.getName());

    private DatastoreServiceTemplate template;

    private MemcacheService memcacheService = MemcacheServiceFactory.getMemcacheService(getClass().getName());

    public void setTemplate(DatastoreServiceTemplate template) {
        this.template = template;
    }

    public void addLoggedInWhiteListUser(String accessPath){
        log.fine("addLoggedInWhiteListUser()");

        UserService userService = UserServiceFactory.getUserService();
        User user = userService.getCurrentUser();
        String userUri = "/addama/users/"+user.getEmail();
        Entity e = new Entity(KeyFactory.createKey("white-list", userUri+accessPath));
        e.setProperty("hasAccess", false);
        e.setProperty("uri",accessPath);
        e.setProperty("userUri", userUri);
        template.inTransaction(new PutEntityTransactionCallback(e));
        return;
    }

    public boolean isUserInWhiteList(String userUri,String accessPath){

        log.fine("isUserInWhiteList(" + userUri + ", " + accessPath + ")");
        try {

            String tempUri = accessPath;
                //if there are no users in the white list, then move up the stack of uri's and see if there
                //is one to query
            int entityCount = 0;
            while(!tempUri.equals("")){
                //first check memcache - if entry is there then the user has access
                 if (memcacheService.contains(tempUri)) {
                    entityCount =  (Integer) memcacheService.get(tempUri);
                 }
                else{
                     Query q = new Query("white-list");
                     q.addFilter("uri",Query.FilterOperator.EQUAL,tempUri);
                     PreparedQuery pq = template.prepare(q);
                     entityCount = pq.countEntities();
                 }

                if(entityCount != 0)
                    break;

                tempUri = StringUtils.substringBeforeLast(tempUri,"/");
            }

            if(tempUri.equals(""))
                return true;

            //check and see if user is in this white list
            if(memcacheService.contains(userUri+tempUri)){
                Entity e = (Entity) memcacheService.get(userUri+tempUri);
                if(e!= null){
                    return Boolean.parseBoolean(e.getProperty("hasAccess").toString());
                }
                else
                    return false;
            }

            //user not found in cache, so get out of datastore and put in cache
            Key k = KeyFactory.createKey("white-list", userUri+tempUri);
            Entity e = template.getEntityByKey(k);
            if(e != null){
                memcacheService.put(userUri+tempUri,e);
                return true;
            }
            else{
                memcacheService.put(userUri+tempUri,null);
                return false;
            }
        } catch (EntityNotFoundException e) {
            log.warning("isUserInWhiteList(" + userUri+ ", " + accessPath + "):" + e);
            return false;
        }
    }

    public String[][] getWhiteListUsers(){
        log.fine("getWhiteListUsers()");

        Query q = new Query("white-list");

        ArrayList<String[]> userEmails = new ArrayList<String[]>();
        String hasAccess = "false";
        String accessPath = "";
        String userEmail = "";
        PreparedQuery pq = template.prepare(q);
        Iterator<Entity> itr = pq.asIterator();
        while (itr.hasNext()) {
            Entity e = itr.next();
             if (e.hasProperty("hasAccess")) {
                hasAccess = e.getProperty("hasAccess").toString();
            }
            if(e.hasProperty("uri")){
               accessPath = e.getProperty("uri").toString();
            }
            if(e.hasProperty("userUri")){
                String userUri = e.getProperty("userUri").toString();
                userEmail = StringUtils.substringAfterLast(userUri,"/");
            }
            String[] whiteListEntry = new String[3];
            whiteListEntry[0]=userEmail;
            whiteListEntry[1]=hasAccess;
            whiteListEntry[2]=accessPath;
            userEmails.add(whiteListEntry);
        }

        return userEmails.toArray(new String[userEmails.size()][3]);
    }

    public void addWhiteListUser(String userEmail, String accessPath){
        log.fine("addWhiteListUser()");
        String userUri = "/addama/users/" + userEmail;
        Entity e = new Entity(KeyFactory.createKey("white-list", userUri+accessPath));
        e.setProperty("hasAccess", false);
        e.setProperty("uri", accessPath);
        e.setProperty("userUri", userUri);
        template.inTransaction(new PutEntityTransactionCallback(e));
        return;

    }

    public void deleteWhiteListUser(String userEmail, String accessPath){
        log.fine("deleteWhiteListUser()");
        if (!StringUtils.isEmpty(userEmail) && !StringUtils.isEmpty(accessPath)) {
            template.inTransaction(new DeleteEntityTransactionCallback(KeyFactory.createKey("white-list", "/addama/users/"+userEmail+accessPath)));
        }

    }

    public void grantWhiteListAccess(String userEmail, String accessPath){
        log.fine("grantWhiteListAccess()");
        Entity e = new Entity(KeyFactory.createKey("white-list", "/addama/users/"+userEmail+accessPath));
        e.setProperty("hasAccess", true);
        e.setProperty("uri", accessPath);
        e.setProperty("userUri", "/addama/users/"+userEmail);
        template.inTransaction(new PutEntityTransactionCallback(e));
    }
}
