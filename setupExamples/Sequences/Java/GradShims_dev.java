
import java.util.ArrayList;
import java.util.List;
import rs2d.spinlab.hardware.controller.HardwareHandler;
import rs2d.spinlab.sequenceGenerator.SequenceGeneratorAbstract;
import rs2d.spinlab.tools.param.BooleanParam;
import rs2d.spinlab.tools.param.Category;
import rs2d.spinlab.tools.param.EnumGroup;
import rs2d.spinlab.tools.param.ListNumberParam;
import rs2d.spinlab.tools.param.NumberEnum;
import rs2d.spinlab.tools.param.NumberParam;
import rs2d.spinlab.tools.param.TextParam;
import rs2d.spinlab.tools.role.RoleEnum;
import rs2d.spinlab.instrument.Instrument;
import rs2d.spinlab.instrument.InstrumentTxChannel;
import rs2d.spinlab.instrument.probe.Probe;
import rs2d.spinlab.instrument.probe.ProbeChannelPower;
import rs2d.spinlab.instrument.util.GradientMath;
import rs2d.spinlab.instrument.util.TxMath;
import rs2d.spinlab.plugins.loaders.LoaderFactory;
import rs2d.spinlab.plugins.loaders.PluginLoaderInterface;
import rs2d.spinlab.sequence.element.TimeElement;
import rs2d.spinlab.sequence.table.Shape;
import rs2d.spinlab.sequence.table.Table;
import rs2d.spinlab.sequence.table.Utility;
import rs2d.spinlab.sequence.table.generator.TableGeneratorInterface;
import rs2d.spinlab.tools.param.MriDefaultParams;
import rs2d.spinlab.tools.table.Order;
import rs2d.spinlab.tools.utility.GradientAxe;
import rs2d.spinlab.tools.utility.Nucleus;

public class GradShims_dev extends SequenceGeneratorAbstract {

    private Nucleus nucleus;
    private double proton_frequency;
    private double gMax;
    private double observe_frequency;
    private int user_matrix_dimension_1D;
    private double spectral_width;
    private double observation_time;
    double min_instruction_delay = 0.000010;     // single instruction minimal duration

    public GradShims_dev() {
        super();
        initParam();
    }

    @Override

    public void init() {
        super.init();
        // Define default, min, max and suggested values regarding the instrument.
        getParamFromName("BASE_FREQ_1").setDefaultValue(Instrument.instance().getDevices().getMagnet().getProtonFrequency());
        getParamFromName("MAGNETIC_FIELD_STRENGTH").setDefaultValue(Instrument.instance().getDevices().getMagnet().getField());
        getParamFromName("DIGITAL_FILTER_SHIFT").setDefaultValue(Instrument.instance().getDevices().getCameleons().get(0).getAcquDeadPointCount());
        getParamFromName("DIGITAL_FILTER_REMOVED").setDefaultValue(Instrument.instance().getDevices().getCameleons().get(0).isRemoveAcquDeadPoint());
    }

    @Override
    public void generate() throws Exception {
        this.beforeRouting();
        if (!this.isRouted()) {
            this.route();
            this.initAfterRouting();
        }
        this.afterRouting();
        this.checkAndFireException();
    }

    private void initAfterRouting() {

        // -----------------------------------------------
        // RX parameters : nucleus, RX gain & frequencies
        // -----------------------------------------------
        // TX parameters : probe & observed nucleus
        List<Integer> txRoute = (List<Integer>) getParamFromName("TX_ROUTE").getValue();    // route TX through Cameleon

        Probe probe = Instrument.instance().getTransmitProbe();
        InstrumentTxChannel txCh = Instrument.instance().getTxChannels().get(txRoute.get(0));
        ProbeChannelPower pulse = TxMath.getProbePower(probe, null, nucleus.name());

        // -----------------------------------------------
        // TX parameters : RF pulse & attenuation
        // -----------------------------------------------
        double tx_length_90 = getSequence().getPublicTable("Tx_length").getFirst().doubleValue();    // get user defined length of RF pulses
        Shape tx_shape_90 = (Shape) getSequence().getPublicTable("Tx_shape");    // get user defined shape of RF pulses
        double tx_amp_90_desired = 40;     // set 180° RF puse arround 80% of Cameleon output (used to adjust PULSE_ATT of the instrument)

        double power_factor_90 = Utility.powerFillingFactor(tx_shape_90);       // get RF pulse power factor from instrument to calculate RF pulse amplitude 
        double instrument_length_90 = pulse.getHardPulse90().x;
        double instrument_power_90 = pulse.getHardPulse90().y / power_factor_90;

        int tx_att = 1 + (int) TxMath.getTxAttFor(instrument_power_90, txCh, tx_amp_90_desired, observe_frequency);
        double tx_amp_90 = TxMath.getTxAmpFor(instrument_power_90, txCh, tx_att, observe_frequency);

        double tx_amp_90_seq = tx_amp_90 * instrument_length_90 / tx_length_90;

        if (tx_amp_90_seq > 100.0) {  // TX LENGTH 90 MIN 
            double tx_length_90_min = Math.ceil((tx_amp_90 * instrument_length_90) / 100.0 * 10000) / 10000;
            getUnreachParamExceptionManager().addParam("TX_LENGTH_90", tx_length_90, tx_length_90_min, ((NumberParam) getParamFromName("TX_LENGTH_90")).getMaxValue(), "Pulse length too short to reach RF power with this pulse shape");
            tx_amp_90_seq = tx_amp_90 * instrument_length_90 / tx_length_90_min;
        }

        // --------------------------------------------------------
        // set calculated parameters to display values & sequence
        // --------------------------------------------------------
        this.setParamValue("PULSE_ATT", tx_att);            // display PULSE_ATT
        this.setParamValue("TX_AMP_90", tx_amp_90_seq);     // display 90� amplitude
    }

    private void beforeRouting() throws Exception {
        // Define parameters need for the tx routing.
        // Example: Observe nucleus, Orientation.

        // -----------------------------------------------
        // RX parameters : nucleus, RX gain & frequencies
        // -----------------------------------------------
        nucleus = Nucleus.getNucleusForName((String) getParamFromName("NUCLEUS_1").getValue());
        proton_frequency = Instrument.instance().getDevices().getMagnet().getProtonFrequency();
        gMax = GradientMath.getMaxGradientStrength();
        observe_frequency = nucleus.getFrequency(proton_frequency);

        setSequenceParamValue("Rx Gain", "RECEIVER_GAIN");
        setParamValue("RECEIVER_COUNT", Instrument.instance().getObservableRxs(nucleus).size());

        setSequenceParamValue("IF", Instrument.instance().getIfFrequency());
        setParamValue("INTERMEDIATE_FREQUENCY", Instrument.instance().getIfFrequency());

        double base_frequency = (Double) getParamFromName("BASE_FREQ_1").getValue();
        setSequenceParamValue("Tx_frequency", base_frequency);
        setParamValue("OBSERVED_FREQUENCY", base_frequency);

        setSequenceParamValue("TxNucleus", "NUCLEUS_1");
        setParamValue("OBSERVED_NUCLEUS", getParamFromName("NUCLEUS_1").getValue());

        user_matrix_dimension_1D = ((NumberParam) getParamFromName("USER_MATRIX_DIMENSION_1D")).getValue().intValue();
        int user_matrix_dimension_2D = ((NumberParam) getParamFromName("USER_MATRIX_DIMENSION_2D")).getValue().intValue();
        int user_matrix_dimension_3D = ((NumberParam) getParamFromName("USER_MATRIX_DIMENSION_3D")).getValue().intValue();
        int user_matrix_dimension_4D = ((NumberParam) getParamFromName("USER_MATRIX_DIMENSION_4D")).getValue().intValue();

        int acquisition_matrix_dimension_1D = user_matrix_dimension_1D;
        int acquisition_matrix_dimension_2D = user_matrix_dimension_2D;
        int acquisition_matrix_dimension_3D = user_matrix_dimension_3D;
        int acquisition_matrix_dimension_4D = user_matrix_dimension_4D;

        setSequenceParamValue("PS", "DUMMY_SCAN");
        setSequenceParamValue("Nb Point", acquisition_matrix_dimension_1D);
        setSequenceParamValue("Nb 1D", "NUMBER_OF_AVERAGES");
        setSequenceParamValue("Nb 2D", user_matrix_dimension_2D);
        setSequenceParamValue("Nb 3D", user_matrix_dimension_3D);
        setSequenceParamValue("Nb 4D", user_matrix_dimension_4D);

        // set the calculated acquisition matrix
        setParamValue("ACQUISITION_MATRIX_DIMENSION_1D", acquisition_matrix_dimension_1D);
        setParamValue("ACQUISITION_MATRIX_DIMENSION_2D", acquisition_matrix_dimension_2D);
        setParamValue("ACQUISITION_MATRIX_DIMENSION_3D", acquisition_matrix_dimension_3D);
        setParamValue("ACQUISITION_MATRIX_DIMENSION_4D", acquisition_matrix_dimension_4D);

        // -----------------------------------------------
        // spectral width & observation time
        // -----------------------------------------------
        spectral_width = ((NumberParam) getParamFromName("SPECTRAL_WIDTH")).getValue().doubleValue();            // get user defined spectral width
        int acq_points = ((NumberParam) getParamFromName("ACQUISITION_MATRIX_DIMENSION_1D")).getValue().intValue();     // get user defined number of points to acquire
        double sw = HardwareHandler.getInstance().getSequenceHandler().getCompiler().getNearestSW(spectral_width);      // get real spectral width from Cameleon
        setSequenceParamValue("SW", sw);    // set spectral width to sequence

        observation_time = acq_points / sw;
        setParamValue("ACQUISITION_TIME_PER_SCAN", observation_time);   // display observation time
        setSequenceTableFirstValue("TO", observation_time);             // set observation time to sequence
    }

