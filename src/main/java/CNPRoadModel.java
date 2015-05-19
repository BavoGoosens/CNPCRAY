import com.github.rinde.rinsim.core.model.road.GraphRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.geom.ConnectionData;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.Point;

import java.util.*;

/**
 * Created by parallels on 5/19/15.
 */
public class CNPRoadModel extends GraphRoadModel {

    public CNPRoadModel(Graph<? extends ConnectionData> pGraph) {
        super(pGraph);
    }

    public BatteryStation getNearestBatteryStation(Point location) {
        int bestPathSize = Integer.MAX_VALUE;
        BatteryStation bestBatteryStation = null;
        for (RoadUser roadUser: this.getObjectsAndPositions().keySet()) {
            if (roadUser instanceof BatteryStation) {
                BatteryStation batteryStation = (BatteryStation) roadUser;
                List<Point> path = this.getShortestPathTo(location, batteryStation.getPosition().get());
                if (path.size() < bestPathSize) {
                    bestBatteryStation = batteryStation;
                    bestPathSize = path.size();
                }
            }
        }
        return bestBatteryStation;
    }

    public Collection<CNPAgent> getAgents() {
        Collection<CNPAgent> agents = new ArrayList<CNPAgent>();
        for (RoadUser roadUser: this.getObjects()) {
            if (roadUser instanceof CNPAgent) {
                CNPAgent cnpAgent = (CNPAgent) roadUser;
                agents.add(cnpAgent);
            }
        }
        return agents;
    }

    public Collection<BatteryStation> getBatteryStations() {
        Collection<BatteryStation> stations = new ArrayList<BatteryStation>();
        for (RoadUser roadUser: this.getObjects()) {
            if (roadUser instanceof BatteryStation) {
                BatteryStation station = (BatteryStation) roadUser;
                stations.add(station);
            }
        }
        return stations;
    }


}
