package common.topology.transactional.initializer;

import benchmark.DataHolder;
import benchmark.datagenerator.DataGenerator;
import benchmark.datagenerator.DataGeneratorConfig;
import benchmark.datagenerator.apps.GS.TPGTxnGenerator.GSTPGDataGenerator;
import benchmark.datagenerator.apps.GS.TPGTxnGenerator.GSTPGDataGeneratorConfig;
import benchmark.datagenerator.apps.GS.TPGTxnGenerator.GSTPGDynamicDataGenerator;
import benchmark.datagenerator.apps.SL.TPGTxnGenerator.SLTPGDataGeneratorConfig;
import benchmark.dynamicWorkloadGenerator.DynamicDataGeneratorConfig;
import benchmark.dynamicWorkloadGenerator.DynamicWorkloadGenerator;
import common.collections.Configuration;
import common.collections.OsUtils;
import common.param.TxnEvent;
import common.param.mb.MicroEvent;
import db.Database;
import db.DatabaseException;
import lock.SpinLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scheduler.context.SchedulerContext;
import storage.SchemaRecord;
import storage.TableRecord;
import storage.datatype.DataBox;
import storage.datatype.IntDataBox;
import storage.datatype.LongDataBox;
import storage.datatype.StringDataBox;
import storage.table.RecordSchema;
import transaction.TableInitilizer;
import utils.AppConfig;

import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

import static common.CONTROL.enable_log;
import static common.CONTROL.enable_states_partition;
import static common.Constants.Event_Path;
import static profiler.Metrics.NUM_ITEMS;
import static transaction.State.configure_store;

public class GSInitializer extends TableInitilizer {
    private static final Logger LOG = LoggerFactory.getLogger(GSInitializer.class);
    private final int numberOfStates;
    private final int startingValue = 10000;
    //different R-W ratio.
    //just enable one of the decision array
    protected transient boolean[] read_decision;
    int i = 0;
    private String dataRootPath;
    private DataGenerator dataGenerator;
    private final DataGeneratorConfig dataConfig;
    private final int partitionOffset;


    public GSInitializer(Database db, int numberOfStates, double theta, int tthread, Configuration config) {
        super(db, theta, tthread, config);
        floor_interval = (int) Math.floor(numberOfStates / (double) tthread);//NUM_ITEMS / tthread;
        this.dataRootPath = config.getString("rootFilePath")  + OsUtils.OS_wrapper("inputs");
        this.partitionOffset = numberOfStates / tthread;
        this.numberOfStates = numberOfStates;
        // set up generator
        configure_store(theta, tthread, numberOfStates);
        createTPGGenerator(config);
        dataConfig = dataGenerator.getDataConfig();
    }

