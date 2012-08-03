package modularwotclaninfo;

import com.google.gson.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import javax.swing.ImageIcon;
import javax.swing.SwingWorker;

/**
 *
 * @author Yoyo117 (johnp)
 */
public class GetClanData extends SwingWorker<Clan, Clan> {

    private final GUI gui;

    private final String fallbackInput;
    private final String searchType;
    private PossibleClan bestMatch;

    private ArrayList<Player> players;

    protected GetClanData(PossibleClan bestMatch, String fallbackInput, String searchType, GUI gui)
    {
        this.bestMatch = bestMatch;
        this.gui = gui;
        this.fallbackInput = fallbackInput;
        this.searchType = searchType;
    }

    @Override
    protected Clan doInBackground() throws Exception
    {
        if (bestMatch == null) { // use fallBack
            //URL URL = new URL("http://worldoftanks.eu/community/clans/?type=table&offset=0&limit=10&order_by="+this.searchType+"&search="+this.clanTagName+"&echo=2&id=clans_index");
            // TODO: does API support orderBy ?!?
            URL URL = new URL("http://worldoftanks.eu/community/clans/api/1.1/?source_token=Intellect_Soft-WoT_Mobile-unofficial_stats&search="+this.fallbackInput.replace(" ", "%20")+"&offset=0&limit=10");
            URLConnection URLConnection = URL.openConnection();
            URLConnection.setRequestProperty("Accept", "application/json, text/javascript, */*; q=0.01");
            URLConnection.setRequestProperty("Accept-Language", "en-us;q=0.5,en;q=0.3");
            URLConnection.setRequestProperty("Accept-Encoding", "paco");
            URLConnection.setRequestProperty("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
            URLConnection.setRequestProperty("Connection", "close");
            URLConnection.setRequestProperty("X-Requested-With", "XMLHttpRequest");
            // timeout after 10 seconds
            URLConnection.setConnectTimeout(10000);

            StringBuilder data = new StringBuilder(3000);
            try (BufferedReader clanBufferedReader = new BufferedReader(new InputStreamReader(URLConnection.getInputStream(), "UTF8"))) {
                for (String line; (line = clanBufferedReader.readLine()) != null; data.append(line));
            }

            JsonParser jsonParser = new JsonParser();
            JsonObject json = jsonParser.parse(data.toString()).getAsJsonObject();

            if (!"ok".equalsIgnoreCase(json.get("status").getAsString())) {
                throw new ClanAPIException(json.get("status_code").getAsString(), this.gui);
            }

            JsonArray results = json.get("data").getAsJsonObject().get("items").getAsJsonArray();
            JsonObject json_clan = null;
            for (JsonElement e : results) {
                JsonObject o = e.getAsJsonObject();
                if (fallbackInput.equalsIgnoreCase(o.get(this.searchType).getAsString())) {
                    json_clan = o;
                    break;
                }
            }
            if (json_clan == null) {
                throw new ClanNotFoundException(this.searchType, this.gui);
            }
            String name = json_clan.get("name").getAsString();
            String tag = json_clan.get("abbreviation").getAsString();
            long ID = json_clan.get("id").getAsLong();
            int member_count = json_clan.get("member_count").getAsInt();
            ImageIcon emblem = new ImageIcon(new URL("http://worldoftanks.eu"+json_clan.get("clan_emblem_url").getAsString()));
            bestMatch = new PossibleClan(name, tag, ID, member_count, emblem);
        }

        // get members
        //URL = new URL("http://worldoftanks.eu/community/clans/"+clanID+"/members/?type=table&offset=0&limit=100&order_by=name&search=&echo=1&id=clan_members_index");
        URL URL = new URL("http://worldoftanks.eu/community/clans/"+bestMatch.getID()+"/api/1.1/?source_token=Intellect_Soft-WoT_Mobile-unofficial_stats");
        URLConnection URLConnection = URL.openConnection();
        URLConnection.setRequestProperty("Accept", "application/json, text/javascript, */*; q=0.01");
        URLConnection.setRequestProperty("Accept-Language", "en-us;q=0.5,en;q=0.3");
        URLConnection.setRequestProperty("Accept-Encoding", "paco");
        URLConnection.setRequestProperty("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
        URLConnection.setRequestProperty("Connection", "close");
        URLConnection.setRequestProperty("X-Requested-With", "XMLHttpRequest");
        // timeout after 15 seconds
        URLConnection.setConnectTimeout(15000);

        StringBuilder members_data = new StringBuilder(10000);
        try (BufferedReader membersBufferedReader = new BufferedReader(new InputStreamReader(URLConnection.getInputStream(), "UTF8"))) {
            for (String line; (line = membersBufferedReader.readLine()) != null; members_data.append(line));
        }

        JsonParser jsonParser = new JsonParser();
        JsonObject members_json = jsonParser.parse(members_data.toString()).getAsJsonObject();

        if (!"ok".equalsIgnoreCase(members_json.get("status").getAsString())) {
            throw new ClanAPIException(members_json.get("status_code").getAsString(), this.gui);
        }

        JsonArray members = members_json.get("data").getAsJsonObject().get("members").getAsJsonArray();

        GetPlayerData[] workers = new GetPlayerData[members.size()];
        players = new ArrayList<>(members.size());
        for (int i = 0; i < members.size(); i++) {
            JsonObject member = members.get(i).getAsJsonObject();
            workers[i] = new GetPlayerData(member.get("account_id").getAsLong(), this.gui);
            workers[i].execute();
        }
/*
        // do this while subthreads are running
        String tag = json_clan.get("abbreviation").getAsString();
        String name = json_clan.get("name").getAsString();
        int member_count = json_clan.get("member_count").getAsInt();
        ImageIcon emblem = new ImageIcon(new URL("http://worldoftanks.eu"+json_clan.get("clan_emblem_url").getAsString()));
        // early-return clan info
        this.gui.publishClanInfo(tag, name, member_count, emblem);
*/

        ArrayList<Vehicle> vehicles = new ArrayList<>(4_000);
        long start = System.currentTimeMillis();
        for (GetPlayerData w : workers) {
            try {
                Player p = w.get();
                players.add(p);
                vehicles.addAll(p.getVehicles());
                // TODO: update progress bar
                // TODO: sort by tier here ?!? (implementation details)
            } catch (ExecutionException e) {
                Utils.handleExecutionException(e, this.gui);
            } catch (InterruptedException e) { /* shouldn't happen */}
        }
        System.out.printf("Overall time: %dms%n", System.currentTimeMillis()-start);
        vehicles = Utils.sortVehiclesByTier(vehicles);
        vehicles = Utils.sortVehiclesByClass(vehicles);
        vehicles = Utils.sortVehiclesByNation(vehicles);

        players = Utils.sortPlayersByEfficiency(players);
        double avg_top_eff = 0D, avg_eff = 0D;
        for (int i = 0; i < players.size(); i++) {
            if (i == 20) avg_top_eff = avg_eff/20;
            avg_eff += players.get(i).getEfficiency();
        }
        avg_eff /= players.size();

        players = Utils.sortPlayersByWinrate(players);
        double avg_top_wr = 0D, avg_wr = 0D;
        for (int i = 0; i < players.size(); i++) {
            if (i == 20) avg_top_wr = avg_wr/20;
            avg_wr += players.get(i).getAvg_wr();
        }
        avg_wr /= players.size();

        return new Clan(bestMatch.getName(), bestMatch.getClanTag(), bestMatch.getID(),
                players, vehicles, avg_wr, avg_top_wr, avg_eff, avg_top_eff, bestMatch.getEmblem());
    }

    @Override // Executed in EDT !!
    protected void done(){
        try {
            Clan clan = get();
            this.gui.publishClanPlayers(clan);
        } catch (ExecutionException e) {
            Utils.handleExecutionException(e, this.gui);
        } catch (InterruptedException e) { e.printStackTrace(); gui.inputReset(); }
    }


}
