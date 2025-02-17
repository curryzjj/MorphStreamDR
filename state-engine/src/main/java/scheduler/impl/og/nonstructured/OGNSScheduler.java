package scheduler.impl.og.nonstructured;

import durability.struct.FaultToleranceRelax;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import profiler.MeasureTools;
import scheduler.context.og.OGNSContext;
import scheduler.struct.MetaTypes;
import scheduler.struct.og.Operation;
import scheduler.struct.og.OperationChain;
import utils.FaultToleranceConstants;
import utils.SOURCE_CONTROL;

import java.util.concurrent.atomic.AtomicBoolean;

import static common.CONTROL.enable_log;
import static profiler.MeasureTools.BEGIN_SCHEDULE_ABORT_TIME_MEASURE;
import static profiler.MeasureTools.END_SCHEDULE_ABORT_TIME_MEASURE;
import static utils.FaultToleranceConstants.LOGOption_path;

public class OGNSScheduler extends AbstractOGNSScheduler<OGNSContext> {
    private static final Logger log = LoggerFactory.getLogger(OGNSScheduler.class);

    public ExecutableTaskListener executableTaskListener = new ExecutableTaskListener();

    public AtomicBoolean needAbortHandling = new AtomicBoolean(false);

    public OGNSScheduler(int totalThreads, int NUM_ITEMS, int app) {
        super(totalThreads, NUM_ITEMS, app);
    }

    @Override
    public void INITIALIZE(OGNSContext context) {
        needAbortHandling.compareAndSet(true, false);
        tpg.firstTimeExploreTPG(context);
        if (tpg.isLogging == LOGOption_path && FaultToleranceRelax.isSelectiveLogging) {
            this.loggingManager.selectiveLoggingPartition(context.thisThreadId);
        }
        context.partitionStateManager.initialize(executableTaskListener);
        SOURCE_CONTROL.getInstance().waitForOtherThreads(context.thisThreadId);
    }

    public void REINITIALIZE(OGNSContext context) {
        tpg.secondTimeExploreTPG(context);
        SOURCE_CONTROL.getInstance().waitForOtherThreads(context.thisThreadId);
        needAbortHandling.compareAndSet(true, false);
    }

    @Override
    public void start_evaluation(OGNSContext context, long mark_ID, int num_events) {
        INITIALIZE(context);
        do {
            EXPLORE(context);
            PROCESS(context, mark_ID);
        } while (!FINISHED(context));
        SOURCE_CONTROL.getInstance().waitForOtherThreads(context.thisThreadId);
        if (needAbortHandling.get()) {
            BEGIN_SCHEDULE_ABORT_TIME_MEASURE(context.thisThreadId);
            if (enable_log) {
                log.info("need abort handling, rollback and redo");
            }
            REINITIALIZE(context);
            do {
                EXPLORE(context);
                PROCESS(context, mark_ID);
            } while (!FINISHED(context));
            END_SCHEDULE_ABORT_TIME_MEASURE(context.thisThreadId);
        }
        RESET(context);
    }

    /**
     * fast explore dependencies in TPG and put ready/speculative operations into task queues.
     *
     * @param context
     */
    @Override
    public void EXPLORE(OGNSContext context) {
         context.partitionStateManager.handleStateTransitions();
    }

    @Override
    protected void NOTIFY(OperationChain task, OGNSContext context) {
        context.partitionStateManager.onOcExecuted(task);
    }

    @Override
    protected void checkTransactionAbort(Operation operation, OperationChain operationChain) {
        // in coarse-grained algorithms, we will not handle transaction abort gracefully, just update the state of the operation
        operation.stateTransition(MetaTypes.OperationStateType.ABORTED);
        if (isLogging == FaultToleranceConstants.LOGOption_path && operation.getTxnOpId() == 0) {
            MeasureTools.BEGIN_SCHEDULE_TRACKING_TIME_MEASURE(operation.context.thisThreadId);
            this.tpg.threadToPathRecord.get(operationChain.context.thisThreadId).addAbortBid(operation.bid);
            MeasureTools.END_SCHEDULE_TRACKING_TIME_MEASURE(operation.context.thisThreadId);
        }
        // save the abort information and redo the batch.
        needAbortHandling.compareAndSet(false, true);
    }


    /**
     * Register an operation to queue.
     */
    public class ExecutableTaskListener {
        public void onOCExecutable(OperationChain operationChain) {
            DISTRIBUTE(operationChain, (OGNSContext) operationChain.context);//TODO: make it clear..
        }

        public void onOCFinalized(OperationChain operationChain) {
            operationChain.context.scheduledOPs += operationChain.getOperations().size();
        }

        public void onOCRollbacked(OperationChain operationChain) {
            operationChain.context.scheduledOPs -= operationChain.getOperations().size();
        }
    }
}
