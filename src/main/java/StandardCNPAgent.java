import com.github.rinde.rinsim.core.TimeLapse;
import com.github.rinde.rinsim.core.model.comm.CommDevice;
import com.github.rinde.rinsim.core.model.comm.CommDeviceBuilder;
import com.github.rinde.rinsim.core.model.comm.CommUser;
import com.github.rinde.rinsim.core.model.comm.Message;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.Point;
import com.google.common.base.Optional;
import org.apache.commons.math3.random.RandomGenerator;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

/**
 * Created by bavo and michiel
 */
public class StandardCNPAgent extends CNPAgent {

    private final static int workersNeeded = 1;

    StandardCNPAgent(String name, RandomGenerator r) {
        this(name, r, fullEnergy);
    }

    StandardCNPAgent(String name, RandomGenerator r, long energy) {
        super(name, r, energy);
    }

    @Override
    protected int workersNeeded() {
        return StandardCNPAgent.workersNeeded;
    }



}