package modularwotclaninfo;

import com.google.gson.stream.JsonReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.SwingWorker;

/**
 *
 * @author Yoyo117 (johnp)
 */
public class GetPlayerData extends SwingWorker<Player, Player> {

    private final long ID;
    private final GUI gui;

    protected GetPlayerData(long playerId, GUI gui)
    {
        this.ID = playerId;
        this.gui = gui;
    }

    @Override
    protected Player doInBackground() throws Exception
    {
        //URL URL = new URL("http://worldoftanks.eu/uc/accounts/"+ID+"/api/1.5/?source_token=Intellect_Soft-WoT_Mobile-unofficial_stats");
        URL URL = new URL("http://worldoftanks.eu/uc/accounts/"+ID+"/api/1.5/?source_token=Intellect_Soft-WoT_Mobile-unofficial_stats");
        URLConnection URLConnection = URL.openConnection();
        URLConnection.setRequestProperty("User-Agent", "Java like Android");
        URLConnection.setRequestProperty("Accept", "application/json, text/javascript, */*; q=0.01");
        URLConnection.setRequestProperty("Accept-Language", "en-us;q=0.5,en;q=0.3");
        URLConnection.setRequestProperty("Accept-Encoding", "paco");
        URLConnection.setRequestProperty("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
        URLConnection.setRequestProperty("Connection", "close");
        URLConnection.setRequestProperty("X-Requested-With", "XMLHttpRequest");
        // timeout after 20 seconds
        URLConnection.setConnectTimeout(20000);

        // TIMING
        //long start = System.currentTimeMillis();



        //StringBuilder data = new StringBuilder(22500); // TODO: optimize capacity
        //JsonParser jsonParser = new JsonParser();
        //JsonElement json;
        //try (BufferedReader reader = new BufferedReader(new InputStreamReader(URLConnection.getInputStream(), "UTF8"))) {
        try (JsonReader reader = new JsonReader(new InputStreamReader(URLConnection.getInputStream(), "UTF8"))) {
            // TODO: check performance and probably use everywhere || PERFORMES BETTER !
            //for (String line; (line = reader.readLine()) != null; data.append(line));
            //json = jsonParser.parse(data.toString());

            // "read while loading"
            /* // relevant Format
             obj-begin
             name "status" == "ok" -> continue
             name "data"
              obj-begin
              name "name"
              name "vehicles"
               array-begin
               for each obj extract info
               array-end
              name "updated_at"
              name "battles"
               obj-begin
               name "spotted"
               name "hits_percents"
               name "damage_dealt"
               obj-end
              name "summary"
               obj-begin
               name "wins"
               name "losses"
               name "battles_count"
               obj-end
              name "experience"
               obj-begin
               name "battle_avg_xp"
               obj-end
              name "clan"
               obj-begin
                name "member"
                 obj-begin
                 name "role"
                 obj-end
               obj-end
              obj-end
             obj-end
            */


            // initialize all empty
            String name=null, role=null;
            double last_updated=0D;
            int spotted=0, hitRatio=0, captured=0, dmgDealt=0, frags=0, defended=0;
            int wins=0, losses=0, battles=0, survived=0, avg_xp=0;
            ArrayList<Vehicle> vehicles = new ArrayList<>(100);

            // parse (performance ~= lower commented part :( )
            reader.beginObject();
            while (reader.hasNext()) {
                String name1 = reader.nextName();
                switch(name1) {
                    case "status":
                        if (!"ok".equalsIgnoreCase(reader.nextString())) {
                            while (reader.hasNext()) {
                                String namex = reader.nextName();
                                switch (namex) {
                                    case "status_code":
                                        throw new PlayerAPIException(reader.nextString(), this.gui);
                                }
                            }
                        }
                        break;
                    case "data":
                        reader.beginObject();
                        while (reader.hasNext()) {
                            String name2 = reader.nextName();
                            switch(name2) {
                                case "name": name = reader.nextString(); break;
                                case "vehicles":
                                    reader.beginArray();
                                    while (reader.hasNext()) {
                                        reader.beginObject();
                                        String vname = null, nation = null, vclass = null;
                                        int lvl = 0, vbattles = 0, vwins = 0;
                                        while (reader.hasNext()) {
                                            String name3 = reader.nextName();
                                            switch(name3) {
                                                case "localized_name":
                                                    vname = reader.nextString();
                                                    break;
                                                case "level":
                                                    lvl = reader.nextInt();
                                                    break;
                                                case "battle_count":
                                                    vbattles = reader.nextInt();
                                                    break;
                                                case "nation":
                                                    nation = reader.nextString().toUpperCase();
                                                    break;
                                                case "win_count":
                                                    vwins = reader.nextInt();
                                                    break;
                                                case "class":
                                                    vclass = reader.nextString();
                                                    break;
                                                default: reader.skipValue(); break;
                                            }
                                        }
                                        // UI optimizations
                                        switch (vclass) {
                                            case "heavyTank": vclass = "Heavy Tank"; break;
                                            case "mediumTank": vclass = "Medium Tank"; break;
                                            case "lightTank": vclass = "Light Tank"; break;
                                            case "AT-SPG": vclass = "Tank Destroyer"; break;
                                        }
                                        double wr = (double)vwins/vbattles;
                                        vehicles.add(new Vehicle(vname, nation, vclass, lvl, vbattles, wr));
                                        reader.endObject();
                                    }
                                    reader.endArray();
                                    break;
                                case "updated_at": last_updated = reader.nextDouble();
                                    break;
                                case "battles":
                                    reader.beginObject();
                                    while (reader.hasNext()) {
                                        String name4 = reader.nextName();
                                        switch (name4) {
                                            case "spotted": spotted = reader.nextInt();
                                                break;
                                            case "hits_percents": hitRatio = reader.nextInt();
                                                break;
                                            case "capture_points": captured = reader.nextInt();
                                                break;
                                            case "damage_dealt": dmgDealt = reader.nextInt();
                                                break;
                                            case "frags": frags = reader.nextInt();
                                                break;
                                            case "dropped_capture_points": defended = reader.nextInt();
                                                break;
                                            default: reader.skipValue();
                                        }
                                    }
                                    reader.endObject();
                                    break;
                                case "summary":
                                    reader.beginObject();
                                    while (reader.hasNext()) {
                                        String name5 = reader.nextName();
                                        switch (name5) {
                                            case "wins": wins = reader.nextInt();
                                                break;
                                            case "losses": losses = reader.nextInt();
                                                break;
                                            case "battles_count": battles = reader.nextInt();
                                                break;
                                            case "survived_battles": survived = reader.nextInt();
                                                break;
                                            default: reader.skipValue();
                                        }
                                    }
                                    reader.endObject();
                                    break;
                                case "experience":
                                    reader.beginObject();
                                    while (reader.hasNext()) {
                                        String name6 = reader.nextName();
                                        switch (name6) {
                                            case "battle_avg_xp": avg_xp = reader.nextInt();
                                                break;
                                            default: reader.skipValue();
                                        }
                                    }
                                    reader.endObject();
                                    break;
                                case "clan":
                                    reader.beginObject();
                                    while (reader.hasNext()) {
                                        String name7 = reader.nextName();
                                        switch (name7) {
                                            case "member":
                                                reader.beginObject();
                                                while (reader.hasNext()) {
                                                    String name8 = reader.nextName();
                                                    switch (name8) {
                                                        case "role": role = reader.nextString();
                                                            break;
                                                        default: reader.skipValue();
                                                    }
                                                }
                                                reader.endObject();
                                                break;
                                            default: reader.skipValue();
                                        }
                                    }
                                    reader.endObject();
                                    break;
                                default: reader.skipValue();
                            }
                        }
                        reader.endObject();
                        break;
                    default: reader.skipValue();
                }
            }
            reader.endObject();
            reader.close();


/*

            JsonObject json_data = json.getAsJsonObject().get("data").getAsJsonObject();

            String name = json_data.get("name").getAsString();
            String role = json_data.get("clan").getAsJsonObject().get("member").getAsJsonObject().get("role").getAsString();

            JsonObject json_summary = json_data.get("summary").getAsJsonObject();

            int battles = json_summary.get("battles_count").getAsInt();
            int wins = json_summary.get("wins").getAsInt();
            int losses = json_summary.get("losses").getAsInt();

            JsonObject json_battles = json_data.get("battles").getAsJsonObject();

            int hitRatio = json_battles.get("hits_percents").getAsInt();
            int dmgDealt = json_battles.get("damage_dealt").getAsInt();
            int frags = json_battles.get("frags").getAsInt();
            int spotted = json_battles.get("spotted").getAsInt();
            int defended = json_battles.get("dropped_capture_points").getAsInt();
            int captured = json_battles.get("capture_points").getAsInt();

            JsonObject json_exp = json_data.get("experience").getAsJsonObject();

            int avg_xp = json_exp.get("battle_avg_xp").getAsInt();

            JsonArray json_vehicles = json_data.get("vehicles").getAsJsonArray();

            ArrayList<Vehicle> vehicles = new ArrayList<>(json_vehicles.size());
            for (JsonElement ele : json_vehicles) {
                JsonObject o = ele.getAsJsonObject();
                String vname = o.get("localized_name").getAsString();
                String nation = o.get("nation").getAsString().toUpperCase(); // UI opt
                String vclass = o.get("class").getAsString();

                // UI optimizations
                switch (vclass) {
                    case "heavyTank": vclass = "Heavy Tank"; break;
                    case "mediumTank": vclass = "Medium Tank"; break;
                    case "lightTank": vclass = "Light Tank"; break;
                    case "AT-SPG": vclass = "Tank Destroyer"; break;
                    default: break;
                }

                int lvl = o.get("level").getAsInt();
                int vbattles = o.get("battle_count").getAsInt();
                int vwins = o.get("win_count").getAsInt();
                double wr = (double)vwins/vbattles;

                vehicles.add(new Vehicle(vname, nation, vclass, lvl, vbattles, wr));
            }

            double last_updated = json_data.get("updated_at").getAsDouble();

*/
            //System.out.println(System.currentTimeMillis() - start);


            vehicles.trimToSize();

            double avg_dmg = (double)dmgDealt/battles;
            double avg_wr = (double)wins/battles;
            double avg_lr = (double)losses/battles;
            double avg_srv = (double)survived/battles;

            double avg_tier = 0;
            for (Vehicle v : vehicles) {
                avg_tier += v.getTier()*v.getBattles();
            }
            avg_tier /= (double)battles;

            double avg_frags = (double)frags/battles;
            double avg_spotted = (double)spotted/battles;
            double avg_defended = (double)defended/battles;
            double avg_captured = (double)captured/battles;

            double eff = avg_frags*(350.0D-avg_tier*20.0D) + avg_dmg*(0.2D + 1.5D/avg_tier)
                        + 200.0D*avg_spotted + 150.0D*avg_defended + 150.0D*avg_captured;

            vehicles = Utils.sortVehiclesByTier(vehicles);
            int maxTier = vehicles.get(0).getTier();

            Icon roleIcon;
            switch(role) {
                case "Soldier":
                    roleIcon = new ImageIcon(getClass().getResource("/img/soldier.png"), "Soldier");
                    break;
                case "Recruit":
                    roleIcon = new ImageIcon(getClass().getResource("/img/recruit.png"), "Recruit");
                    break;
                case "Field Commander":
                    roleIcon = new ImageIcon(getClass().getResource("/img/fieldcommander.png"), "Field Commander");
                    break;
                case "Commander":
                    roleIcon = new ImageIcon(getClass().getResource("/img/commander.png"), "Commander");
                    break;
                case "Deputy Commander":
                    roleIcon = new ImageIcon(getClass().getResource("/img/deputycommander.png"), "Deputy Commander");
                    break;
                case "Recruiter":
                    roleIcon = new ImageIcon(getClass().getResource("/img/recruiter.png"), "Recruiter");
                    break;
                case "Diplomat":
                    roleIcon = new ImageIcon(getClass().getResource("/img/diplomat.png"), "Recruiter");
                    break;
                case "Treasurer":
                    roleIcon = new ImageIcon(getClass().getResource("/img/treasurer.png"), "Treasurer");
                    break;
                default:
                    System.err.println("Unknown clanRole: "+role);
                    roleIcon = null;
            }

            return new Player(this.ID, last_updated, name, role, roleIcon, maxTier,
                    battles, hitRatio, avg_dmg, avg_wr, avg_lr, avg_srv, avg_xp, eff, vehicles);
        }
    }
}