    private void afterRouting() throws Exception {
        Shape tx_shape_90 = (Shape) getSequence().getPublicTable("Tx_shape");
        Shape tx_phase_shape_90 = (Shape) getSequence().getPublicTable("Tx_shape_phase");
        tx_phase_shape_90.clear();
        int nb_shape_points = 128;

        setTableValuesFromGaussGen(tx_shape_90, nb_shape_points, 0.250, 100, false);

        setSequenceTableFirstValue("Tx_length", "TX_LENGTH_90");     // set RF pulse length to sequence

        float txAmp = ((NumberParam) this.getParamFromName("TX_AMP_90")).getValue().floatValue();
        float flip_angle = ((NumberParam) this.getParamFromName("FLIP_ANGLE")).getValue().floatValue();

        float newTxAmp = txAmp * flip_angle / 90f;

        setSequenceTableFirstValue("Tx_amp", newTxAmp);           // set 90° RF pulse amplitude to sequence
        setSequenceParamValue("Tx_att", "PULSE_ATT");
        setSequenceTableFirstValue("Last_delay", "REPETITION_TIME");

        double fov = ((NumberParam) getParamFromName("FIELD_OF_VIEW")).getValue().doubleValue();
        double pixel_dimension = fov / user_matrix_dimension_1D;

        // -----------------------------------------------
        // calculate READ gradient amplitude
        // -----------------------------------------------
        double grad_ratio_read_prep = 0.5;      // get prephasing gradient ratio
        double grad_amp_read_prep, grad_amp_read_read;

        grad_amp_read_read = spectral_width / (GradientMath.GAMMA * pixel_dimension * user_matrix_dimension_1D);                 // amplitude in T/m
        if (grad_amp_read_read > gMax) {
            grad_amp_read_read = gMax;
            spectral_width = grad_amp_read_read * (GradientMath.GAMMA * pixel_dimension * user_matrix_dimension_1D);
            spectral_width = HardwareHandler.getInstance().getSequenceHandler().getCompiler().getNearestSW(spectral_width);
        }
        double grad_rise_time = ((NumberParam) getParamFromName("GRADIENT_RISE_TIME")).getValue().doubleValue();
        double grad_shape_rise_factor_up = Utility.voltageFillingFactor((Shape) getSequence().getPublicTable("Grad_shape_up"));
        double grad_shape_rise_factor_down = Utility.voltageFillingFactor((Shape) getSequence().getPublicTable("Grad_shape_down"));
        double grad_shape_rise_time = grad_shape_rise_factor_up * grad_rise_time + grad_shape_rise_factor_down * grad_rise_time;        // shape dependant equivalent rise time
        double grad_phase_application_time = ((NumberParam) getParamFromName("GRADIENT_PHASE_APPLICATION_TIME")).getValue().doubleValue();

        double grad_area_read_prep, grad_area_read_read;

        setParamValue("GradientValue", grad_amp_read_read); // With the amplitude in T/m
        grad_amp_read_read = grad_amp_read_read * 100.0 / gMax;
        grad_area_read_read = (observation_time + grad_shape_rise_time) * grad_amp_read_read;   // area of read gradient

        grad_area_read_prep = grad_area_read_read * grad_ratio_read_prep;                       // area of prephasing read gradient
        grad_amp_read_prep = grad_area_read_prep / (grad_phase_application_time + grad_shape_rise_time);

        double grad_area_sequence_max = 100 * (grad_phase_application_time + grad_shape_rise_time);
        if (grad_area_read_prep > grad_area_sequence_max) {
            double grad_phase_application_time_min = grad_area_read_prep / 100 - grad_shape_rise_time;
            grad_phase_application_time_min = Math.ceil(grad_phase_application_time_min * Math.pow(10, 4)) / (Math.pow(10, 4));
            getUnreachParamExceptionManager().addParam("GRADIENT_PHASE_APPLICATION_TIME", grad_phase_application_time, grad_phase_application_time_min, ((NumberParam) getParamFromName("GRADIENT_PHASE_APPLICATION_TIME")).getMaxValue(), "Gradient application time too short to reach this pixel dimension");
            grad_phase_application_time = grad_phase_application_time_min;
        }

        setSequenceTableFirstValue("Grad_phase_duration", grad_phase_application_time);

        Table grad_amp_read_read_seq = getSequence().getPublicTable("Grad_amp_read_read");
        grad_amp_read_read_seq.clear();
        grad_amp_read_read_seq.setOrder(Order.Two);
        grad_amp_read_read_seq.setLocked(true);
        grad_amp_read_read_seq.add(grad_amp_read_read);
        grad_amp_read_read_seq.add(-grad_amp_read_read);

        Table grad_amp_prep_seq = getSequence().getPublicTable("Grad_amp_read_prep");
        grad_amp_prep_seq.clear();
        grad_amp_prep_seq.setOrder(Order.Two);
        grad_amp_prep_seq.setLocked(true);
        grad_amp_prep_seq.add(-grad_amp_read_prep);
        grad_amp_prep_seq.add(grad_amp_read_prep);

        // We need a conversion between X-Y-Z and R-P-S
        String shimmed_Gradient = ((TextParam) getParamFromName("Shimmed_Gradient")).getValue();

        switch (shimmed_Gradient) {
            case "X":
                getSequence().getParam("Grad_dir").setValue(GradientAxe.R);
                break;
            case "Y":
                getSequence().getParam("Grad_dir").setValue(GradientAxe.P);
                break;
            default:
                getSequence().getParam("Grad_dir").setValue(GradientAxe.S);
                break;
        }

        int event90 = 2;
        int eventDelay1 = 6;
        int eventAcq = 8;

        double echoTime = ((NumberParam) this.getParamFromName(MriDefaultParams.ECHO_TIME.name())).getValue().doubleValue();

        double time1 = getTimeBetweenEvents(event90 + 1, eventAcq - 1);
        double tx_length_90 = getSequence().getPublicTable("Tx_length").getFirst().doubleValue();    // get user defined length of RF pulses
        time1 = time1 + tx_length_90 / 2 + observation_time / 2;
        time1 = removeTimeForEvents(time1, eventDelay1);
        double delay1 = echoTime - time1;

        if ((delay1 < min_instruction_delay)) {

            double echoTimeMin = time1 + min_instruction_delay;
            this.getUnreachParamExceptionManager().addParam(MriDefaultParams.ECHO_TIME.name(), echoTimeMin, echoTimeMin, ((NumberParam) this.getParamFromName(MriDefaultParams.ECHO_TIME.name())).getMaxValue(), "Echo time too short for the User Mx1D and SW");
            delay1 = min_instruction_delay;
        }

        // ------------------------------------------
        // set calculated delays
        // ------------------------------------------
        setSequenceTableFirstValue("Mixing Time", delay1);

    }

    /**
     * Substract the time value of the event corresponding to the "index"
     * parameter from the parameter "time"
     *
     * @param time
     * @param indexEvent The index of the time event to be substract to the
     * value of the parameter time
     * @return The calculated time
     */
    public double removeTimeForEvents(double time, int... indexEvent) {
        for (int i = 0; i < indexEvent.length; i++) {
            time -= ((TimeElement) getSequence().getTimeChannel().get(indexEvent[i])).getTime().getFirst().doubleValue();
        }
        return time;
    }

    /**
     * Calculate the time during 2 including events correspnding to the index
     *
     * @param indexFirstEvent The index of the first time event
     * @param indexLastEvent The index of the last time event
     * @return The total time between the 2 events (including)
     */
    public double getTimeBetweenEvents(int indexFirstEvent, int indexLastEvent) {
        double time = 0;
        for (int i = indexFirstEvent; i < indexLastEvent + 1; i++) {
            time += ((TimeElement) getSequence().getTimeChannel().get(i)).getTime().getFirst().doubleValue();
        }
        return time;
    }

    /**
     * Generate a table of element with a Gaussian generator
     *
     * @param table The table to be set
     * @param nbpoint The number of point of the generated gaussian
     * @param width The width of the generated gaussian
     * @param amp The amplitude of the generated gaussian (in %)
     * @param abs true if you want the absolute values and false otherwise
     */
    private void setTableValuesFromGaussGen(Table table, int nbpoint, double width, double amp, Boolean abs) throws Exception {
        TableGeneratorInterface gen = loadTableGenerator("Gaussian");
        gen.getParams().get(0).setValue(nbpoint);
        gen.getParams().get(1).setValue(width);
        gen.getParams().get(2).setValue(amp);
        gen.getParams().get(3).setValue(abs);//abs

        table.setGenerator(gen);
        if (gen == null) {
            table.clear();
            table.setFirst(100);
        } else {
            gen.generate();
        }
    }

    private TableGeneratorInterface loadTableGenerator(String generatorName) throws Exception {
        TableGeneratorInterface gen = null;
        PluginLoaderInterface loader = LoaderFactory.getTableGeneratorPluginLoader();
        if (loader.containsPlugin(generatorName)) {
            gen = (TableGeneratorInterface) loader.getPluginByName(generatorName);
        }
        return gen;

    }

    @Override
    public ArrayList<RoleEnum> getPluginAccess() {
        ArrayList<RoleEnum> roleEnums = new ArrayList<>();
        roleEnums.add(RoleEnum.User);
        return roleEnums;
    }

