import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;

public class ScannerUser {

    long chat_ID;

    //settings by default
    int userPrice1 = 150; int userPrice2 = 250;
    int userArea = 25;
    int userPeriod = 5;

    Timer timer;
    //marker to avoid timer run error and stop if settings has been changed
    boolean timerOn;

    JSONArray checkedAdverts = new JSONArray();

    public ScannerUser(long chat_ID){
        this.chat_ID = chat_ID;

    }

    class ScanTask extends TimerTask {
        public void run() {
            timerOn = true;
            runner();
        }
    }

    public boolean timerStart (){
        if (!timerOn) {
            timer = new Timer();
            timer.schedule(new ScanTask(), 0, userPeriod * 60000L);
            return true;
        }
        return false;
    }

    public boolean timerStop (){
        if (timerOn) {
            timer.cancel();
            timerOn = false;
            return true;
        }
        return false;
    }

    public int getUserPeriod() {
        return userPeriod;
    }
    public boolean setUserPeriod(int period) {
        this.userPeriod = period;
        if (timerOn) {
            timer.cancel();
            timerOn = false;
            return true;
        }
        return false;

    }

    public int getUserPrice1() {
        return userPrice1;
    }

    public boolean setUserPrice1(int price1) {
        this.userPrice1 = price1;
        if (timerOn) {
            timer.cancel();
            timerOn = false;
            return true;
        }
        return false;
    }

    public int getUserPrice2() {
        return userPrice2;
    }

    public void setUserPrice2(int userPrice2) {
        this.userPrice2 = userPrice2;
    }

    public int getUserArea() {
        return userArea;
    }
    public boolean setUserArea(int area) {
        this.userArea = area;
        if (timerOn) {
            timer.cancel();
            timerOn = false;
            return true;
        }
        return false;
    }

    public void runner(){

        JSONArray newLinkArray = findLinks();
        JSONArray freshLinkArray = compareFiles(newLinkArray);
        sendAdvertsToUser(freshLinkArray);
        if (checkedAdverts.size() > 20) cleanOldAdverts();
    }

    public void cleanOldAdverts (){
        int c = checkedAdverts.size() - 20;
        for (int i = 0; i < c; i++) checkedAdverts.remove(0);
    }

    /**
     * Optional method to create an image of static map created in Google Maps Static API with apartment address marker on it to send it to user in message
     * @param address of apartment in advert
     * @return URL of static map image
     */
    public String getImageURL (String address) {

        String S = "&";
        String key = "key=YOUR-GOOGLE-API-KEY";
        String size = "size=512x512";
        String mapID = "map_id=MAP-STYLE-ID";
        String visible = "visible=56.949553,24.105006";
        String apartmentMarker = "markers=color:red%7Clabel:X%7C" + address;
        String mapImageURL = "https://maps.googleapis.com/maps/api/staticmap?" + size + S + key + S + mapID + S + visible + S + apartmentMarker;

        return mapImageURL;
    }

    public void sendAdvertsToUser (JSONArray advertsList) {

        String textSample = "%4$s-комнатная квартира за %2$s евро в месяц\n%3$s кв. м.\n\n%1$s";
        MyBot bot = new MyBot();

        for (int i = 0; i < advertsList.size(); i++) {
            JSONObject advertObject = (JSONObject) advertsList.get(i);
            JSONObject obj = (JSONObject) advertObject.get("Advert");
            String address = (String) obj.get("Address");

            String imageURL = getImageURL(address);

            String message = String.format(textSample, (String) obj.get("Link"), (String) obj.get("Price"), (String) obj.get("Area"), (String) obj.get("Rooms"));

            SendPhoto sendPhoto = new SendPhoto().setChatId(chat_ID).setPhoto(imageURL).setCaption(message);

            try {
                bot.execute(sendPhoto);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Method to compare adverts from current search with previously checked,
     * exclude checked from found ads,
     * include fresh ads in list as checked
     * @param advertsToCompare from current search
     * @return fresh ads
     */
    public JSONArray compareFiles (JSONArray advertsToCompare) {

        boolean contains = checkedAdverts.containsAll(advertsToCompare);
        advertsToCompare.removeAll(checkedAdverts);

        if (!contains) checkedAdverts.addAll(advertsToCompare);

        return advertsToCompare;

    }

    /**
     * @return list of found adverts with set parameters
     */
    public JSONArray findLinks() {
        JSONArray jsonArray = new JSONArray();

        try {
            String url = "https://www.ss.com/lv/real-estate/flats/riga/today/";

            Document page = (Document) Jsoup.parse(new URL(url), 3000);
            Elements lines = page.select("tr[id~=^tr_\\d{8}$]");

            for (Element line : lines) {

                String rawPrice = line.select("td:nth-child(10)").text();

                if (!rawPrice.contains("€/mēn.")) continue;

                rawPrice = rawPrice.substring(0,3);

                boolean correctPrice = Pattern.compile("^\\d{3}$").matcher(rawPrice).matches();

                if (!correctPrice) continue;

                String rawArea = line.select("td:nth-child(6)").text();
                String rooms = line.select("td:nth-child(5)").text();


                int intPrice = Integer.parseInt(rawPrice);
                int intArea = Integer.parseInt(rawArea);

                if (intPrice >= userPrice1 && intPrice <= userPrice2 && intArea >= userArea) {
                    String link = line.select("a").first().attr("abs:href");
                    String address = line.select("td:nth-child(4)").text().replaceAll(" ", "+").replace("g.", "gatve");

                    JSONObject data = new JSONObject();
                    JSONObject advert = new JSONObject();

                    data.put("Link", link);
                    data.put("Price", rawPrice);
                    data.put("Area", rawArea);
                    data.put("Rooms", rooms);
                    data.put("Address", address);
                    advert.put("Advert", data);
                    jsonArray.add(advert);
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return jsonArray;
    }
}

