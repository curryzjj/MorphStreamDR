package combo;

import common.sink.MeasureSink;
import execution.runtime.tuple.impl.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SINKCombo extends MeasureSink {
    private static final Logger LOG = LoggerFactory.getLogger(SINKCombo.class);
    private static final long serialVersionUID = 5481794109405775823L;
    boolean start_measure = false;
    public long lastTask = -1;
    public long startRecovery = -1;
    public boolean stopRecovery = false;

    public void start() {
        if (!start_measure) {//only once.
            helper.StartMeasurement();
            start_measure = true;
        }
    }

    public void end(int global_cnt) {
        double results = helper.EndMeasurement(global_cnt, remainTime);
        measure_end(results);
    }

    @Override
    public void execute(Tuple input) throws InterruptedException {
        latency_measure(input);
    }

    public void display() {
    }
}
