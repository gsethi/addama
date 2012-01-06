package org.systemsbiology.addama.appengine.util;

import com.google.appengine.api.datastore.*;
import org.json.JSONException;
import org.json.JSONObject;
import org.systemsbiology.addama.commons.gae.dataaccess.callbacks.DeleteEntityTransactionCallback;
import org.systemsbiology.addama.commons.gae.dataaccess.callbacks.PutEntityTransactionCallback;
import org.systemsbiology.addama.commons.web.views.Jsonable;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static com.google.appengine.api.datastore.DatastoreServiceFactory.getDatastoreService;
import static com.google.appengine.api.datastore.KeyFactory.createKey;
import static com.google.appengine.api.datastore.Query.FilterOperator.EQUAL;
import static java.util.Arrays.asList;
import static org.apache.commons.lang.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.systemsbiology.addama.appengine.util.Memberships.Membership.*;
import static org.systemsbiology.addama.commons.gae.Appspot.APP_ID;
import static org.systemsbiology.addama.commons.gae.dataaccess.DatastoreServiceTemplate.inTransaction;

/**
 * @author hrovira
 */
public class Memberships {
    private static final Logger log = Logger.getLogger(Memberships.class.getName());

    private static final String MEMBERSHIPS_DOMAIN_ACTIVE = "memberships-domain-active";
    private static final String MEMBERSHIPS_DOMAIN = "memberships-domain";
    private static final String MEMBERSHIPS_MODERATORS = "memberships-moderators";
    private static final String MEMBERSHIPS_URI = "memberships-uris";
    private static final String MEMBERSHIP = "membership";

    // TODO : Add memcache
    private static final DatastoreService datastore = getDatastoreService();

    /*
    * Domain Membership
    */

    public static DomainMember[] domainMembers() {
        Query q = new Query(MEMBERSHIPS_DOMAIN);
        List<DomainMember> entries = new ArrayList<DomainMember>();
        for (Entity e : datastore.prepare(q).asIterable()) {
            entries.add(new DomainMember(e));
        }
        return entries.toArray(new DomainMember[entries.size()]);
    }

    public static boolean isDomainMembershipActivated() {
        try {
            Key k = createKey(MEMBERSHIPS_DOMAIN_ACTIVE, APP_ID());
            return datastore.get(k) != null;
        } catch (EntityNotFoundException e) {
            return false;
        }
    }

    public static void activateDomainMembership(boolean enabled) {
        Key k = createKey(MEMBERSHIPS_DOMAIN_ACTIVE, APP_ID);
        if (enabled) {
            inTransaction(datastore, new PutEntityTransactionCallback(new Entity(k)));
        } else {
            inTransaction(datastore, new DeleteEntityTransactionCallback(k));
        }
    }

    public static void setDomainMembership(DomainMember... domainMembers) {
        if (domainMembers == null) {
            return;
        }

        ArrayList<Entity> members = new ArrayList<Entity>();
        ArrayList<Key> revoked = new ArrayList<Key>();

        for (DomainMember domainMember : domainMembers) {
            Key k = createKey(MEMBERSHIPS_DOMAIN, domainMember.getUser());
            Membership m = domainMember.getMembership();
            if (m != null) {
                Entity e = new Entity(k);
                e.setProperty(MEMBERSHIP, m.name());
                members.add(e);
            } else {
                revoked.add(k);
            }
        }

        inTransaction(datastore, new PutEntityTransactionCallback(members));
        inTransaction(datastore, new DeleteEntityTransactionCallback(revoked));
    }

    /*
     * Moderator
     */

    public static void setModerator(ModeratedItem moderatedItem) {
        Entity e = new Entity(createKey(MEMBERSHIPS_MODERATORS, moderatedItem.getUri()));
        e.setProperty("moderator", moderatedItem.getModerator());
        inTransaction(datastore, new PutEntityTransactionCallback(e));
    }

    public static void revokeModeration(String moderatedUri) {
        Key k = createKey(MEMBERSHIPS_MODERATORS, moderatedUri);
        inTransaction(datastore, new DeleteEntityTransactionCallback(k));
    }