    //GEN Generate Parameters
//<editor-fold defaultstate="collapsed" desc="Generated Code">


public void initParam() {
    super.initParam();
    this.initAccu_dim();
    this.initAcquisition_matrix_dimension_1d();
    this.initAcquisition_matrix_dimension_2d();
    this.initAcquisition_matrix_dimension_3d();
    this.initAcquisition_matrix_dimension_4d();
    this.initAcquisition_time_per_scan();
    this.initBase_freq_1();
    this.initBase_freq_2();
    this.initBase_freq_3();
    this.initBase_freq_4();
    this.initDigital_filter_removed();
    this.initDigital_filter_shift();
    this.initDummy_scan();
    this.initEcho_time();
    this.initField_of_view();
    this.initFlip_angle();
    this.initGradient_phase_application_time();
    this.initGradient_rise_time();
    this.initGradientvalue();
    this.initImage_orientation_specimen();
    this.initImage_position_specimen();
    this.initIntermediate_frequency();
    this.initLast_put();
    this.initMagnetic_field_strength();
    this.initModality();
    this.initNucleus_1();
    this.initNucleus_2();
    this.initNucleus_3();
    this.initNucleus_4();
    this.initNumber_of_averages();
    this.initObserved_frequency();
    this.initObserved_nucleus();
    this.initOffset_freq_1();
    this.initOffset_freq_2();
    this.initOffset_freq_3();
    this.initOffset_freq_4();
    this.initOrientation();
    this.initParopt_param();
    this.initProbe();
    this.initPulse_att();
    this.initReceiver_count();
    this.initReceiver_gain();
    this.initRepetition_time();
    this.initSequence_name();
    this.initSetup_mode();
    this.initShimmed_gradient();
    this.initSlice_thickness();
    this.initSpecimen_position();
    this.initSpectral_width();
    this.initTransform_plugin();
    this.initTx_amp_90();
    this.initTx_length_90();
    this.initTx_route();
    this.initUser_matrix_dimension_1d();
    this.initUser_matrix_dimension_2d();
    this.initUser_matrix_dimension_3d();
    this.initUser_matrix_dimension_4d();
}

private void initAccu_dim(){
    NumberParam accu_dim = new NumberParam();
    accu_dim.setMinValue(0);
    accu_dim.setMaxValue(3);
    accu_dim.setNumberEnum(NumberEnum.valueOf("Integer"));
    accu_dim.setDefaultValue(1);
    accu_dim.setValue(1);
    accu_dim.setRestrictedToSuggested(false);
    accu_dim.setName("ACCU_DIM");
    accu_dim.setDescription("ACCU_DIM.description");
    accu_dim.setLocked(true);
    accu_dim.setLockedToDefault(false);
    RoleEnum[] roles;
    roles = new RoleEnum[1];
    roles[0] = RoleEnum.valueOf("User");
    accu_dim.setRoles(roles);
    accu_dim.setGroup(EnumGroup.valueOf("Scan"));
    accu_dim.setCategory(Category.valueOf("Acquisition"));
    this.addParam(accu_dim);
}

private void initAcquisition_matrix_dimension_1d(){
    NumberParam acquisition_matrix_dimension_1d = new NumberParam();
    acquisition_matrix_dimension_1d.setMinValue(0);
    acquisition_matrix_dimension_1d.setMaxValue(65536);
    acquisition_matrix_dimension_1d.setNumberEnum(NumberEnum.valueOf("Scan"));
    acquisition_matrix_dimension_1d.setDefaultValue(128);
    acquisition_matrix_dimension_1d.setValue(256);
    acquisition_matrix_dimension_1d.setRestrictedToSuggested(false);
    acquisition_matrix_dimension_1d.setName("ACQUISITION_MATRIX_DIMENSION_1D");
    acquisition_matrix_dimension_1d.setDescription("The acquisition size of the first dimension");
    acquisition_matrix_dimension_1d.setLocked(true);
    acquisition_matrix_dimension_1d.setLockedToDefault(false);
    RoleEnum[] roles;
    roles = new RoleEnum[1];
    roles[0] = RoleEnum.valueOf("User");
    acquisition_matrix_dimension_1d.setRoles(roles);
    acquisition_matrix_dimension_1d.setGroup(EnumGroup.valueOf("Scan"));
    acquisition_matrix_dimension_1d.setCategory(Category.valueOf("Acquisition"));
    this.addParam(acquisition_matrix_dimension_1d);
}

private void initAcquisition_matrix_dimension_2d(){
    NumberParam acquisition_matrix_dimension_2d = new NumberParam();
    acquisition_matrix_dimension_2d.setMinValue(0);
    acquisition_matrix_dimension_2d.setMaxValue(65536);
    acquisition_matrix_dimension_2d.setNumberEnum(NumberEnum.valueOf("Scan"));
    acquisition_matrix_dimension_2d.setDefaultValue(128);
    acquisition_matrix_dimension_2d.setValue(2);
    acquisition_matrix_dimension_2d.setRestrictedToSuggested(false);
    acquisition_matrix_dimension_2d.setName("ACQUISITION_MATRIX_DIMENSION_2D");
    acquisition_matrix_dimension_2d.setDescription("The acquisition size of the second dimension");
    acquisition_matrix_dimension_2d.setLocked(true);
    acquisition_matrix_dimension_2d.setLockedToDefault(false);
    RoleEnum[] roles;
    roles = new RoleEnum[1];
    roles[0] = RoleEnum.valueOf("User");
    acquisition_matrix_dimension_2d.setRoles(roles);
    acquisition_matrix_dimension_2d.setGroup(EnumGroup.valueOf("Scan"));
    acquisition_matrix_dimension_2d.setCategory(Category.valueOf("Acquisition"));
    this.addParam(acquisition_matrix_dimension_2d);
}

private void initAcquisition_matrix_dimension_3d(){
    NumberParam acquisition_matrix_dimension_3d = new NumberParam();
    acquisition_matrix_dimension_3d.setMinValue(0);
    acquisition_matrix_dimension_3d.setMaxValue(65536);
    acquisition_matrix_dimension_3d.setNumberEnum(NumberEnum.valueOf("Scan"));
    acquisition_matrix_dimension_3d.setDefaultValue(1);
    acquisition_matrix_dimension_3d.setValue(1);
    acquisition_matrix_dimension_3d.setRestrictedToSuggested(false);
    acquisition_matrix_dimension_3d.setName("ACQUISITION_MATRIX_DIMENSION_3D");
    acquisition_matrix_dimension_3d.setDescription("The acquisition size of the third dimension");
    acquisition_matrix_dimension_3d.setLocked(true);
    acquisition_matrix_dimension_3d.setLockedToDefault(false);
    RoleEnum[] roles;
    roles = new RoleEnum[1];
    roles[0] = RoleEnum.valueOf("User");
    acquisition_matrix_dimension_3d.setRoles(roles);
    acquisition_matrix_dimension_3d.setGroup(EnumGroup.valueOf("Scan"));
    acquisition_matrix_dimension_3d.setCategory(Category.valueOf("Acquisition"));
    this.addParam(acquisition_matrix_dimension_3d);
}

private void initAcquisition_matrix_dimension_4d(){
    NumberParam acquisition_matrix_dimension_4d = new NumberParam();
    acquisition_matrix_dimension_4d.setMinValue(0);
    acquisition_matrix_dimension_4d.setMaxValue(65536);
    acquisition_matrix_dimension_4d.setNumberEnum(NumberEnum.valueOf("Scan"));
    acquisition_matrix_dimension_4d.setDefaultValue(1);
    acquisition_matrix_dimension_4d.setValue(1);
    acquisition_matrix_dimension_4d.setRestrictedToSuggested(false);
    acquisition_matrix_dimension_4d.setName("ACQUISITION_MATRIX_DIMENSION_4D");
    acquisition_matrix_dimension_4d.setDescription("The acquisition size of the fourth dimension");
    acquisition_matrix_dimension_4d.setLocked(true);
    acquisition_matrix_dimension_4d.setLockedToDefault(false);
    RoleEnum[] roles;
    roles = new RoleEnum[1];
    roles[0] = RoleEnum.valueOf("User");
    acquisition_matrix_dimension_4d.setRoles(roles);
    acquisition_matrix_dimension_4d.setGroup(EnumGroup.valueOf("Scan"));
    acquisition_matrix_dimension_4d.setCategory(Category.valueOf("Acquisition"));
    this.addParam(acquisition_matrix_dimension_4d);
}

private void initAcquisition_time_per_scan(){
    NumberParam acquisition_time_per_scan = new NumberParam();
    acquisition_time_per_scan.setMinValue(0.0);
    acquisition_time_per_scan.setMaxValue(1.0E9);
    acquisition_time_per_scan.setNumberEnum(NumberEnum.valueOf("Time"));
    acquisition_time_per_scan.setDefaultValue(1.0);
    acquisition_time_per_scan.setValue(0.007995392);
    acquisition_time_per_scan.setRestrictedToSuggested(false);
    acquisition_time_per_scan.setName("ACQUISITION_TIME_PER_SCAN");
    acquisition_time_per_scan.setDescription("The acquisition time per scan");
    acquisition_time_per_scan.setLocked(true);
    acquisition_time_per_scan.setLockedToDefault(false);
    RoleEnum[] roles;
    roles = new RoleEnum[1];
    roles[0] = RoleEnum.valueOf("User");
    acquisition_time_per_scan.setRoles(roles);
    acquisition_time_per_scan.setGroup(EnumGroup.valueOf("Reception"));
    acquisition_time_per_scan.setCategory(Category.valueOf("Acquisition"));
    this.addParam(acquisition_time_per_scan);
}

private void initBase_freq_1(){
    NumberParam base_freq_1 = new NumberParam();
    base_freq_1.setMinValue(0.0);
    base_freq_1.setMaxValue(3.0E9);
    base_freq_1.setNumberEnum(NumberEnum.valueOf("Frequency"));
    base_freq_1.setDefaultValue(1.274981876E8);
    base_freq_1.setValue(1.274981876E8);
    base_freq_1.setRestrictedToSuggested(false);
    base_freq_1.setName("BASE_FREQ_1");
    base_freq_1.setDescription("The base frequency of the first sequence channel");
    base_freq_1.setLocked(true);
    base_freq_1.setLockedToDefault(true);
    RoleEnum[] roles;
    roles = new RoleEnum[1];
    roles[0] = RoleEnum.valueOf("User");
    base_freq_1.setRoles(roles);
    base_freq_1.setGroup(EnumGroup.valueOf("Emission"));
    base_freq_1.setCategory(Category.valueOf("Acquisition"));
    this.addParam(base_freq_1);
}

private void initBase_freq_2(){
    NumberParam base_freq_2 = new NumberParam();
    base_freq_2.setMinValue(0.0);
    base_freq_2.setMaxValue(3.0E9);
    base_freq_2.setNumberEnum(NumberEnum.valueOf("Frequency"));
    base_freq_2.setDefaultValue(0.0);
    base_freq_2.setValue(0.0);
    base_freq_2.setRestrictedToSuggested(false);
    base_freq_2.setName("BASE_FREQ_2");
    base_freq_2.setDescription("The base frequency of the second sequence channel");
    base_freq_2.setLocked(true);
    base_freq_2.setLockedToDefault(false);
    RoleEnum[] roles;
    roles = new RoleEnum[1];
    roles[0] = RoleEnum.valueOf("User");
    base_freq_2.setRoles(roles);
    base_freq_2.setGroup(EnumGroup.valueOf("Emission"));
    base_freq_2.setCategory(Category.valueOf("Acquisition"));
    this.addParam(base_freq_2);
}

private void initBase_freq_3(){
    NumberParam base_freq_3 = new NumberParam();
    base_freq_3.setMinValue(0.0);
    base_freq_3.setMaxValue(3.0E9);
    base_freq_3.setNumberEnum(NumberEnum.valueOf("Frequency"));
    base_freq_3.setDefaultValue(0.0);
    base_freq_3.setValue(0.0);
    base_freq_3.setRestrictedToSuggested(false);
    base_freq_3.setName("BASE_FREQ_3");
    base_freq_3.setDescription("The base frequency of the third sequence channel");
    base_freq_3.setLocked(true);
    base_freq_3.setLockedToDefault(false);
    RoleEnum[] roles;
    roles = new RoleEnum[1];
    roles[0] = RoleEnum.valueOf("User");
    base_freq_3.setRoles(roles);
    base_freq_3.setGroup(EnumGroup.valueOf("Emission"));
    base_freq_3.setCategory(Category.valueOf("Acquisition"));
    this.addParam(base_freq_3);
}

private void initBase_freq_4(){
    NumberParam base_freq_4 = new NumberParam();
    base_freq_4.setMinValue(0.0);
    base_freq_4.setMaxValue(3.0E9);
    base_freq_4.setNumberEnum(NumberEnum.valueOf("Frequency"));
    base_freq_4.setDefaultValue(0.0);
    base_freq_4.setValue(0.0);
    base_freq_4.setRestrictedToSuggested(false);
    base_freq_4.setName("BASE_FREQ_4");
    base_freq_4.setDescription("The base frequency of the fourth sequence channel");
    base_freq_4.setLocked(true);
    base_freq_4.setLockedToDefault(false);
    RoleEnum[] roles;
    roles = new RoleEnum[1];
    roles[0] = RoleEnum.valueOf("User");
    base_freq_4.setRoles(roles);
    base_freq_4.setGroup(EnumGroup.valueOf("Emission"));
    base_freq_4.setCategory(Category.valueOf("Acquisition"));
    this.addParam(base_freq_4);
}

private void initDigital_filter_removed(){
    BooleanParam digital_filter_removed = new BooleanParam();
    digital_filter_removed.setDefaultValue(true);
    digital_filter_removed.setValue(false);
    digital_filter_removed.setName("DIGITAL_FILTER_REMOVED");
    digital_filter_removed.setDescription("Data shift due to the digital filter are removed");
    digital_filter_removed.setLocked(true);
    digital_filter_removed.setLockedToDefault(false);
    RoleEnum[] roles;
    roles = new RoleEnum[1];
    roles[0] = RoleEnum.valueOf("User");
    digital_filter_removed.setRoles(roles);
    digital_filter_removed.setGroup(EnumGroup.valueOf("Reception"));
    digital_filter_removed.setCategory(Category.valueOf("Acquisition"));
    this.addParam(digital_filter_removed);
}

private void initDigital_filter_shift(){
    NumberParam digital_filter_shift = new NumberParam();
    digital_filter_shift.setMinValue(-2147483648);
    digital_filter_shift.setMaxValue(2147483647);
    digital_filter_shift.setNumberEnum(NumberEnum.valueOf("Integer"));
    digital_filter_shift.setDefaultValue(20);
    digital_filter_shift.setValue(0);
    digital_filter_shift.setRestrictedToSuggested(false);
    digital_filter_shift.setName("DIGITAL_FILTER_SHIFT");
    digital_filter_shift.setDescription("Data shift due to the digital filter");
    digital_filter_shift.setLocked(true);
    digital_filter_shift.setLockedToDefault(false);
    RoleEnum[] roles;
    roles = new RoleEnum[1];
    roles[0] = RoleEnum.valueOf("User");
    digital_filter_shift.setRoles(roles);
    digital_filter_shift.setGroup(EnumGroup.valueOf("Reception"));
    digital_filter_shift.setCategory(Category.valueOf("Acquisition"));
    this.addParam(digital_filter_shift);
}

private void initDummy_scan(){
    NumberParam dummy_scan = new NumberParam();
    dummy_scan.setMinValue(0);
    dummy_scan.setMaxValue(65536);
    dummy_scan.setNumberEnum(NumberEnum.valueOf("Scan"));
    dummy_scan.setDefaultValue(128);
    dummy_scan.setValue(0);
    dummy_scan.setRestrictedToSuggested(false);
    dummy_scan.setName("DUMMY_SCAN");
    dummy_scan.setDescription("Dummy Scan");
    dummy_scan.setLocked(true);
    dummy_scan.setLockedToDefault(false);
    RoleEnum[] roles;
    roles = new RoleEnum[1];
    roles[0] = RoleEnum.valueOf("User");
    dummy_scan.setRoles(roles);
    dummy_scan.setGroup(EnumGroup.valueOf("Scan"));
    dummy_scan.setCategory(Category.valueOf("Acquisition"));
    this.addParam(dummy_scan);
}

private void initEcho_time(){
    NumberParam echo_time = new NumberParam();
    echo_time.setMinValue(0.0);
    echo_time.setMaxValue(1.0E9);
    echo_time.setNumberEnum(NumberEnum.valueOf("Time"));
    echo_time.setDefaultValue(0.005);
    echo_time.setValue(0.01);
    echo_time.setRestrictedToSuggested(false);
    echo_time.setName("ECHO_TIME");
    echo_time.setDescription("The echo time");
    echo_time.setLocked(false);
    echo_time.setLockedToDefault(false);
    RoleEnum[] roles;
    roles = new RoleEnum[1];
    roles[0] = RoleEnum.valueOf("User");
    echo_time.setRoles(roles);
    echo_time.setGroup(EnumGroup.valueOf("Delay"));
    echo_time.setCategory(Category.valueOf("Acquisition"));
    this.addParam(echo_time);
}

private void initField_of_view(){
    NumberParam field_of_view = new NumberParam();
    field_of_view.setMinValue(0.001);
    field_of_view.setMaxValue(1.7976931348623157E308);
    field_of_view.setNumberEnum(NumberEnum.valueOf("Length"));
    field_of_view.setDefaultValue(0.6);
    field_of_view.setValue(0.06);
    field_of_view.setRestrictedToSuggested(false);
    field_of_view.setName("FIELD_OF_VIEW");
    field_of_view.setDescription("The field of view");
    field_of_view.setLocked(false);
    field_of_view.setLockedToDefault(false);
    RoleEnum[] roles;
    roles = new RoleEnum[1];
    roles[0] = RoleEnum.valueOf("User");
    field_of_view.setRoles(roles);
    field_of_view.setGroup(EnumGroup.valueOf("Dimension"));
    field_of_view.setCategory(Category.valueOf("Acquisition"));
    this.addParam(field_of_view);
}

private void initFlip_angle(){
    NumberParam flip_angle = new NumberParam();
    flip_angle.setMinValue(-360.0);
    flip_angle.setMaxValue(360.0);
    flip_angle.setNumberEnum(NumberEnum.valueOf("Angle"));
    flip_angle.setDefaultValue(5.0);
    flip_angle.setValue(90.0);
    flip_angle.setRestrictedToSuggested(false);
    flip_angle.setName("FLIP_ANGLE");
    flip_angle.setDescription("The flip angle for the excitation");
    flip_angle.setLocked(false);
    flip_angle.setLockedToDefault(false);
    RoleEnum[] roles;
    roles = new RoleEnum[1];
    roles[0] = RoleEnum.valueOf("User");
    flip_angle.setRoles(roles);
    flip_angle.setGroup(EnumGroup.valueOf("Emission"));
    flip_angle.setCategory(Category.valueOf("Acquisition"));
    this.addParam(flip_angle);
}

private void initGradient_phase_application_time(){
    NumberParam gradient_phase_application_time = new NumberParam();
    gradient_phase_application_time.setMinValue(1.0000000000000001E-7);
    gradient_phase_application_time.setMaxValue(0.1);
    gradient_phase_application_time.setNumberEnum(NumberEnum.valueOf("Time"));
    gradient_phase_application_time.setDefaultValue(5.0E-4);
    gradient_phase_application_time.setValue(5.0E-4);
    gradient_phase_application_time.setRestrictedToSuggested(false);
    gradient_phase_application_time.setName("GRADIENT_PHASE_APPLICATION_TIME");
    gradient_phase_application_time.setDescription("");
    gradient_phase_application_time.setLocked(false);
    gradient_phase_application_time.setLockedToDefault(false);
    RoleEnum[] roles;
    roles = new RoleEnum[1];
    roles[0] = RoleEnum.valueOf("User");
    gradient_phase_application_time.setRoles(roles);
    gradient_phase_application_time.setGroup(EnumGroup.valueOf("Gradient"));
    gradient_phase_application_time.setCategory(Category.valueOf("Acquisition"));
    this.addParam(gradient_phase_application_time);
}

private void initGradient_rise_time(){
    NumberParam gradient_rise_time = new NumberParam();
    gradient_rise_time.setMinValue(1.0000000000000001E-7);
    gradient_rise_time.setMaxValue(0.001);
    gradient_rise_time.setNumberEnum(NumberEnum.valueOf("Time"));
    gradient_rise_time.setDefaultValue(1.9999999999999998E-4);
    gradient_rise_time.setValue(1.9999999999999998E-4);
    gradient_rise_time.setRestrictedToSuggested(false);
    gradient_rise_time.setName("GRADIENT_RISE_TIME");
    gradient_rise_time.setDescription("");
    gradient_rise_time.setLocked(false);
    gradient_rise_time.setLockedToDefault(false);
    RoleEnum[] roles;
    roles = new RoleEnum[1];
    roles[0] = RoleEnum.valueOf("User");
    gradient_rise_time.setRoles(roles);
    gradient_rise_time.setGroup(EnumGroup.valueOf("Gradient"));
    gradient_rise_time.setCategory(Category.valueOf("Acquisition"));
    this.addParam(gradient_rise_time);
}

private void initGradientvalue(){
    NumberParam gradientvalue = new NumberParam();
    gradientvalue.setMinValue(0.0);
    gradientvalue.setMaxValue(1.7976931348623157E308);
    gradientvalue.setNumberEnum(NumberEnum.valueOf("Double"));
    gradientvalue.setDefaultValue(0.0);
    gradientvalue.setValue(0.0);
    gradientvalue.setRestrictedToSuggested(false);
    gradientvalue.setName("GradientValue");
    gradientvalue.setDescription("");
    gradientvalue.setLocked(true);
    gradientvalue.setLockedToDefault(false);
    RoleEnum[] roles;
    roles = new RoleEnum[1];
    roles[0] = RoleEnum.valueOf("User");
    gradientvalue.setRoles(roles);
    gradientvalue.setGroup(EnumGroup.valueOf("Gradient"));
    gradientvalue.setCategory(Category.valueOf("Acquisition"));
    this.addParam(gradientvalue);
}

private void initImage_orientation_specimen(){
    ListNumberParam image_orientation_specimen = new ListNumberParam();
    image_orientation_specimen.setMinValue(-1.7976931348623157E308);
    image_orientation_specimen.setMaxValue(1.7976931348623157E308);
    image_orientation_specimen.setNumberEnum(NumberEnum.valueOf("Double"));
    List<Number> listValue = new ArrayList<Number>();
    listValue.add(1.0);
    listValue.add(0.0);
    listValue.add(0.0);
    listValue.add(0.0);
    listValue.add(1.0);
    listValue.add(0.0);
    image_orientation_specimen.setValue(listValue);
    List<Number> listDefaultValue = new ArrayList<Number>();
    listDefaultValue.add(1.0);
    listDefaultValue.add(0.0);
    listDefaultValue.add(0.0);
    listDefaultValue.add(0.0);
    listDefaultValue.add(1.0);
    listDefaultValue.add(0.0);
    image_orientation_specimen.setDefaultValue(listDefaultValue);
    image_orientation_specimen.setName("IMAGE_ORIENTATION_SPECIMEN");
    image_orientation_specimen.setDescription("Direction cosines of the first row and the first column with respect to the Specimen");
    image_orientation_specimen.setLocked(true);
    image_orientation_specimen.setLockedToDefault(false);
    RoleEnum[] roles;
    roles = new RoleEnum[1];
    roles[0] = RoleEnum.valueOf("User");
    image_orientation_specimen.setRoles(roles);
    image_orientation_specimen.setGroup(EnumGroup.valueOf("Dimension"));
    image_orientation_specimen.setCategory(Category.valueOf("Acquisition"));
    this.addParam(image_orientation_specimen);
}

private void initImage_position_specimen(){
    ListNumberParam image_position_specimen = new ListNumberParam();
    image_position_specimen.setMinValue(-1.7976931348623157E308);
    image_position_specimen.setMaxValue(1.7976931348623157E308);
    image_position_specimen.setNumberEnum(NumberEnum.valueOf("Location"));
    List<Number> listValue = new ArrayList<Number>();
    listValue.add(0.0);
    listValue.add(0.0);
    listValue.add(0.0);
    image_position_specimen.setValue(listValue);
    List<Number> listDefaultValue = new ArrayList<Number>();
    listDefaultValue.add(0.0);
    listDefaultValue.add(0.0);
    listDefaultValue.add(0.0);
    image_position_specimen.setDefaultValue(listDefaultValue);
    image_position_specimen.setName("IMAGE_POSITION_SPECIMEN");
    image_position_specimen.setDescription("x, y, and z coordinates of the upper left hand corner of the image");
    image_position_specimen.setLocked(true);
    image_position_specimen.setLockedToDefault(false);
    RoleEnum[] roles;
    roles = new RoleEnum[1];
    roles[0] = RoleEnum.valueOf("User");
    image_position_specimen.setRoles(roles);
    image_position_specimen.setGroup(EnumGroup.valueOf("Dimension"));
    image_position_specimen.setCategory(Category.valueOf("Acquisition"));
    this.addParam(image_position_specimen);
}

private void initIntermediate_frequency(){
    NumberParam intermediate_frequency = new NumberParam();
    intermediate_frequency.setMinValue(0.0);
    intermediate_frequency.setMaxValue(3.0E9);
    intermediate_frequency.setNumberEnum(NumberEnum.valueOf("Frequency"));
    intermediate_frequency.setDefaultValue(2.15E7);
    intermediate_frequency.setValue(2.15E7);
    intermediate_frequency.setRestrictedToSuggested(false);
    intermediate_frequency.setName("INTERMEDIATE_FREQUENCY");
    intermediate_frequency.setDescription("INTERMEDIATE_FREQUENCY.description");
    intermediate_frequency.setLocked(true);
    intermediate_frequency.setLockedToDefault(false);
    RoleEnum[] roles;
    roles = new RoleEnum[1];
    roles[0] = RoleEnum.valueOf("User");
    intermediate_frequency.setRoles(roles);
    intermediate_frequency.setGroup(EnumGroup.valueOf("Reception"));
    intermediate_frequency.setCategory(Category.valueOf("Acquisition"));
    this.addParam(intermediate_frequency);
}

private void initLast_put(){
    ListNumberParam last_put = new ListNumberParam();
    last_put.setMinValue(-2147483648);
    last_put.setMaxValue(2147483647);
    last_put.setNumberEnum(NumberEnum.valueOf("Integer"));
    List<Number> listValue = new ArrayList<Number>();
    listValue.add(0);
    listValue.add(0);
    listValue.add(0);
    last_put.setValue(listValue);
    List<Number> listDefaultValue = new ArrayList<Number>();
    listDefaultValue.add(0);
    listDefaultValue.add(0);
    listDefaultValue.add(0);
    last_put.setDefaultValue(listDefaultValue);
    last_put.setName("LAST_PUT");
    last_put.setDescription("LAST_PUT.description");
    last_put.setLocked(true);
    last_put.setLockedToDefault(false);
    RoleEnum[] roles;
    roles = new RoleEnum[1];
    roles[0] = RoleEnum.valueOf("User");
    last_put.setRoles(roles);
    last_put.setGroup(EnumGroup.valueOf("Reception"));
    last_put.setCategory(Category.valueOf("Acquisition"));
    this.addParam(last_put);
}

private void initMagnetic_field_strength(){
    NumberParam magnetic_field_strength = new NumberParam();
    magnetic_field_strength.setMinValue(-2147483648);
    magnetic_field_strength.setMaxValue(2147483647);
    magnetic_field_strength.setNumberEnum(NumberEnum.valueOf("Integer"));
    magnetic_field_strength.setDefaultValue(3);
    magnetic_field_strength.setValue(5);
    magnetic_field_strength.setRestrictedToSuggested(false);
    magnetic_field_strength.setName("MAGNETIC_FIELD_STRENGTH");
    magnetic_field_strength.setDescription("The magnetic field tregth");
    magnetic_field_strength.setLocked(true);
    magnetic_field_strength.setLockedToDefault(false);
    RoleEnum[] roles;
    roles = new RoleEnum[1];
    roles[0] = RoleEnum.valueOf("User");
    magnetic_field_strength.setRoles(roles);
    magnetic_field_strength.setGroup(EnumGroup.valueOf("Miscellaneous"));
    magnetic_field_strength.setCategory(Category.valueOf("Acquisition"));
    this.addParam(magnetic_field_strength);
}

private void initModality(){
    TextParam modality = new TextParam();
    modality.setDefaultValue("MRI");
    modality.setValue("MRI");
    List<String> list24 = new ArrayList<String>();
    list24.add("NMR");
    list24.add("MRI");
    list24.add("DEFAULT");
    modality.setSuggestedValues(list24);
    modality.setName("MODALITY");
    modality.setDescription("The modality for the acquisition");
    modality.setLocked(true);
    modality.setLockedToDefault(false);
    RoleEnum[] roles;
    roles = new RoleEnum[1];
    roles[0] = RoleEnum.valueOf("User");
    modality.setRoles(roles);
    modality.setGroup(EnumGroup.valueOf("Miscellaneous"));
    modality.setCategory(Category.valueOf("Acquisition"));
    this.addParam(modality);
}

private void initNucleus_1(){
    TextParam nucleus_1 = new TextParam();
    nucleus_1.setDefaultValue("1H");
    nucleus_1.setValue("1H");
    List<String> list25 = new ArrayList<String>();
    list25.add("Other");
    list25.add("Y");
    list25.add("X");
    list25.add("3H");
    list25.add("1H");
    list25.add("19F");
    list25.add("3He");
    list25.add("205Tl");
    list25.add("203Tl");
    list25.add("31P");
    list25.add("7Li");
    list25.add("119Sn");
    list25.add("117Sn");
    list25.add("87Rb");
    list25.add("115Sn");
    list25.add("11B");
    list25.add("125Te");
    list25.add("141Pr");
    list25.add("71Ga");
    list25.add("65Cu");
    list25.add("129Xe");
    list25.add("81Br");
    list25.add("63Cu");
    list25.add("23Na");
    list25.add("51V");
    list25.add("123Te");
    list25.add("27Al");
    list25.add("13C");
    list25.add("79Br");
    list25.add("151Eu");
    list25.add("55Mn");
    list25.add("93Nb");
    list25.add("45Sc");
    list25.add("159Tb");
    list25.add("69Ga");
    list25.add("121Sb");
    list25.add("59Co");
    list25.add("187Re");
    list25.add("185Re");
    list25.add("99Tc");
    list25.add("113Cd");
    list25.add("115In");
    list25.add("113In");
    list25.add("195Pt");
    list25.add("165Ho");
    list25.add("111Cd");
    list25.add("207Pb");
    list25.add("127I");
    list25.add("29Si");
    list25.add("77Se");
    list25.add("199Hg");
    list25.add("171Yb");
    list25.add("75As");
    list25.add("209Bi");
    list25.add("2H");
    list25.add("6Li");
    list25.add("139La");
    list25.add("9Be");
    list25.add("17O");
    list25.add("138La");
    list25.add("133Cs");
    list25.add("123Sb");
    list25.add("181Ta");
    list25.add("175Lu");
    list25.add("137Ba");
    list25.add("153Eu");
    list25.add("10B");
    list25.add("15N");
    list25.add("50V");
    list25.add("135Ba");
    list25.add("35Cl");
    list25.add("85Rb");
    list25.add("91Zr");
    list25.add("61Ni");
    list25.add("169Tm");
    list25.add("131Xe");
    list25.add("37Cl");
    list25.add("176Lu");
    list25.add("21Ne");
    list25.add("189Os");
    list25.add("33S");
    list25.add("14N");
    list25.add("43Ca");
    list25.add("97Mo");
    list25.add("201Hg");
    list25.add("95Mo");
    list25.add("67Zn");
    list25.add("25Mg");
    list25.add("40K");
    list25.add("53Cr");
    list25.add("49Ti");
    list25.add("47Ti");
    list25.add("143Nd");
    list25.add("101Ru");
    list25.add("89Y");
    list25.add("173Yb");
    list25.add("163Dy");
    list25.add("39K");
    list25.add("109Ag");
    list25.add("99Ru");
    list25.add("105Pd");
    list25.add("87Sr");
    list25.add("147Sm");
    list25.add("183W");
    list25.add("107Ag");
    list25.add("157Gd");
    list25.add("177Hf");
    list25.add("83Kr");
    list25.add("73Ge");
    list25.add("149Sm");
    list25.add("161Dy");
    list25.add("145Nd");
    list25.add("57Fe");
    list25.add("103Rh");
    list25.add("155Gd");
    list25.add("167Er");
    list25.add("41K");
    list25.add("179Hf");
    list25.add("187Os");
    list25.add("193Ir");
    list25.add("235U");
    list25.add("197Au");
    list25.add("191Ir");
    nucleus_1.setSuggestedValues(list25);
    nucleus_1.setName("NUCLEUS_1");
    nucleus_1.setDescription("The nucleus used for the first sequence channel");
    nucleus_1.setLocked(true);
    nucleus_1.setLockedToDefault(false);
    RoleEnum[] roles;
    roles = new RoleEnum[1];
    roles[0] = RoleEnum.valueOf("User");
    nucleus_1.setRoles(roles);
    nucleus_1.setGroup(EnumGroup.valueOf("Miscellaneous"));
    nucleus_1.setCategory(Category.valueOf("Acquisition"));
    this.addParam(nucleus_1);
}

private void initNucleus_2(){
    TextParam nucleus_2 = new TextParam();
    nucleus_2.setDefaultValue("1H");
    nucleus_2.setValue("1H");
    List<String> list26 = new ArrayList<String>();
    list26.add("Other");
    list26.add("Y");
    list26.add("X");
    list26.add("3H");
    list26.add("1H");
    list26.add("19F");
    list26.add("3He");
    list26.add("205Tl");
    list26.add("203Tl");
    list26.add("31P");
    list26.add("7Li");
    list26.add("119Sn");
    list26.add("117Sn");
    list26.add("87Rb");
    list26.add("115Sn");
    list26.add("11B");
    list26.add("125Te");
    list26.add("141Pr");
    list26.add("71Ga");
    list26.add("65Cu");
    list26.add("129Xe");
    list26.add("81Br");
    list26.add("63Cu");
    list26.add("23Na");
    list26.add("51V");
    list26.add("123Te");
    list26.add("27Al");
    list26.add("13C");
    list26.add("79Br");
    list26.add("151Eu");
    list26.add("55Mn");
    list26.add("93Nb");
    list26.add("45Sc");
    list26.add("159Tb");
    list26.add("69Ga");
    list26.add("121Sb");
    list26.add("59Co");
    list26.add("187Re");
    list26.add("185Re");
    list26.add("99Tc");
    list26.add("113Cd");
    list26.add("115In");
    list26.add("113In");
    list26.add("195Pt");
    list26.add("165Ho");
    list26.add("111Cd");
    list26.add("207Pb");
    list26.add("127I");
    list26.add("29Si");
    list26.add("77Se");
    list26.add("199Hg");
    list26.add("171Yb");
    list26.add("75As");
    list26.add("209Bi");
    list26.add("2H");
    list26.add("6Li");
    list26.add("139La");
    list26.add("9Be");
    list26.add("17O");
    list26.add("138La");
    list26.add("133Cs");
    list26.add("123Sb");
    list26.add("181Ta");
    list26.add("175Lu");
    list26.add("137Ba");
    list26.add("153Eu");
    list26.add("10B");
    list26.add("15N");
    list26.add("50V");
    list26.add("135Ba");
    list26.add("35Cl");
    list26.add("85Rb");
    list26.add("91Zr");
    list26.add("61Ni");
    list26.add("169Tm");
    list26.add("131Xe");
    list26.add("37Cl");
    list26.add("176Lu");
    list26.add("21Ne");
    list26.add("189Os");
    list26.add("33S");
    list26.add("14N");
    list26.add("43Ca");
    list26.add("97Mo");
    list26.add("201Hg");
    list26.add("95Mo");
    list26.add("67Zn");
    list26.add("25Mg");
    list26.add("40K");
    list26.add("53Cr");
    list26.add("49Ti");
    list26.add("47Ti");
    list26.add("143Nd");
    list26.add("101Ru");
    list26.add("89Y");
    list26.add("173Yb");
    list26.add("163Dy");
    list26.add("39K");
    list26.add("109Ag");
    list26.add("99Ru");
    list26.add("105Pd");
    list26.add("87Sr");
    list26.add("147Sm");
    list26.add("183W");
    list26.add("107Ag");
    list26.add("157Gd");
    list26.add("177Hf");
    list26.add("83Kr");
    list26.add("73Ge");
    list26.add("149Sm");
    list26.add("161Dy");
    list26.add("145Nd");
    list26.add("57Fe");
    list26.add("103Rh");
    list26.add("155Gd");
    list26.add("167Er");
    list26.add("41K");
    list26.add("179Hf");
    list26.add("187Os");
    list26.add("193Ir");
    list26.add("235U");
    list26.add("197Au");
    list26.add("191Ir");
    nucleus_2.setSuggestedValues(list26);
    nucleus_2.setName("NUCLEUS_2");
    nucleus_2.setDescription("The nucleus used for the second sequence channel");
    nucleus_2.setLocked(true);
    nucleus_2.setLockedToDefault(false);
    RoleEnum[] roles;
    roles = new RoleEnum[1];
    roles[0] = RoleEnum.valueOf("User");
    nucleus_2.setRoles(roles);
    nucleus_2.setGroup(EnumGroup.valueOf("Miscellaneous"));
    nucleus_2.setCategory(Category.valueOf("Acquisition"));
    this.addParam(nucleus_2);
}

private void initNucleus_3(){
    TextParam nucleus_3 = new TextParam();
    nucleus_3.setDefaultValue("1H");
    nucleus_3.setValue("1H");
    List<String> list27 = new ArrayList<String>();
    list27.add("Other");
    list27.add("Y");
    list27.add("X");
    list27.add("3H");
    list27.add("1H");
    list27.add("19F");
    list27.add("3He");
    list27.add("205Tl");
    list27.add("203Tl");
    list27.add("31P");
    list27.add("7Li");
    list27.add("119Sn");
    list27.add("117Sn");
    list27.add("87Rb");
    list27.add("115Sn");
    list27.add("11B");
    list27.add("125Te");
    list27.add("141Pr");
    list27.add("71Ga");
    list27.add("65Cu");
    list27.add("129Xe");
    list27.add("81Br");
    list27.add("63Cu");
    list27.add("23Na");
    list27.add("51V");
    list27.add("123Te");
    list27.add("27Al");
    list27.add("13C");
    list27.add("79Br");
    list27.add("151Eu");
    list27.add("55Mn");
    list27.add("93Nb");
    list27.add("45Sc");
    list27.add("159Tb");
    list27.add("69Ga");
    list27.add("121Sb");
    list27.add("59Co");
    list27.add("187Re");
    list27.add("185Re");
    list27.add("99Tc");
    list27.add("113Cd");
    list27.add("115In");
    list27.add("113In");
    list27.add("195Pt");
    list27.add("165Ho");
    list27.add("111Cd");
    list27.add("207Pb");
    list27.add("127I");
    list27.add("29Si");
    list27.add("77Se");
    list27.add("199Hg");
    list27.add("171Yb");
    list27.add("75As");
    list27.add("209Bi");
    list27.add("2H");
    list27.add("6Li");
    list27.add("139La");
    list27.add("9Be");
    list27.add("17O");
    list27.add("138La");
    list27.add("133Cs");
    list27.add("123Sb");
    list27.add("181Ta");
    list27.add("175Lu");
    list27.add("137Ba");
    list27.add("153Eu");
    list27.add("10B");
    list27.add("15N");
    list27.add("50V");
    list27.add("135Ba");
    list27.add("35Cl");
    list27.add("85Rb");
    list27.add("91Zr");
    list27.add("61Ni");
    list27.add("169Tm");
    list27.add("131Xe");
    list27.add("37Cl");
    list27.add("176Lu");
    list27.add("21Ne");
    list27.add("189Os");
    list27.add("33S");
    list27.add("14N");
    list27.add("43Ca");
    list27.add("97Mo");
    list27.add("201Hg");
    list27.add("95Mo");
    list27.add("67Zn");
    list27.add("25Mg");
    list27.add("40K");
    list27.add("53Cr");
    list27.add("49Ti");
    list27.add("47Ti");
    list27.add("143Nd");
    list27.add("101Ru");
    list27.add("89Y");
    list27.add("173Yb");
    list27.add("163Dy");
    list27.add("39K");
    list27.add("109Ag");
    list27.add("99Ru");
    list27.add("105Pd");
    list27.add("87Sr");
    list27.add("147Sm");
    list27.add("183W");
    list27.add("107Ag");
    list27.add("157Gd");
    list27.add("177Hf");
    list27.add("83Kr");
    list27.add("73Ge");
    list27.add("149Sm");
    list27.add("161Dy");
    list27.add("145Nd");
    list27.add("57Fe");
    list27.add("103Rh");
    list27.add("155Gd");
    list27.add("167Er");
    list27.add("41K");
    list27.add("179Hf");
    list27.add("187Os");
    list27.add("193Ir");
    list27.add("235U");
    list27.add("197Au");
    list27.add("191Ir");
    nucleus_3.setSuggestedValues(list27);
    nucleus_3.setName("NUCLEUS_3");
    nucleus_3.setDescription("The nucleus used for the third sequence channel");
    nucleus_3.setLocked(true);
    nucleus_3.setLockedToDefault(false);
    RoleEnum[] roles;
    roles = new RoleEnum[1];
    roles[0] = RoleEnum.valueOf("User");
    nucleus_3.setRoles(roles);
    nucleus_3.setGroup(EnumGroup.valueOf("Miscellaneous"));
    nucleus_3.setCategory(Category.valueOf("Acquisition"));
    this.addParam(nucleus_3);
}

private void initNucleus_4(){
    TextParam nucleus_4 = new TextParam();
    nucleus_4.setDefaultValue("1H");
    nucleus_4.setValue("1H");
    List<String> list28 = new ArrayList<String>();
    list28.add("Other");
    list28.add("Y");
    list28.add("X");
    list28.add("3H");
    list28.add("1H");
    list28.add("19F");
    list28.add("3He");
    list28.add("205Tl");
    list28.add("203Tl");
    list28.add("31P");
    list28.add("7Li");
    list28.add("119Sn");
    list28.add("117Sn");
    list28.add("87Rb");
    list28.add("115Sn");
    list28.add("11B");
    list28.add("125Te");
    list28.add("141Pr");
    list28.add("71Ga");
    list28.add("65Cu");
    list28.add("129Xe");
    list28.add("81Br");
    list28.add("63Cu");
    list28.add("23Na");
    list28.add("51V");
    list28.add("123Te");
    list28.add("27Al");
    list28.add("13C");
    list28.add("79Br");
    list28.add("151Eu");
    list28.add("55Mn");
    list28.add("93Nb");
    list28.add("45Sc");
    list28.add("159Tb");
    list28.add("69Ga");
    list28.add("121Sb");
    list28.add("59Co");
    list28.add("187Re");
    list28.add("185Re");
    list28.add("99Tc");
    list28.add("113Cd");
    list28.add("115In");
    list28.add("113In");
    list28.add("195Pt");
    list28.add("165Ho");
    list28.add("111Cd");
    list28.add("207Pb");
    list28.add("127I");
    list28.add("29Si");
    list28.add("77Se");
    list28.add("199Hg");
    list28.add("171Yb");
    list28.add("75As");
    list28.add("209Bi");
    list28.add("2H");
    list28.add("6Li");
    list28.add("139La");
    list28.add("9Be");
    list28.add("17O");
    list28.add("138La");
    list28.add("133Cs");
    list28.add("123Sb");
    list28.add("181Ta");
    list28.add("175Lu");
    list28.add("137Ba");
    list28.add("153Eu");
    list28.add("10B");
    list28.add("15N");
    list28.add("50V");
    list28.add("135Ba");
    list28.add("35Cl");
    list28.add("85Rb");
    list28.add("91Zr");
    list28.add("61Ni");
    list28.add("169Tm");
    list28.add("131Xe");
    list28.add("37Cl");
    list28.add("176Lu");
    list28.add("21Ne");
    list28.add("189Os");
    list28.add("33S");
    list28.add("14N");
    list28.add("43Ca");
    list28.add("97Mo");
    list28.add("201Hg");
    list28.add("95Mo");
    list28.add("67Zn");
    list28.add("25Mg");
    list28.add("40K");
    list28.add("53Cr");
    list28.add("49Ti");
    list28.add("47Ti");
    list28.add("143Nd");
    list28.add("101Ru");
    list28.add("89Y");
    list28.add("173Yb");
    list28.add("163Dy");
    list28.add("39K");
    list28.add("109Ag");
    list28.add("99Ru");
    list28.add("105Pd");
    list28.add("87Sr");
    list28.add("147Sm");
    list28.add("183W");
    list28.add("107Ag");
    list28.add("157Gd");
    list28.add("177Hf");
    list28.add("83Kr");
    list28.add("73Ge");
    list28.add("149Sm");
    list28.add("161Dy");
    list28.add("145Nd");
    list28.add("57Fe");
    list28.add("103Rh");
    list28.add("155Gd");
    list28.add("167Er");
    list28.add("41K");
    list28.add("179Hf");
    list28.add("187Os");
    list28.add("193Ir");
    list28.add("235U");
    list28.add("197Au");
    list28.add("191Ir");
    nucleus_4.setSuggestedValues(list28);
    nucleus_4.setName("NUCLEUS_4");
    nucleus_4.setDescription("The nucleus used for the fourth sequence channel");
    nucleus_4.setLocked(true);
    nucleus_4.setLockedToDefault(false);
    RoleEnum[] roles;
    roles = new RoleEnum[1];
    roles[0] = RoleEnum.valueOf("User");
    nucleus_4.setRoles(roles);
    nucleus_4.setGroup(EnumGroup.valueOf("Miscellaneous"));
    nucleus_4.setCategory(Category.valueOf("Acquisition"));
    this.addParam(nucleus_4);
}

private void initNumber_of_averages(){
    NumberParam number_of_averages = new NumberParam();
    number_of_averages.setMinValue(0);
    number_of_averages.setMaxValue(65536);
    number_of_averages.setNumberEnum(NumberEnum.valueOf("Scan"));
    number_of_averages.setDefaultValue(1);
    number_of_averages.setValue(1);
    number_of_averages.setRestrictedToSuggested(false);
    number_of_averages.setName("NUMBER_OF_AVERAGES");
    number_of_averages.setDescription("The number of average");
    number_of_averages.setLocked(false);
    number_of_averages.setLockedToDefault(false);
    RoleEnum[] roles;
    roles = new RoleEnum[1];
    roles[0] = RoleEnum.valueOf("User");
    number_of_averages.setRoles(roles);
    number_of_averages.setGroup(EnumGroup.valueOf("Scan"));
    number_of_averages.setCategory(Category.valueOf("Acquisition"));
    this.addParam(number_of_averages);
}

private void initObserved_frequency(){
    NumberParam observed_frequency = new NumberParam();
    observed_frequency.setMinValue(0.0);
    observed_frequency.setMaxValue(3.0E9);
    observed_frequency.setNumberEnum(NumberEnum.valueOf("Frequency"));
    observed_frequency.setDefaultValue(6.3E7);
    observed_frequency.setValue(1.274981876E8);
    observed_frequency.setRestrictedToSuggested(false);
    observed_frequency.setName("OBSERVED_FREQUENCY");
    observed_frequency.setDescription("The frequency of the acquisition");
    observed_frequency.setLocked(true);
    observed_frequency.setLockedToDefault(false);
    RoleEnum[] roles;
    roles = new RoleEnum[1];
    roles[0] = RoleEnum.valueOf("User");
    observed_frequency.setRoles(roles);
    observed_frequency.setGroup(EnumGroup.valueOf("Reception"));
    observed_frequency.setCategory(Category.valueOf("Acquisition"));
    this.addParam(observed_frequency);
}

private void initObserved_nucleus(){
    TextParam observed_nucleus = new TextParam();
    observed_nucleus.setDefaultValue("1H");
    observed_nucleus.setValue("1H");
    List<String> list31 = new ArrayList<String>();
    list31.add("Other");
    list31.add("Y");
    list31.add("X");
    list31.add("3H");
    list31.add("1H");
    list31.add("19F");
    list31.add("3He");
    list31.add("205Tl");
    list31.add("203Tl");
    list31.add("31P");
    list31.add("7Li");
    list31.add("119Sn");
    list31.add("117Sn");
    list31.add("87Rb");
    list31.add("115Sn");
    list31.add("11B");
    list31.add("125Te");
    list31.add("141Pr");
    list31.add("71Ga");
    list31.add("65Cu");
    list31.add("129Xe");
    list31.add("81Br");
    list31.add("63Cu");
    list31.add("23Na");
    list31.add("51V");
    list31.add("123Te");
    list31.add("27Al");
    list31.add("13C");
    list31.add("79Br");
    list31.add("151Eu");
    list31.add("55Mn");
    list31.add("93Nb");
    list31.add("45Sc");
    list31.add("159Tb");
    list31.add("69Ga");
    list31.add("121Sb");
    list31.add("59Co");
    list31.add("187Re");
    list31.add("185Re");
    list31.add("99Tc");
    list31.add("113Cd");
    list31.add("115In");
    list31.add("113In");
    list31.add("195Pt");
    list31.add("165Ho");
    list31.add("111Cd");
    list31.add("207Pb");
    list31.add("127I");
    list31.add("29Si");
    list31.add("77Se");
    list31.add("199Hg");
    list31.add("171Yb");
    list31.add("75As");
    list31.add("209Bi");
    list31.add("2H");
    list31.add("6Li");
    list31.add("139La");
    list31.add("9Be");
    list31.add("17O");
    list31.add("138La");
    list31.add("133Cs");
    list31.add("123Sb");
    list31.add("181Ta");
    list31.add("175Lu");
    list31.add("137Ba");
    list31.add("153Eu");
    list31.add("10B");
    list31.add("15N");
    list31.add("50V");
    list31.add("135Ba");
    list31.add("35Cl");
    list31.add("85Rb");
    list31.add("91Zr");
    list31.add("61Ni");
    list31.add("169Tm");
    list31.add("131Xe");
    list31.add("37Cl");
    list31.add("176Lu");
    list31.add("21Ne");
    list31.add("189Os");
    list31.add("33S");
    list31.add("14N");
    list31.add("43Ca");
    list31.add("97Mo");
    list31.add("201Hg");
    list31.add("95Mo");
    list31.add("67Zn");
    list31.add("25Mg");
    list31.add("40K");
    list31.add("53Cr");
    list31.add("49Ti");
    list31.add("47Ti");
    list31.add("143Nd");
    list31.add("101Ru");
    list31.add("89Y");
    list31.add("173Yb");
    list31.add("163Dy");
    list31.add("39K");
    list31.add("109Ag");
    list31.add("99Ru");
    list31.add("105Pd");
    list31.add("87Sr");
    list31.add("147Sm");
    list31.add("183W");
    list31.add("107Ag");
    list31.add("157Gd");
    list31.add("177Hf");
    list31.add("83Kr");
    list31.add("73Ge");
    list31.add("149Sm");
    list31.add("161Dy");
    list31.add("145Nd");
    list31.add("57Fe");
    list31.add("103Rh");
    list31.add("155Gd");
    list31.add("167Er");
    list31.add("41K");
    list31.add("179Hf");
    list31.add("187Os");
    list31.add("193Ir");
    list31.add("235U");
    list31.add("197Au");
    list31.add("191Ir");
    observed_nucleus.setSuggestedValues(list31);
    observed_nucleus.setName("OBSERVED_NUCLEUS");
    observed_nucleus.setDescription("The observed nucleus");
    observed_nucleus.setLocked(true);
    observed_nucleus.setLockedToDefault(false);
    RoleEnum[] roles;
    roles = new RoleEnum[1];
    roles[0] = RoleEnum.valueOf("User");
    observed_nucleus.setRoles(roles);
    observed_nucleus.setGroup(EnumGroup.valueOf("Reception"));
    observed_nucleus.setCategory(Category.valueOf("Acquisition"));
    this.addParam(observed_nucleus);
}

private void initOffset_freq_1(){
    NumberParam offset_freq_1 = new NumberParam();
    offset_freq_1.setMinValue(-1.5E9);
    offset_freq_1.setMaxValue(1.5E9);
    offset_freq_1.setNumberEnum(NumberEnum.valueOf("FrequencyOffset"));
    offset_freq_1.setDefaultValue(0.0);
    offset_freq_1.setValue(0.0);
    offset_freq_1.setRestrictedToSuggested(false);
    offset_freq_1.setName("OFFSET_FREQ_1");
    offset_freq_1.setDescription("The offset frequency of the first sequence channel");
    offset_freq_1.setLocked(true);
    offset_freq_1.setLockedToDefault(false);
    RoleEnum[] roles;
    roles = new RoleEnum[1];
    roles[0] = RoleEnum.valueOf("User");
    offset_freq_1.setRoles(roles);
    offset_freq_1.setGroup(EnumGroup.valueOf("Emission"));
    offset_freq_1.setCategory(Category.valueOf("Acquisition"));
    this.addParam(offset_freq_1);
}

private void initOffset_freq_2(){
    NumberParam offset_freq_2 = new NumberParam();
    offset_freq_2.setMinValue(-1.5E9);
    offset_freq_2.setMaxValue(1.5E9);
    offset_freq_2.setNumberEnum(NumberEnum.valueOf("FrequencyOffset"));
    offset_freq_2.setDefaultValue(0.0);
    offset_freq_2.setValue(0.0);
    offset_freq_2.setRestrictedToSuggested(false);
    offset_freq_2.setName("OFFSET_FREQ_2");
    offset_freq_2.setDescription("The offset frequency of the second sequence channel");
    offset_freq_2.setLocked(true);
    offset_freq_2.setLockedToDefault(false);
    RoleEnum[] roles;
    roles = new RoleEnum[1];
    roles[0] = RoleEnum.valueOf("User");
    offset_freq_2.setRoles(roles);
    offset_freq_2.setGroup(EnumGroup.valueOf("Emission"));
    offset_freq_2.setCategory(Category.valueOf("Acquisition"));
    this.addParam(offset_freq_2);
}

private void initOffset_freq_3(){
    NumberParam offset_freq_3 = new NumberParam();
    offset_freq_3.setMinValue(-1.5E9);
    offset_freq_3.setMaxValue(1.5E9);
    offset_freq_3.setNumberEnum(NumberEnum.valueOf("FrequencyOffset"));
    offset_freq_3.setDefaultValue(0.0);
    offset_freq_3.setValue(0.0);
    offset_freq_3.setRestrictedToSuggested(false);
    offset_freq_3.setName("OFFSET_FREQ_3");
    offset_freq_3.setDescription("The offset frequency of the third sequence channel");
    offset_freq_3.setLocked(true);
    offset_freq_3.setLockedToDefault(false);
    RoleEnum[] roles;
    roles = new RoleEnum[1];
    roles[0] = RoleEnum.valueOf("User");
    offset_freq_3.setRoles(roles);
    offset_freq_3.setGroup(EnumGroup.valueOf("Emission"));
    offset_freq_3.setCategory(Category.valueOf("Acquisition"));
    this.addParam(offset_freq_3);
}

private void initOffset_freq_4(){
    NumberParam offset_freq_4 = new NumberParam();
    offset_freq_4.setMinValue(-1.5E9);
    offset_freq_4.setMaxValue(1.5E9);
    offset_freq_4.setNumberEnum(NumberEnum.valueOf("FrequencyOffset"));
    offset_freq_4.setDefaultValue(0.0);
    offset_freq_4.setValue(0.0);
    offset_freq_4.setRestrictedToSuggested(false);
    offset_freq_4.setName("OFFSET_FREQ_4");
    offset_freq_4.setDescription("The offset frequency of the fourth sequence channel");
    offset_freq_4.setLocked(true);
    offset_freq_4.setLockedToDefault(false);
    RoleEnum[] roles;
    roles = new RoleEnum[1];
    roles[0] = RoleEnum.valueOf("User");
    offset_freq_4.setRoles(roles);
    offset_freq_4.setGroup(EnumGroup.valueOf("Emission"));
    offset_freq_4.setCategory(Category.valueOf("Acquisition"));
    this.addParam(offset_freq_4);
}

private void initOrientation(){
    TextParam orientation = new TextParam();
    orientation.setDefaultValue("AXIAL");
    orientation.setValue("AXIAL");
    List<String> list36 = new ArrayList<String>();
    list36.add("AXIAL");
    list36.add("CORONAL");
    list36.add("SAGITTAL");
    list36.add("OBLIQUE");
    orientation.setSuggestedValues(list36);
    orientation.setName("ORIENTATION");
    orientation.setDescription("Field of view orientation");
    orientation.setLocked(true);
    orientation.setLockedToDefault(false);
    RoleEnum[] roles;
    roles = new RoleEnum[1];
    roles[0] = RoleEnum.valueOf("User");
    orientation.setRoles(roles);
    orientation.setGroup(EnumGroup.valueOf("Dimension"));
    orientation.setCategory(Category.valueOf("Acquisition"));
    this.addParam(orientation);
}

private void initParopt_param(){
    TextParam paropt_param = new TextParam();
    paropt_param.setDefaultValue("PULSE_LENGTH");
    paropt_param.setValue("");
    paropt_param.setName("PAROPT_PARAM");
    paropt_param.setDescription("Name of the current optimised parameter");
    paropt_param.setLocked(false);
    paropt_param.setLockedToDefault(false);
    RoleEnum[] roles;
    roles = new RoleEnum[1];
    roles[0] = RoleEnum.valueOf("User");
    paropt_param.setRoles(roles);
    paropt_param.setGroup(EnumGroup.valueOf("Miscellaneous"));
    paropt_param.setCategory(Category.valueOf("Acquisition"));
    this.addParam(paropt_param);
}

private void initProbe(){
    TextParam probe = new TextParam();
    probe.setDefaultValue("");
    probe.setValue("");
    probe.setName("PROBE");
    probe.setDescription("The probe used for the mr acquisition");
    probe.setLocked(true);
    probe.setLockedToDefault(false);
    RoleEnum[] roles;
    roles = new RoleEnum[1];
    roles[0] = RoleEnum.valueOf("User");
    probe.setRoles(roles);
    probe.setGroup(EnumGroup.valueOf("Miscellaneous"));
    probe.setCategory(Category.valueOf("Acquisition"));
    this.addParam(probe);
}

private void initPulse_att(){
    NumberParam pulse_att = new NumberParam();
    pulse_att.setMinValue(0.0);
    pulse_att.setMaxValue(63.0);
    pulse_att.setNumberEnum(NumberEnum.valueOf("TxAtt"));
    pulse_att.setDefaultValue(0.0);
    pulse_att.setValue(35.0);
    pulse_att.setRestrictedToSuggested(false);
    pulse_att.setName("PULSE_ATT");
    pulse_att.setDescription("");
    pulse_att.setLocked(true);
    pulse_att.setLockedToDefault(false);
    RoleEnum[] roles;
    roles = new RoleEnum[1];
    roles[0] = RoleEnum.valueOf("User");
    pulse_att.setRoles(roles);
    pulse_att.setGroup(EnumGroup.valueOf("Emission"));
    pulse_att.setCategory(Category.valueOf("Acquisition"));
    this.addParam(pulse_att);
}

private void initReceiver_count(){
    NumberParam receiver_count = new NumberParam();
    receiver_count.setMinValue(1);
    receiver_count.setMaxValue(32);
    receiver_count.setNumberEnum(NumberEnum.valueOf("Integer"));
    receiver_count.setDefaultValue(1);
    receiver_count.setValue(1);
    receiver_count.setRestrictedToSuggested(false);
    receiver_count.setName("RECEIVER_COUNT");
    receiver_count.setDescription("The number of receivers");
    receiver_count.setLocked(true);
    receiver_count.setLockedToDefault(false);
    RoleEnum[] roles;
    roles = new RoleEnum[1];
    roles[0] = RoleEnum.valueOf("User");
    receiver_count.setRoles(roles);
    receiver_count.setGroup(EnumGroup.valueOf("Reception"));
    receiver_count.setCategory(Category.valueOf("Acquisition"));
    this.addParam(receiver_count);
}

private void initReceiver_gain(){
    NumberParam receiver_gain = new NumberParam();
    receiver_gain.setMinValue(0.0);
    receiver_gain.setMaxValue(120.0);
    receiver_gain.setNumberEnum(NumberEnum.valueOf("RxGain"));
    receiver_gain.setDefaultValue(1.0);
    receiver_gain.setValue(1.0);
    receiver_gain.setRestrictedToSuggested(false);
    receiver_gain.setName("RECEIVER_GAIN");
    receiver_gain.setDescription("The receiver gain");
    receiver_gain.setLocked(false);
    receiver_gain.setLockedToDefault(false);
    RoleEnum[] roles;
    roles = new RoleEnum[1];
    roles[0] = RoleEnum.valueOf("User");
    receiver_gain.setRoles(roles);
    receiver_gain.setGroup(EnumGroup.valueOf("Reception"));
    receiver_gain.setCategory(Category.valueOf("Acquisition"));
    this.addParam(receiver_gain);
}

private void initRepetition_time(){
    NumberParam repetition_time = new NumberParam();
    repetition_time.setMinValue(0.0);
    repetition_time.setMaxValue(1.0E9);
    repetition_time.setNumberEnum(NumberEnum.valueOf("Time"));
    repetition_time.setDefaultValue(0.2);
    repetition_time.setValue(1.0);
    repetition_time.setRestrictedToSuggested(false);
    repetition_time.setName("REPETITION_TIME");
    repetition_time.setDescription("The repetition time");
    repetition_time.setLocked(false);
    repetition_time.setLockedToDefault(false);
    RoleEnum[] roles;
    roles = new RoleEnum[1];
    roles[0] = RoleEnum.valueOf("User");
    repetition_time.setRoles(roles);
    repetition_time.setGroup(EnumGroup.valueOf("Delay"));
    repetition_time.setCategory(Category.valueOf("Acquisition"));
    this.addParam(repetition_time);
}

private void initSequence_name(){
    TextParam sequence_name = new TextParam();
    sequence_name.setDefaultValue("GradShims_dev");
    sequence_name.setValue("GradShims_dev");
    sequence_name.setName("SEQUENCE_NAME");
    sequence_name.setDescription("The name of the sequence");
    sequence_name.setLocked(true);
    sequence_name.setLockedToDefault(true);
    RoleEnum[] roles;
    roles = new RoleEnum[1];
    roles[0] = RoleEnum.valueOf("User");
    sequence_name.setRoles(roles);
    sequence_name.setGroup(EnumGroup.valueOf("Miscellaneous"));
    sequence_name.setCategory(Category.valueOf("Acquisition"));
    this.addParam(sequence_name);
}

private void initSetup_mode(){
    BooleanParam setup_mode = new BooleanParam();
    setup_mode.setDefaultValue(false);
    setup_mode.setValue(false);
    setup_mode.setName("SETUP_MODE");
    setup_mode.setDescription("True during setup process");
    setup_mode.setLocked(true);
    setup_mode.setLockedToDefault(false);
    RoleEnum[] roles;
    roles = new RoleEnum[1];
    roles[0] = RoleEnum.valueOf("User");
    setup_mode.setRoles(roles);
    setup_mode.setGroup(EnumGroup.valueOf("Miscellaneous"));
    setup_mode.setCategory(Category.valueOf("Acquisition"));
    this.addParam(setup_mode);
}

private void initShimmed_gradient(){
    TextParam shimmed_gradient = new TextParam();
    shimmed_gradient.setDefaultValue("X");
    shimmed_gradient.setValue("X");
    List<String> list45 = new ArrayList<String>();
    list45.add("X");
    list45.add("Y");
    list45.add("Z");
    shimmed_gradient.setSuggestedValues(list45);
    shimmed_gradient.setName("Shimmed_Gradient");
    shimmed_gradient.setDescription("The gradient to shimmed");
    shimmed_gradient.setLocked(false);
    shimmed_gradient.setLockedToDefault(false);
    RoleEnum[] roles;
    roles = new RoleEnum[1];
    roles[0] = RoleEnum.valueOf("User");
    shimmed_gradient.setRoles(roles);
    shimmed_gradient.setGroup(EnumGroup.valueOf("Gradient"));
    shimmed_gradient.setCategory(Category.valueOf("Acquisition"));
    this.addParam(shimmed_gradient);
}

private void initSlice_thickness(){
    NumberParam slice_thickness = new NumberParam();
    slice_thickness.setMinValue(5.0E-5);
    slice_thickness.setMaxValue(1.7976931348623157E308);
    slice_thickness.setNumberEnum(NumberEnum.valueOf("Length"));
    slice_thickness.setDefaultValue(0.005);
    slice_thickness.setValue(0.005);
    slice_thickness.setRestrictedToSuggested(false);
    slice_thickness.setName("SLICE_THICKNESS");
    slice_thickness.setDescription("The slice thickness");
    slice_thickness.setLocked(true);
    slice_thickness.setLockedToDefault(false);
    RoleEnum[] roles;
    roles = new RoleEnum[1];
    roles[0] = RoleEnum.valueOf("User");
    slice_thickness.setRoles(roles);
    slice_thickness.setGroup(EnumGroup.valueOf("Dimension"));
    slice_thickness.setCategory(Category.valueOf("Acquisition"));
    this.addParam(slice_thickness);
}

private void initSpecimen_position(){
    TextParam specimen_position = new TextParam();
    specimen_position.setDefaultValue("FeetFirstProne");
    specimen_position.setValue("FeetFirstProne");
    List<String> list47 = new ArrayList<String>();
    list47.add("HeadFirstProne");
    list47.add("HeadFirstSupine");
    list47.add("FeetFirstProne");
    list47.add("FeetFirstSupine");
    specimen_position.setSuggestedValues(list47);
    specimen_position.setName("SPECIMEN_POSITION");
    specimen_position.setDescription("Specimen position descriptor relative to the magnet.");
    specimen_position.setLocked(true);
    specimen_position.setLockedToDefault(false);
    RoleEnum[] roles;
    roles = new RoleEnum[1];
    roles[0] = RoleEnum.valueOf("User");
    specimen_position.setRoles(roles);
    specimen_position.setGroup(EnumGroup.valueOf("Dimension"));
    specimen_position.setCategory(Category.valueOf("Acquisition"));
    this.addParam(specimen_position);
}

private void initSpectral_width(){
    NumberParam spectral_width = new NumberParam();
    spectral_width.setMinValue(0.0);
    spectral_width.setMaxValue(1.0E8);
    spectral_width.setNumberEnum(NumberEnum.valueOf("SW"));
    spectral_width.setDefaultValue(12500.0);
    spectral_width.setValue(32000.0);
    spectral_width.setRestrictedToSuggested(false);
    spectral_width.setName("SPECTRAL_WIDTH");
    spectral_width.setDescription("The spectral width of the reception");
    spectral_width.setLocked(false);
    spectral_width.setLockedToDefault(false);
    RoleEnum[] roles;
    roles = new RoleEnum[1];
    roles[0] = RoleEnum.valueOf("User");
    spectral_width.setRoles(roles);
    spectral_width.setGroup(EnumGroup.valueOf("Reception"));
    spectral_width.setCategory(Category.valueOf("Acquisition"));
    this.addParam(spectral_width);
}

private void initTransform_plugin(){
    TextParam transform_plugin = new TextParam();
    transform_plugin.setDefaultValue("none");
    transform_plugin.setValue("none");
    List<String> list49 = new ArrayList<String>();
    list49.add("Bordered2D");
    list49.add("Centered2D");
    list49.add("Sequential2D");
    list49.add("Sequential4D");
    transform_plugin.setSuggestedValues(list49);
    transform_plugin.setName("TRANSFORM_PLUGIN");
    transform_plugin.setDescription("Transform the acquisition space to the k space");
    transform_plugin.setLocked(true);
    transform_plugin.setLockedToDefault(false);
    RoleEnum[] roles;
    roles = new RoleEnum[1];
    roles[0] = RoleEnum.valueOf("User");
    transform_plugin.setRoles(roles);
    transform_plugin.setGroup(EnumGroup.valueOf("Scan"));
    transform_plugin.setCategory(Category.valueOf("Acquisition"));
    this.addParam(transform_plugin);
}

private void initTx_amp_90(){
    NumberParam tx_amp_90 = new NumberParam();
    tx_amp_90.setMinValue(0.0);
    tx_amp_90.setMaxValue(100.0);
    tx_amp_90.setNumberEnum(NumberEnum.valueOf("TxAmp"));
    tx_amp_90.setDefaultValue(0.0);
    tx_amp_90.setValue(44.04176647796125);
    tx_amp_90.setRestrictedToSuggested(false);
    tx_amp_90.setName("TX_AMP_90");
    tx_amp_90.setDescription("");
    tx_amp_90.setLocked(true);
    tx_amp_90.setLockedToDefault(false);
    RoleEnum[] roles;
    roles = new RoleEnum[1];
    roles[0] = RoleEnum.valueOf("User");
    tx_amp_90.setRoles(roles);
    tx_amp_90.setGroup(EnumGroup.valueOf("Emission"));
    tx_amp_90.setCategory(Category.valueOf("Acquisition"));
    this.addParam(tx_amp_90);
}

private void initTx_length_90(){
    NumberParam tx_length_90 = new NumberParam();
    tx_length_90.setMinValue(1.0000000000000001E-7);
    tx_length_90.setMaxValue(1.0E9);
    tx_length_90.setNumberEnum(NumberEnum.valueOf("Time"));
    tx_length_90.setDefaultValue(0.001);
    tx_length_90.setValue(0.001);
    tx_length_90.setRestrictedToSuggested(false);
    tx_length_90.setName("TX_LENGTH_90");
    tx_length_90.setDescription("");
    tx_length_90.setLocked(true);
    tx_length_90.setLockedToDefault(false);
    RoleEnum[] roles;
    roles = new RoleEnum[1];
    roles[0] = RoleEnum.valueOf("User");
    tx_length_90.setRoles(roles);
    tx_length_90.setGroup(EnumGroup.valueOf("Emission"));
    tx_length_90.setCategory(Category.valueOf("Acquisition"));
    this.addParam(tx_length_90);
}

private void initTx_route(){
    ListNumberParam tx_route = new ListNumberParam();
    tx_route.setMinValue(-2147483648);
    tx_route.setMaxValue(2147483647);
    tx_route.setNumberEnum(NumberEnum.valueOf("Integer"));
    List<Number> listValue = new ArrayList<Number>();
    listValue.add(0);
    tx_route.setValue(listValue);
    tx_route.setName("TX_ROUTE");
    tx_route.setDescription("LogCh->PhysCh");
    tx_route.setLocked(true);
    tx_route.setLockedToDefault(false);
    RoleEnum[] roles;
    roles = new RoleEnum[1];
    roles[0] = RoleEnum.valueOf("User");
    tx_route.setRoles(roles);
    tx_route.setGroup(EnumGroup.valueOf("Emission"));
    tx_route.setCategory(Category.valueOf("Acquisition"));
    this.addParam(tx_route);
}

private void initUser_matrix_dimension_1d(){
    NumberParam user_matrix_dimension_1d = new NumberParam();
    user_matrix_dimension_1d.setMinValue(0);
    user_matrix_dimension_1d.setMaxValue(65536);
    user_matrix_dimension_1d.setNumberEnum(NumberEnum.valueOf("Scan"));
    user_matrix_dimension_1d.setDefaultValue(128);
    user_matrix_dimension_1d.setValue(256);
    user_matrix_dimension_1d.setRestrictedToSuggested(false);
    user_matrix_dimension_1d.setName("USER_MATRIX_DIMENSION_1D");
    user_matrix_dimension_1d.setDescription("The matrix size of the first dimension");
    user_matrix_dimension_1d.setLocked(false);
    user_matrix_dimension_1d.setLockedToDefault(false);
    RoleEnum[] roles;
    roles = new RoleEnum[1];
    roles[0] = RoleEnum.valueOf("User");
    user_matrix_dimension_1d.setRoles(roles);
    user_matrix_dimension_1d.setGroup(EnumGroup.valueOf("Scan"));
    user_matrix_dimension_1d.setCategory(Category.valueOf("Acquisition"));
    this.addParam(user_matrix_dimension_1d);
}

private void initUser_matrix_dimension_2d(){
    NumberParam user_matrix_dimension_2d = new NumberParam();
    user_matrix_dimension_2d.setMinValue(0);
    user_matrix_dimension_2d.setMaxValue(65536);
    user_matrix_dimension_2d.setNumberEnum(NumberEnum.valueOf("Scan"));
    user_matrix_dimension_2d.setDefaultValue(128);
    user_matrix_dimension_2d.setValue(2);
    user_matrix_dimension_2d.setRestrictedToSuggested(false);
    user_matrix_dimension_2d.setName("USER_MATRIX_DIMENSION_2D");
    user_matrix_dimension_2d.setDescription("The matrix size of the second dimension");
    user_matrix_dimension_2d.setLocked(true);
    user_matrix_dimension_2d.setLockedToDefault(false);
    RoleEnum[] roles;
    roles = new RoleEnum[1];
    roles[0] = RoleEnum.valueOf("User");
    user_matrix_dimension_2d.setRoles(roles);
    user_matrix_dimension_2d.setGroup(EnumGroup.valueOf("Scan"));
    user_matrix_dimension_2d.setCategory(Category.valueOf("Acquisition"));
    this.addParam(user_matrix_dimension_2d);
}

private void initUser_matrix_dimension_3d(){
    NumberParam user_matrix_dimension_3d = new NumberParam();
    user_matrix_dimension_3d.setMinValue(0);
    user_matrix_dimension_3d.setMaxValue(65536);
    user_matrix_dimension_3d.setNumberEnum(NumberEnum.valueOf("Scan"));
    user_matrix_dimension_3d.setDefaultValue(1);
    user_matrix_dimension_3d.setValue(1);
    user_matrix_dimension_3d.setRestrictedToSuggested(false);
    user_matrix_dimension_3d.setName("USER_MATRIX_DIMENSION_3D");
    user_matrix_dimension_3d.setDescription("The matrix size of the third dimension");
    user_matrix_dimension_3d.setLocked(true);
    user_matrix_dimension_3d.setLockedToDefault(false);
    RoleEnum[] roles;
    roles = new RoleEnum[1];
    roles[0] = RoleEnum.valueOf("User");
    user_matrix_dimension_3d.setRoles(roles);
    user_matrix_dimension_3d.setGroup(EnumGroup.valueOf("Scan"));
    user_matrix_dimension_3d.setCategory(Category.valueOf("Acquisition"));
    this.addParam(user_matrix_dimension_3d);
}

private void initUser_matrix_dimension_4d(){
    NumberParam user_matrix_dimension_4d = new NumberParam();
    user_matrix_dimension_4d.setMinValue(0);
    user_matrix_dimension_4d.setMaxValue(65536);
    user_matrix_dimension_4d.setNumberEnum(NumberEnum.valueOf("Scan"));
    user_matrix_dimension_4d.setDefaultValue(1);
    user_matrix_dimension_4d.setValue(1);
    user_matrix_dimension_4d.setRestrictedToSuggested(false);
    user_matrix_dimension_4d.setName("USER_MATRIX_DIMENSION_4D");
    user_matrix_dimension_4d.setDescription("The matrix size of the fourth dimension");
    user_matrix_dimension_4d.setLocked(true);
    user_matrix_dimension_4d.setLockedToDefault(false);
    RoleEnum[] roles;
    roles = new RoleEnum[1];
    roles[0] = RoleEnum.valueOf("User");
    user_matrix_dimension_4d.setRoles(roles);
    user_matrix_dimension_4d.setGroup(EnumGroup.valueOf("Scan"));
    user_matrix_dimension_4d.setCategory(Category.valueOf("Acquisition"));
    this.addParam(user_matrix_dimension_4d);
}
public float getVersion() {return 0.01f;}public String getName() {return "GradShims_dev";}// </editor-fold>

//GEN
}
