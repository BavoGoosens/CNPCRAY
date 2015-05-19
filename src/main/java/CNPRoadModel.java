import com.github.rinde.rinsim.core.model.road.GraphRoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.geom.ConnectionData;
import com.github.rinde.rinsim.geom.Graph;
import com.github.rinde.rinsim.geom.Point;

import java.util.List;
import java.util.Map;
import java.util.Set;

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


}
