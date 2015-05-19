/**
 * Created by parallels on 5/19/15.
 */

public class EnergyLoad {

    private CNPAgent agent;
    private long energyLoaded;

    public EnergyLoad(CNPAgent agent, long energyLoaded) {
        this.agent = agent;
        this.energyLoaded = energyLoaded;
    }

    public CNPAgent getAgent() {
        return this.agent;
    }

    public long getEnergyLoaded() {
        return this.energyLoaded;
    }



}