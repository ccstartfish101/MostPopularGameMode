package mostpopularws;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.google.appengine.api.urlfetch.URLFetchServiceFactory;

import com.google.appengine.api.urlfetch.HTTPHeader;
import com.google.appengine.api.urlfetch.HTTPMethod;
import com.google.appengine.api.urlfetch.HTTPRequest;
import com.google.appengine.api.urlfetch.HTTPResponse;

import javax.servlet.http.*;

@SuppressWarnings("serial")
public class MostPopularWSServlet extends HttpServlet {
    private static final Logger    logger     = Logger.getLogger(MostPopularWSServlet.class.getName());
    private static final String    USER_AGENT = "GAE/" + MostPopularWSServlet.class.getName();
    private static final String IPINFO_URL = "https://ipinfo.io/";
    // thread safe set for storing all the game modes
    private static final Set<Key> gameModeList = Collections.synchronizedSet( new HashSet<Key>());
    private static MemcacheService memcache;
    private static MemcacheService rev_memcache;
    private static URLFetchService urlfetch;
    private static DatastoreService datastore; 
    private static final Map<String, String> ipcache = new ConcurrentHashMap<String, String>();

    static {
            // transactional storage for keeping the count per country per game mode
            datastore = DatastoreServiceFactory.getDatastoreService();
            // caching from property value to Key
            memcache = MemcacheServiceFactory.getMemcacheService();
            // caching from key to property value 
            rev_memcache = MemcacheServiceFactory.getMemcacheService();
        }

    public void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        resp.setContentType("application/json");
        String countryCode;
        /*
        //Please uncomment this line and comment the line below if you are testing using real IP addresses 
      	countryCode = getClientCountry(req);
        */
        /* 
         * This is the mode used in SimpleTestClient
         * The country code is provided by the cookie from the request
        */
        countryCode = getMockClientCountry(req);
        
        resp.addHeader("Country", countryCode);
        
        //trusted address
        resp.addHeader("ip", req.getRemoteAddr());
        String gameMode = getGameMode(req);
        
        if (gameMode == null || countryCode == null){
            resp.getWriter().println("No gameMode or country code specified");
            return;
        }
        Entity entity = null;
        Key key = (Key) memcache.get(countryCode + gameMode);
        if (key != null) {
            try {
                entity = datastore.get(key);
            } catch (EntityNotFoundException e1) {
                e1.printStackTrace();
            }
            entity.setProperty("count", (long)entity.getProperty("count") + (long)1);
            // updating entity in the datastore
            datastore.put(entity);
        }
        else {
                // key {countryCode}_{gameMode}
                Entity parentEntity = new Entity("gameMode", gameMode);
                entity = new Entity("country", countryCode, parentEntity.getKey());
                Key gameModeKey = parentEntity.getKey();
                Key countryKey = entity.getKey();
                memcache.put(countryCode + gameMode, countryKey);
                memcache.put(gameMode, gameModeKey);
                gameModeList.add(countryKey);
                rev_memcache.put(gameModeKey, gameMode);
                entity.setProperty("count", (long)1);
                datastore.put(entity);
            }
        
        // Order in descending by count:
        Filter countryFilter = new Query.FilterPredicate(Entity.KEY_RESERVED_PROPERTY,
                                                    Query.FilterOperator.IN,
                                                    gameModeList);
        Query q = new Query("country").setFilter(countryFilter).addSort("count", SortDirection.DESCENDING);
        // Use PreparedQuery interface to retrieve results
        PreparedQuery pq = datastore.prepare(q);
        List<Entity> results =pq.asList(FetchOptions.Builder.withDefaults());
        //get the first one to put back into the response code
        if (!results.isEmpty()){
            // will be in {countryCode}_{gameMode} format
            String maxKey = (String)rev_memcache.get(results.get(0).getParent());
            resp.addHeader("MostPopular", maxKey);
            resp.getWriter().println("MostPopular : " + maxKey);
        } 
    }
    private static String getCookieDataField(HttpServletRequest request, String fieldName){
        Cookie[] cookies = request.getCookies();
        //if cookies are null, then return null
        if(null == cookies) {
            return null;
        }         
        for(int i=0; i < cookies.length; i++) {
            //if the cookie's name is "GameMode"
            if(fieldName.equals(cookies[i].getName())) {
                //get its value and return it
                String val = cookies[i].getValue();
                return val;
            }
        }
        return null;
    }
    public String getGameMode(HttpServletRequest request){
        return getCookieDataField(request, "GameMode");
    }
    public static String getClientCountry(HttpServletRequest request) {
        try {
            return getCountry(request.getRemoteAddr());
        } catch(Throwable t){
            t.printStackTrace(System.out);
            return "Error";
        }
    }
    public static String fetchCountry(String ip) {
        try {
            if (urlfetch == null) {
                urlfetch = URLFetchServiceFactory.getURLFetchService();
            }

            URL url = new URL(IPINFO_URL + ip + "/country");
            HTTPRequest request = new HTTPRequest(url, HTTPMethod.GET);
            request.addHeader(new HTTPHeader("User-Agent", USER_AGENT));

            HTTPResponse response = urlfetch.fetch(request);
            if (response.getResponseCode() != 200) {
                return null;
            }

            String country = new String(response.getContent(), "UTF-8");
            return country;
        } catch (Throwable t) {
            logger.log(Level.WARNING, "Unable to fetch GeoIp for " + ip, t);
            return null;
        }
    }
    public static String getCountry(String ip) {
        if (ip == null) {
            return "localhost";
        }

        String country = (String) ipcache.get(ip);

        if (country == null) {
            // Perform lookup at geoip.wtanaka.com
            country = fetchCountry(ip);

            if (country != null) {
                // Store lookup in local ipcache.
                ipcache.put(ip, country);
            }
        }

        return country;
    }
    
    public static String getMockClientCountry(HttpServletRequest request){
        return getCookieDataField(request, "Country");
        
    }
}
