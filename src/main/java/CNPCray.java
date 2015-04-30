import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.comm.CommModel;
import com.github.rinde.rinsim.core.model.pdp.DefaultPDPModel;
import com.github.rinde.rinsim.core.model.road.GraphRoadModel;
import com.github.rinde.rinsim.geom.*;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.AGVRenderer;
import com.github.rinde.rinsim.ui.renderers.CommRenderer;
import com.github.rinde.rinsim.ui.renderers.GraphRoadModelRenderer;
import com.github.rinde.rinsim.ui.renderers.RoadUserRenderer;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import org.eclipse.swt.graphics.RGB;

import java.util.Map;
import java.util.ArrayList;
import java.util.Random;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Created by bavo en michiel.
 */
public class CNPCray {
    private static final double VEHICLE_LENGTH = 2d;

    /**
     * @param args - No args.
     */
    public static void main(String[] args) {
        int graphSize = 30;
        int numberOfEmptyConnections = 15;
        int numberOfAgents = 50;
        final DefaultPDPModel pdpModel = DefaultPDPModel.create();
        final CommModel commModel = CommModel.builder().build();
        final Simulator sim = Simulator.builder()
                .addModel(new GraphRoadModel(createGraph(graphSize, numberOfEmptyConnections)))
                .addModel(pdpModel)
                .addModel(commModel)
                .build();

        for (int i = 0; i < numberOfAgents; i++) {
            sim.register(new CNPAgent(sim.getRandomGenerator()));
        }

        sim.register(new BatteryStation(sim.getRandomGenerator()));
        sim.register(new BatteryStation(sim.getRandomGenerator()));
        sim.register(new BatteryStation(sim.getRandomGenerator()));
        sim.register(new BatteryStation(sim.getRandomGenerator()));
        sim.register(new TaskStation(sim.getRandomGenerator()));
        sim.register(new TaskStation(sim.getRandomGenerator()));
        sim.register(new TaskStation(sim.getRandomGenerator()));
        sim.register(new TaskStation(sim.getRandomGenerator()));


        View.create(sim)
                .with(GraphRoadModelRenderer.builder()
                )
                .with(RoadUserRenderer.builder()
                                .addColorAssociation(BatteryStation.class, new RGB(0, 255, 0))
                                .addColorAssociation(CNPAgent.class, new RGB(255,0 , 0))
                                .addColorAssociation(TaskStation.class, new RGB(0,0,255))
                )
                .with(CommRenderer.builder()
                                .showReliabilityColors()
                )
                .show();
    }

    static ImmutableTable<Integer, Integer, Point> createMatrix(int cols, int rows) {
        final ImmutableTable.Builder<Integer, Integer, Point> builder = ImmutableTable
                .builder();
        for (int c = 0; c < cols; c++) {
            for (int r = 0; r < rows; r++) {
                builder.put(r, c, new Point(c,r));
            }
        }
        return builder.build();
    }

    static ListenableGraph<LengthData> createGraph(int size, int numberOfEmptyConnections) {
        final Graph<LengthData> g = new TableGraph<>();
        final Table<Integer, Integer, Point> matrix = createMatrix(size, size);
        ArrayList<Integer> emptyConnections = generateEmptyConnections(size, numberOfEmptyConnections);

        for (int i = 0; i < size; i++) {
            if (!emptyConnections.contains(i)) {
                Iterable<Point> pathCol = matrix.column(i).values();
                Iterable<Point> pathRow = matrix.row(i).values();
                Graphs.addBiPath(g, pathCol);
                Graphs.addBiPath(g, pathRow);
            }
        }

        return new ListenableGraph<>(g);
    }

    private static ArrayList<Integer> generateEmptyConnections(int n, int count) {
        Random random = new Random();
        ArrayList<Integer> emptyConnections = new ArrayList<Integer>();
        for (int i = 0; i < count; i++) {
            emptyConnections.add(random.nextInt(n));
        }
        return emptyConnections;
    }
}
