
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by bavo en michiel.
 */
public class StatisticsHub {

    private HashMap<String, ArrayList<String>> data = new HashMap<>();

    public StatisticsHub(){

    }

    public synchronized void dataUpdate(String agentName, String dataClass,  String dataType, Object entry){
        String dataEntry = dataClass + " , " + dataType + " , " + entry + " ;";
        if (this.data.containsKey(agentName)){
            ArrayList entriesForAgent = this.data.get(agentName);
            entriesForAgent.add(dataEntry);
        } else {
            ArrayList agentList = new ArrayList();
            agentList.add(dataEntry);
            data.put(agentName, agentList);
        }
    }

    public void exportRoadUsersToCSV(){

    }

    public void exportDataTypeToCSV(){

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
