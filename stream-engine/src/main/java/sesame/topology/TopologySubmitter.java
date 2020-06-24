package sesame.topology;

import application.CONTROL;
import application.util.Configuration;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sesame.components.Topology;
import sesame.components.TopologyComponent;
import sesame.execution.ExecutionGraph;
import sesame.optimization.OptimizationManager;
import state_engine.common.SpinLock;
import state_engine.profiler.Metrics;

import java.util.Collection;

import static application.CONTROL.enable_shared_state;
import static sesame.controller.affinity.SequentialBinding.SequentialBindingInitilize;
import static state_engine.profiler.Metrics.POST_COMPUTE_COMPLEXITY;


public class TopologySubmitter {
    private final static Logger LOG = LoggerFactory.getLogger(TopologySubmitter.class);
    private OptimizationManager OM;
    public OptimizationManager getOM() {
        return OM;
    }
    public void setOM(OptimizationManager OM) {
        this.OM = OM;
    }

    /**
     * TODO: support different configurations in TM.
     */

    public Topology submitTopology(Topology topology, Configuration conf) {
        //compile
        ExecutionGraph g = new TopologyComiler().generateEG(topology, conf);
        Collection<TopologyComponent> topologyComponents = g.topology.getRecords().values();

        if (CONTROL.enable_shared_state) {
            Metrics.COMPUTE_COMPLEXITY = conf.getInt("COMPUTE_COMPLEXITY");
            POST_COMPUTE_COMPLEXITY = conf.getInt("POST_COMPUTE");
            Metrics.NUM_ACCESSES = conf.getInt("NUM_ACCESS");
            Metrics.NUM_ITEMS = conf.getInt("NUM_ITEMS");
            Metrics.H2_SIZE = Metrics.NUM_ITEMS / conf.getInt("tthread");
        }

        //launch
        OM = new OptimizationManager(g, conf, conf.getBoolean("profile", false),
                conf.getDouble("relax", 1), topology.getPlatform());//support different kinds of optimization module.

        if (enable_shared_state) {
            SequentialBindingInitilize();
            LOG.info("DB initialize starts @" + DateTime.now());
            long start = System.nanoTime();

            int tthread = conf.getInt("tthread");
            g.topology.spinlock = new SpinLock[tthread];//number of threads -- number of cores -- number of partitions.
            g.topology.tableinitilizer = topology.txnTopology.initializeDB(g.topology.spinlock); //For simplicity, assume all table shares the same partition mapping.
            long end = System.nanoTime();

            LOG.info("DB initialize takes:" + (end - start) / 1E6 + " ms");
            OM.lanuch(g.topology, topology.getPlatform(), topology.db);
        } else
            OM.lanuch(topology, topology.getPlatform(), topology.db);

        OM.start();
        return g.topology;
    }
}
