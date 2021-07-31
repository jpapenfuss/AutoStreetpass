import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
autostreetpass - On wireless network interface adapters that change allow MAC address changes, cycle through MAC addresses documented to be AT&T/McDonalds WiFi hotspots and other Nintendo "StreetPass". This allows users with Nintendo 3DS systems to "visit" StreetPass sites and gain various benefits and earn achievements that would typically require people to meet physically at a supporting location. 
*/

public class AutoStreetpass {
    // Doesn't really matter.
    private final String USER_AGENT = "Java 1.8";
    // URL for JSON version of Google Docs sheet
    private final String sheetURL = "https://spreadsheets.google.com/feeds/list/1OfgyryUHeCPth76ziFT985XNLS-O5EXtjQDa0kA1L6M/2/public/values?alt=json";
    // Should match MAC address format - case sensitive
    private final String macPattern = "[0-9A-Z][0-9A-Z]:[0-9A-F][0-9A-F]:[0-9A-F][0-9A-F]:[0-9A-F][0-9A-F]:[0-9A-F][0-9A-F]:[0-9A-F][0-9A-F]";
    // Init variables
    String currentMAC;
    // This will be an array of MAC addresses to use.
    List < String > allMatches = new ArrayList < String > ();
    // This iterator is used for the list of MAC addresses collected.
    Iterator iterator;
    int sleepDuration = 60000;
    String hostapdConfig1 = "interface=wlan0\nbridge=br0\ndriver=nl80211\nssid=attwifi\nbssid=";
    String hostapdConfig2 = "\nhw_mode=g\nchannel=6\nauth_algs=1\nwpa=0\nmacaddr_acl=1\naccept_mac_file=/etc/hostapd/mac_accept\nwmm_enabled=0\nignore_broadcast_ssid=0\n";
    String hostapdConfigFile = "/etc/hostapd/hostapd.conf";
    /*
     * 		main method start
     * */
    public static void main(String args[]) throws Exception {
        String currentMAC;
        String newMAC;
        pipass http = new pipass();

        http.doGet();
        http.createIterator();
        http.doLoop();
    }
    /*
     * 		main method end
     */

    private void doLoop() {
        while (true) {
            String nextMAC = (String) getNext();
            try {
                File f = new File(hostapdConfigFile);
                f.delete();
            } catch (Exception e) {
                System.out.println("Error encountered deleting file " + hostapdConfigFile + " - file may not exist, or we may not have permission.");
                System.out.println("Caught exception: " + e);
                System.exit(1);
            }

            try {
                PrintWriter hostapdConfig = new PrintWriter(hostapdConfigFile);
                System.out.println("Configuring hostapd with MAC " + nextMAC);
                hostapdConfig.println(hostapdConfig1 + nextMAC + hostapdConfig2);
                hostapdConfig.close();
            } catch (Exception e) {
                System.out.println("Error encountered creating and writing new " + hostapdConfigFile + " - we may not have permission.");
                System.out.println("Caught exception: " + e);
                System.exit(1);

            }
            try {
                Process killhostapd = new ProcessBuilder("killall", "hostapd").start();
                Thread.sleep(1000);
            } catch (Exception e) {
                System.out.println("Failed to kill hostapd.");
                System.out.println("Caught exception: " + e);
                System.exit(1);
            }
            try {
                Process starthostapd = new ProcessBuilder("hostapd", "/etc/hostapd/hostapd.conf").start();
                System.out.println("Started hostapd.");
            } catch (Exception e) {
                System.out.println("There was an error writing the config file, killing hostapd, or starting hostapd.");
                System.out.println("Caught exception: " + e);
                System.exit(1);
            }
            try {
                Thread.sleep(sleepDuration);
            } catch (Exception e) {
                System.out.println("Failed to sleep thread.");
                System.out.println("Caught exception: " + e);
                System.exit(1);
            }
        }

    }

    /*
     * 		doGet start
     */
    private void doGet() throws Exception {
        URL obj = new URL(sheetURL);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", USER_AGENT);
        int responseCode = con.getResponseCode();
        System.out.println(responseCode);

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();
        while ((inputLine = in .readLine()) != null) {
            response.append(inputLine);
        } in .close();
        Pattern p = Pattern.compile(macPattern);
        Matcher m = p.matcher(response.toString());
        while (m.find()) {
            allMatches.add(m.group());
        }
    }
    /*
     * 		doGet end
     */

    /*
     * 		createIterator start
     */
    private void createIterator() {
        // Java didn't like this being in main, so here it is instead.
        iterator = allMatches.iterator();
    }
    /*
     * 		createIterator end
     */

    private String getNext() {
        String nextMAC = "";
        if (iterator.hasNext()) {
            iterator.next();
        } else {
            createIterator();
            iterator.next();
        }
        nextMAC = (String) iterator.next(); // Burns a duplicate MAC
        System.out.println(nextMAC);
        return nextMAC;
    }
}