    protected void createTPGGenerator(Configuration config) {
        if(config.getBoolean("isDynamic")) {
            //TODO:add the dynamic workload dataGenerator
            DynamicDataGeneratorConfig dynamicDataGeneratorConfig = new DynamicDataGeneratorConfig();
            dynamicDataGeneratorConfig.initialize(config);
            configurePath(dynamicDataGeneratorConfig);
            dataGenerator = new GSTPGDynamicDataGenerator(dynamicDataGeneratorConfig);
        }else {
            GSTPGDataGeneratorConfig dataConfig = new GSTPGDataGeneratorConfig();
            dataConfig.initialize(config);
            configurePath(dataConfig);
            dataGenerator = new GSTPGDataGenerator(dataConfig);
        }
    }
    /**
     * Control the input file path.
     * TODO: think carefully which configuration shall vary.
     *
     * @param config
     */
    /**
     * Control the input file path.
     * TODO: think carefully which configuration shall vary.
     *
     * @param dataConfig
     */
    private void configurePath(DataGeneratorConfig dataConfig) {
        MessageDigest digest;
        String subFolder = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes;
            if (dataConfig instanceof GSTPGDataGeneratorConfig)
            bytes = digest.digest(String.format("%d_%d_%d_%d_%d_%d_%d_%d_%d_%s",
                            dataConfig.getTotalThreads(),
                            dataConfig.getTotalEvents(),
                            dataConfig.getnKeyStates(),
                            ((GSTPGDataGeneratorConfig) dataConfig).NUM_ACCESS,
                            ((GSTPGDataGeneratorConfig) dataConfig).State_Access_Skewness,
                            ((GSTPGDataGeneratorConfig) dataConfig).Ratio_of_Overlapped_Keys,
                            ((GSTPGDataGeneratorConfig) dataConfig).Ratio_of_Transaction_Aborts,
                            ((GSTPGDataGeneratorConfig) dataConfig).Transaction_Length,
                            ((GSTPGDataGeneratorConfig) dataConfig).Ratio_of_Multiple_State_Access,
                            AppConfig.isCyclic)
                        .getBytes(StandardCharsets.UTF_8));
            else if (dataConfig instanceof DynamicDataGeneratorConfig)
                bytes = digest.digest(String.format("%d_%d_%d_%d_%d_%d_%d_%d_%d_%s_%s",
                                dataConfig.getTotalThreads(),
                                dataConfig.getTotalEvents(),
                                dataConfig.getnKeyStates(),
                                ((DynamicDataGeneratorConfig) dataConfig).Transaction_Length,
                                ((DynamicDataGeneratorConfig) dataConfig).NUM_ACCESS,
                                ((DynamicDataGeneratorConfig) dataConfig).Ratio_of_Multiple_State_Access,
                                ((DynamicDataGeneratorConfig) dataConfig).State_Access_Skewness,
                                ((DynamicDataGeneratorConfig) dataConfig).Ratio_of_Overlapped_Keys,
                                ((DynamicDataGeneratorConfig) dataConfig).Ratio_of_Transaction_Aborts,
                                AppConfig.isCyclic,
                                config.getString("workloadType"))
                        .getBytes(StandardCharsets.UTF_8));
            else
                bytes = digest.digest(String.format("%d_%d_%d_%s_%s_%s",
                                dataConfig.getTotalThreads(),
                                dataConfig.getTotalEvents(),
                                dataConfig.getnKeyStates(),
                                ((DynamicDataGeneratorConfig) dataConfig).getApp(),
                                AppConfig.isCyclic)
                        .getBytes(StandardCharsets.UTF_8));
            subFolder = OsUtils.osWrapperPostFix(
                    DatatypeConverter.printHexBinary(bytes));
        } catch (Exception e) {
            e.printStackTrace();
        }
        dataConfig.setRootPath(dataConfig.getRootPath() + OsUtils.OS_wrapper(subFolder));
        dataConfig.setIdsPath(dataConfig.getIdsPath() + OsUtils.OS_wrapper(subFolder));
        this.dataRootPath += OsUtils.OS_wrapper(subFolder);
    }

    /**
     * "INSERT INTO Table (key, value_list) VALUES (?, ?);"
     * initial account value_list is 0...?
     */
    private void insertMicroRecord(String key, long value, int pid, SpinLock[] spinlock_, int partition_id) {
        try {
            if (spinlock_ != null)
                db.InsertRecord("MicroTable", new TableRecord(Record(key, value), pid, spinlock_), partition_id);
            else
                db.InsertRecord("MicroTable", new TableRecord(Record(key, value), this.tthread), partition_id);
        } catch (DatabaseException e) {
            e.printStackTrace();
        }
    }

    private SchemaRecord Record(String key, long value) {
        List<DataBox> values = new ArrayList<>();
        values.add(new StringDataBox(key, key.length()));
        values.add(new LongDataBox(value));
        return new SchemaRecord(values);
    }

    @Override
    public void loadDB(int thread_id, int NUM_TASK) {
        loadDB(thread_id, null, NUM_TASK);
    }

    @Override
    public void loadDB(int thread_id, SpinLock[] spinlock, int NUM_TASK) {
        int partition_interval = (int) Math.ceil(numberOfStates / (double) NUM_TASK);
        int left_bound = thread_id * partition_interval;
        int right_bound;
        if (thread_id == NUM_TASK - 1) {//last executor need to handle left-over
            right_bound = config.getInt("NUM_ITEMS");
        } else {
            right_bound = (thread_id + 1) * partition_interval;
        }
        int pid;
        String _key;
        for (int key = left_bound; key < right_bound; key++) {
            pid = get_pid(partition_interval, key);
            _key = String.valueOf(key);
            insertMicroRecord(_key, startingValue , pid, spinlock, thread_id);
        }
        if (enable_log)
            LOG.info("Thread:" + thread_id + " finished loading data from: " + left_bound + " to: " + right_bound);
    }

    @Override
    public void loadDB(SchedulerContext context, int thread_id, int NUMTasks) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void loadDB(SchedulerContext context, int thread_id, SpinLock[] spinlock, int NUMTasks) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean Generate() {
        String folder = dataRootPath;
        File file = new File(folder);
        if (file.exists()) {
            if (config.getBoolean("isDynamic")) {
                dataGenerator.generateTPGProperties();
            }
            if (enable_log) LOG.info("Data already exists.. skipping data generation...");
            return false;
        }
        file.mkdirs();

        dataGenerator.generateStream();//prepare input events.
        if (enable_log) LOG.info(String.format("Data Generator will dump data at %s.", dataRootPath));
        dataGenerator.dumpGeneratedDataToFile();
        if (enable_log) LOG.info("Data Generation is done...");
        dataGenerator.clearDataStructures();
        return true;
    }

    @Override
    protected void Load() throws IOException {
        int totalEvents = dataConfig.getTotalEvents();
        boolean shufflingActive = dataConfig.getShufflingActive();
        String folder = dataConfig.getRootPath();
        File file = new File(folder + "events.txt");
        int[] p_bids = new int[tthread];
        if (file.exists()) {
            if (enable_log) LOG.info("Reading transfer events...");
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            loadMicroEvents(reader, totalEvents, shufflingActive, p_bids);
            reader.close();
        }
    }

    private void loadMicroEvents(BufferedReader reader, int totalEvents, boolean shufflingActive, int[] p_bids) throws IOException  {
        String txn = reader.readLine();
        int count = 0;
//        int p_bids[] = new int[tthread];
        while (txn != null) {
            String[] split = txn.split(",");
            int npid = (int) (Long.parseLong(split[1]) / partitionOffset);
            // construct bid array
            int keyLength = split.length - 3;
            HashMap<Integer, Integer> pids = new HashMap<>();
            long[] keys = new long[keyLength];
            for (int i = 1; i < keyLength + 1; i++) {
                keys[i-1] = Long.parseLong(split[i]);
                pids.put((int) (keys[i-1] / partitionOffset), 0);
            }

            // construct event
            MicroEvent event = new MicroEvent(
                    Integer.parseInt(split[0]), //0-Bid,
                    npid, //1-Pid
                    Arrays.toString(p_bids), //2-Bid_array
                    Arrays.toString(pids.keySet().toArray(new Integer[0])), //3-Partition_index
                    pids.size(), //4-Num_of_partition
                    Arrays.toString(keys), //5-Key_array
                    keyLength,//6-Total_num_access
                    Integer.parseInt(split[keyLength + 1]), //7-Transaction_length
                    Boolean.parseBoolean(split[keyLength + 2]));//8-IsAbort
            event.setTimestamp(event.getBid() * this.increaseTime);
            DataHolder.events.add(event);
            if (enable_log) LOG.debug(String.format("%d deposit read...", count));
            txn = reader.readLine();
        }
        if (enable_log) LOG.info("Done reading transfer events...");
        if (shufflingActive) {
            shuffleEvents(DataHolder.events, totalEvents);
        }
    }

    private void shuffleEvents(ArrayList<TxnEvent> txnEvents, int totalEvents) {
        Random random = new Random();
        int index;
        TxnEvent temp;
        for (int i = totalEvents - 1; i > 0; i--) {
            index = random.nextInt(i + 1);
            temp = txnEvents.get(index);
            txnEvents.set(index, txnEvents.get(i));
            txnEvents.set(i, temp);
        }
    }

    @Override
    public void store(String file_name) throws IOException {
        //Not Used
    }

    @Override
    public List<String> getTranToDecisionConf() {
        return dataGenerator.getTranToDecisionConf();
    }

    private RecordSchema MicroTableSchema() {
        List<DataBox> dataBoxes = new ArrayList<>();
        List<String> fieldNames = new ArrayList<>();
        dataBoxes.add(new IntDataBox());
        dataBoxes.add(new StringDataBox());
        fieldNames.add("Key");//PK
        fieldNames.add("Value");
        return new RecordSchema(fieldNames, dataBoxes);
    }

    public void creates_Table(Configuration config) {
        RecordSchema s = MicroTableSchema();
        db.createTable(s, "MicroTable", config.getInt("tthread"), config.getInt("NUM_ITEMS"));
        try {
            prepare_input_events(config.getInt("totalEvents"));
            int delta = config.getInt("maxThreads") / config.getInt("tthread");
            if (getTranToDecisionConf() != null && getTranToDecisionConf().size() !=0){
                StringBuilder stringBuilder = new StringBuilder();
                for (String decision : getTranToDecisionConf()){
                    for (int i = 0; i < delta; i++) {
                        stringBuilder.append(decision);
                        stringBuilder.append(";");
                    }
                }
                stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                config.put("WorkloadConfig",stringBuilder.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