    /*
    * ModeratedItem URIs
    */

    public static ModeratedItem getModeratedItem(String requestUri) {
        if (isEmpty(requestUri)) {
            return null;
        }

        try {
            Entity e = datastore.get(createKey(MEMBERSHIPS_MODERATORS, requestUri));
            return new ModeratedItem(e);
        } catch (EntityNotFoundException e) {
            log.info(requestUri + ": not moderated");
        }

        return null;

    }

    public static ModeratedItem[] getModeratedItems() {
        return getModeratedUris(new Query(MEMBERSHIPS_MODERATORS));
    }

    public static ModeratedItem[] getModeratedItems(String moderator) {
        if (isEmpty(moderator)) {
            return new ModeratedItem[0];
        }

        return getModeratedUris(new Query(MEMBERSHIPS_MODERATORS).addFilter("moderator", EQUAL, moderator));
    }

    public static ModeratedUser[] getModeratedUsers(String moderatedUri) {
        Query q = new Query(MEMBERSHIPS_URI, createKey(MEMBERSHIPS_MODERATORS, moderatedUri));
        return getModeratedUsers(q);
    }

    public static void createMemberships(Membership m, String moderatedUri, String... users) {
        if (m == null || isEmpty(moderatedUri) || users == null || users.length == 0) {
            return;
        }

        List<Entity> memberships = new ArrayList<Entity>();

        Key moderator = createKey(MEMBERSHIPS_MODERATORS, moderatedUri);
        for (String user : users) {
            Entity e = new Entity(createKey(moderator, MEMBERSHIPS_URI, user));
            e.setProperty(MEMBERSHIP, m.name());
            memberships.add(e);
        }

        inTransaction(datastore, new PutEntityTransactionCallback(memberships));

    }

    public static void revokeMemberships(String moderatedUri, String... users) {
        if (isEmpty(moderatedUri) || users == null) {
            return;
        }

        List<String> usersList = asList(users);
        if (usersList.isEmpty()) {
            return;
        }

        List<Key> removekeys = new ArrayList<Key>();

        Query q = new Query(MEMBERSHIPS_URI, createKey(MEMBERSHIPS_MODERATORS, moderatedUri));
        for (Entity e : datastore.prepare(q).asIterable()) {
            Key k = e.getKey();
            if (usersList.contains(k.getName())) {
                removekeys.add(k);
            }
        }

        inTransaction(datastore, new DeleteEntityTransactionCallback(removekeys));

    }

    /*
     * Members Controls
     */

    public static ModeratedUser[] myMemberships(String email) {
        return getModeratedUsers(new Query(MEMBERSHIPS_URI).addFilter("name", EQUAL, email));
    }

    /*
     * Registry Controls
     */

    public static boolean isAllowedInDomain(DomainMember domainMember) {
        if (!isDomainMembershipActivated()) {
            return true;
        }

        Membership m = domainMember.getMembership();
        if (m.equals(guest) && isDomainOpenToEveryoneAsGuest()) {
            return true;
        }

        String user = domainMember.getUser();
        try {
            Entity e = datastore.get(createKey(MEMBERSHIPS_DOMAIN, user));
            return m.equals(fromEntity(e));
        } catch (EntityNotFoundException e) {
            log.info("user not found:" + user);
        }

        return false;
    }

    public static Membership domainMembership(String email) {
        try {
            Entity e = datastore.get(createKey(MEMBERSHIPS_DOMAIN, email));
            return fromEntity(e);
        } catch (EntityNotFoundException e) {
            log.info("user not found:" + email);
        }

        return applicant;
    }

    public static boolean isAllowedAs(ModeratedItem moderatedItem, DomainMember dm) {
        String user = dm.getUser();
        if (moderatedItem.isModerator(user)) {
            return true;
        }

        try {
            Key moderator = createKey(MEMBERSHIPS_MODERATORS, moderatedItem.getUri());
            Key moderatedUser = createKey(moderator, MEMBERSHIPS_URI, user);
            Membership membership = fromEntity(datastore.get(moderatedUser));
            return membership.equals(dm.getMembership()) || membership.equals(member);
        } catch (EntityNotFoundException e) {
            log.info("not allowed");
        }
        return false;
    }

