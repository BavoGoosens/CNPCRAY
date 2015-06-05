import com.sun.org.glassfish.external.statistics.Statistic;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

/**
 * Created by bavo en michiel.
 */
public class StatisticsHub {

    private HashMap<String, HashMap<String, Object>> data = new HashMap<>();

    public StatisticsHub(){

    }

    public synchronized void dataUpdate(String agentName, String dataClass,  String dataType, Object entry){
        if (this.data.containsKey(agentName)){

        } else {
            //data.put(agentName, new HashMap<String, Object>(dataClass,  );
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
