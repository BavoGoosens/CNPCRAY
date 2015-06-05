
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by bavo en michiel.
 */
public class StatisticsHub {

    private String filename;

    private HashMap<String, ArrayList<String>> data = new HashMap<>();

    public StatisticsHub(String experiment){
        this.filename = experiment;
    }

    public synchronized void dataUpdate(String agentName, String dataClass,  String dataType, Object entry){
        String dataEntry = dataClass + " , " + dataType + " , " + entry + " ;";
        if (this.data.containsKey(agentName)){
            ArrayList entriesForAgent = this.data.get(agentName);
            entriesForAgent.add(dataEntry);
        } else {
            if (agentName.contains("Task"))
                System.out.println("  ");
            ArrayList agentList = new ArrayList();
            agentList.add(dataEntry);
            data.put(agentName, agentList);
        }
    }

    public void exportRoadUsersToCSV(){
        try{
            for (String agent : this.data.keySet()){
                ArrayList<String> agentData = this.data.get(agent);
                FileWriter writer = new FileWriter(this.filename + "____" + agent);
                for (String entry: agentData){
                    writer.append(entry);
                    writer.append("\n");
                }
            }
        } catch (IOException e){
            System.out.println("duhduhdu");
        }
    }

    //hoe lang tot allocation van tasks

    //public synchronized void taskStationUpdate(String taskStationName, str);

    // Which roaduser sent what  (broadcast count)
    //communicationUpdate;

    // alles naar mooie csv's schrijven
    //exportAllData;

    //creation time, allocation time, delivery time, # verschillende taskmanager updates,
    //taskUpdate;






}