    /*
    * Private Methods
    */

    private static ModeratedItem[] getModeratedUris(Query q) {
        ArrayList<ModeratedItem> uris = new ArrayList<ModeratedItem>();
        for (Entity entity : datastore.prepare(q).asIterable()) {
            uris.add(new ModeratedItem(entity));
        }
        return uris.toArray(new ModeratedItem[uris.size()]);
    }

    private static ModeratedUser[] getModeratedUsers(Query q) {
        List<ModeratedUser> moderatedUsers = new ArrayList<ModeratedUser>();
        for (Entity e : datastore.prepare(q).asIterable()) {
            moderatedUsers.add(new ModeratedUser(e));
        }
        return moderatedUsers.toArray(new ModeratedUser[moderatedUsers.size()]);
    }

    private static boolean isDomainOpenToEveryoneAsGuest() {
        try {
            if (datastore.get(createKey(MEMBERSHIPS_DOMAIN, everyone.name())) != null) {
                return true;
            }
        } catch (EntityNotFoundException e) {
            log.info("everyone as guest not found");
        }
        return false;
    }

    /*
    * Public Classes
    */

    public static enum Membership {
        member, guest, applicant, everyone;

        public static Membership fromEntity(Entity e) {
            if (e.hasProperty(MEMBERSHIP)) {
                return valueOf(e.getProperty(MEMBERSHIP).toString());
            }
            return null;
        }

        public static Membership fromJson(JSONObject json) throws JSONException {
            if (json.has(MEMBERSHIP)) {
                return valueOf(json.getString(MEMBERSHIP));
            }
            return null;
        }
    }

    public static class DomainMember implements Jsonable {
        private final String user;
        private final Membership membership;

        public DomainMember(Entity e) {
            this.user = e.getKey().getName();
            this.membership = fromEntity(e);
        }

        public DomainMember(String user, Membership membership) {
            this.user = user;
            this.membership = membership;
        }

        public DomainMember(JSONObject json) throws JSONException {
            this.user = json.getString("user");
            this.membership = fromJson(json);
        }

        public String getUser() {
            return user;
        }

        public Membership getMembership() {
            return membership;
        }

        public JSONObject toJSON() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("user", user);
            json.put(MEMBERSHIP, membership.name());
            return json;
        }

    }

    public static class ModeratedItem implements Jsonable {
        private final String uri;
        private final String moderator;

        public ModeratedItem(Entity e) {
            this.uri = e.getKey().getName();
            this.moderator = e.getProperty("moderator").toString();
        }

        public ModeratedItem(String uri, String moderator) {
            this.uri = uri;
            this.moderator = moderator;
        }

        public String getUri() {
            return uri;
        }

        public String getModerator() {
            return moderator;
        }

        public boolean isModerator(String user) {
            return equalsIgnoreCase(user, moderator);
        }

        public JSONObject toJSON() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("uri", "/addama/memberships/uris/" + uri);
            json.put("moderated", uri);
            json.put("moderator", moderator);
            return json;
        }
    }

    public static class ModeratedUser implements Jsonable {
        private final String moderatedUri;
        private final String user;
        private final Membership membership;

        public ModeratedUser(Entity e) {
            this.moderatedUri = e.getParent().getName();
            this.user = e.getKey().getName();
            this.membership = fromEntity(e);
        }

        public String getModeratedUri() {
            return moderatedUri;
        }

        public String getUser() {
            return user;
        }

        public Membership getMembership() {
            return membership;
        }

        public JSONObject toJSON() throws JSONException {
            JSONObject json = new JSONObject();
            json.put("uri", "/addama/memberships/" + moderatedUri);
            json.put("moderated", moderatedUri);
            json.put("user", user);
            json.put(MEMBERSHIP, membership.name());
            return json;
        }
    }
}
