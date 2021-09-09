
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import rs2d.spinlab.hardware.controller.HardwareHandler;
import rs2d.spinlab.instrument.Instrument;
import rs2d.spinlab.instrument.util.GradientMath;
import rs2d.spinlab.plugins.loaders.LoaderFactory;
import rs2d.spinlab.plugins.loaders.PluginLoaderInterface;
import rs2d.spinlab.sequence.element.TimeElement;
import rs2d.spinlab.sequence.table.generator.*;
import rs2d.spinlab.tools.role.*;
import rs2d.spinlab.sequence.table.*;
import rs2d.spinlab.sequenceGenerator.SequenceGeneratorAbstract;
import rs2d.spinlab.tools.param.*;
import rs2d.spinlab.tools.param.EnumGroup;
import rs2d.spinlab.tools.utility.Nucleus;

// **************************************************************************************************
// *************************************** SEQUENCE GENERATOR ***************************************
// **************************************************************************************************
//
public class FastRFCalib_dev extends SequenceGeneratorAbstract {

    public double proton_frequency;
    public double observe_frequency;
    public double min_time_per_acq_point;
    public double gMax;
    Nucleus nucleus;
    private static final Logger logger = Logger.getLogger("sequenceParams");

    public FastRFCalib_dev() {
        super();
        initParam();
    }

    @Override
    public void init() {
        super.init();

        // Define default, min, max and suggested values regarding the instrument
        getParamFromName("BASE_FREQ_1").setDefaultValue(Instrument.instance().getDevices().getMagnet().getProtonFrequency());
        getParamFromName("MAGNETIC_FIELD_STRENGTH").setDefaultValue(Instrument.instance().getDevices().getMagnet().getField());
        getParamFromName("DIGITAL_FILTER_SHIFT").setDefaultValue(Instrument.instance().getDevices().getCameleons().get(0).getAcquDeadPointCount());
        getParamFromName("DIGITAL_FILTER_REMOVED").setDefaultValue(Instrument.instance().getDevices().getCameleons().get(0).isRemoveAcquDeadPoint());

        List<String> tx_shape = new ArrayList<>();
        tx_shape.add("HARD");
        tx_shape.add("GAUSSIAN");
        tx_shape.add("SINC3");
        tx_shape.add("SINC5");
        ((TextParam) getParamFromName("TX_SHAPE_90")).setSuggestedValues(tx_shape);
        ((TextParam) getParamFromName("TX_SHAPE_90")).setRestrictedToSuggested(true);
    }

// ==============================
// -----   GENERATE
// ==============================
    @Override
    public void generate() throws Exception {
        this.beforeRouting();
        if (!this.isRouted()) {
            this.route();
            this.initAfterRouting();//init before setup
        }
        if (!((BooleanParam) getParamFromName("SETUP_MODE")).getValue()) {
            this.afterRouting();    //avoid exception during setup
        }

        this.checkAndFireException();
    }

// ------------ INIT AFTER ROUTING -------------
    private void initAfterRouting() {
        logger.info("------------ INIT AFTER ROUTING -------------");
        logger.info("------------ Nothing to do -------------");
    }

// ------------ BEFORE ROUTING -------------
    private void beforeRouting() throws Exception {

        logger.info("------------ BEFORE ROUTING -------------");

        // -----------------------------------------------
        // RX parameters : nucleus, RX gain & frequencies
        // -----------------------------------------------
        logger.info("RX parameters : nucleus, RX gain & frequencies");

        nucleus = Nucleus.getNucleusForName((String) getParamFromName("NUCLEUS_1").getValue());
        proton_frequency = Instrument.instance().getDevices().getMagnet().getProtonFrequency();
        min_time_per_acq_point = HardwareHandler.getInstance().getSequenceHandler().getCompiler().getTransfertTimePerDataPt();
        gMax = GradientMath.getMaxGradientStrength();
        observe_frequency = nucleus.getFrequency(proton_frequency);

        setSequenceParamValue("Rx Gain", "RECEIVER_GAIN");
        setParamValue("RECEIVER_COUNT", Instrument.instance().getObservableRxs(nucleus).size());
        logger.log(Level.FINEST, "RECEIVER_GAIN : {0}", ((NumberParam) getParamFromName("RECEIVER_GAIN")).getValue().intValue());

        setSequenceParamValue("IF", Instrument.instance().getIfFrequency());
        setParamValue("INTERMEDIATE_FREQUENCY", Instrument.instance().getIfFrequency());

        double base_frequency = (Double) getParamFromName("BASE_FREQ_1").getValue();
        setSequenceParamValue("Tx_frequency", base_frequency);
        setParamValue("OBSERVED_FREQUENCY", base_frequency);
        logger.log(Level.FINEST, "OBSERVED_FREQUENCY : {0}", base_frequency);

        setSequenceParamValue("TxNucleus", "NUCLEUS_1");
        setParamValue("OBSERVED_NUCLEUS", getParamFromName("NUCLEUS_1").getValue());

        // -----------------------------------------------
        // matrix & acquisition dimensions
        // -----------------------------------------------
        logger.info("matrix & acquisition dimensions");

        int user_matrix_dimension_1D = ((NumberParam) getParamFromName("USER_MATRIX_DIMENSION_1D")).getValue().intValue();
        int user_matrix_dimension_2D = ((NumberParam) getParamFromName("USER_MATRIX_DIMENSION_2D")).getValue().intValue();
        int user_matrix_dimension_3D = ((NumberParam) getParamFromName("USER_MATRIX_DIMENSION_3D")).getValue().intValue();
        int user_matrix_dimension_4D = ((NumberParam) getParamFromName("USER_MATRIX_DIMENSION_4D")).getValue().intValue();

        int acquisition_matrix_dimension_1D = user_matrix_dimension_1D;
        int acquisition_matrix_dimension_2D = user_matrix_dimension_2D;
        int acquisition_matrix_dimension_3D = user_matrix_dimension_3D;
        int acquisition_matrix_dimension_4D = user_matrix_dimension_4D;

        // set the calculated acquisition matrix
        setParamValue("ACQUISITION_MATRIX_DIMENSION_1D", acquisition_matrix_dimension_1D);
        setParamValue("ACQUISITION_MATRIX_DIMENSION_2D", acquisition_matrix_dimension_2D);
        setParamValue("ACQUISITION_MATRIX_DIMENSION_3D", acquisition_matrix_dimension_3D);
        setParamValue("ACQUISITION_MATRIX_DIMENSION_4D", acquisition_matrix_dimension_4D);

        // set the calculated sequence dimensions 
        setSequenceParamValue("PS", "DUMMY_SCAN");
        setSequenceParamValue("Nb Point", acquisition_matrix_dimension_1D);
        setSequenceParamValue("Nb 1D", "NUMBER_OF_AVERAGES");
        setSequenceParamValue("Nb 2D", acquisition_matrix_dimension_3D);
        setSequenceParamValue("Nb 3D", acquisition_matrix_dimension_2D);
        setSequenceParamValue("Nb 4D", acquisition_matrix_dimension_4D);

        logger.log(Level.FINEST, "Nb Point : {0}", acquisition_matrix_dimension_1D);
        logger.log(Level.FINEST, "Nb 1D : {0}", ((NumberParam) getParamFromName("NUMBER_OF_AVERAGES")).getValue().intValue());
        logger.log(Level.FINEST, "Nb 2D : {0}", acquisition_matrix_dimension_2D);

        // -----------------------------------------------
        // spectral width & observation time
        // -----------------------------------------------
        logger.info("spectral width & observation time");

        double spectral_width = ((NumberParam) getParamFromName("SPECTRAL_WIDTH")).getValue().doubleValue();            // get user defined spectral width
        double sw = HardwareHandler.getInstance().getSequenceHandler().getCompiler().getNearestSW(spectral_width);      // get real spectral width from Cameleon
        setSequenceParamValue("SW", sw);    // set spectral width to sequence
        logger.log(Level.FINEST, "sw : {0}", sw);

        double observation_time = acquisition_matrix_dimension_1D / sw;
        logger.log(Level.FINEST, "observation_time : {0}", observation_time);
        setParamValue("ACQUISITION_TIME_PER_SCAN", observation_time);   // display observation time
        setSequenceTableFirstValue("TO", observation_time);             // set observation time to sequence

        // -----------------------------------------------
        // TX shape generation
        // -----------------------------------------------
        logger.info("TX shape generation");

        String window = "Hamming";
        Shape tx_shape_90 = (Shape) getSequence().getPublicTable("Tx_shape_90");
        Shape tx_phase_shape_90 = (Shape) getSequence().getPublicTable("Tx_shape_phase_90");
        tx_phase_shape_90.clear();
        int nb_shape_points = 128;

        generateTxShape(tx_shape_90, tx_phase_shape_90, nb_shape_points, window);

        // -----------------------------------------------
        // enable gradient lines
        // -----------------------------------------------
        logger.fine("enable gradient lines");
        setSequenceParamValue("Grad_enable_slice", "GRADIENT_ENABLE_SLICE");

        // -----------------------------------------------
        // set the pulse parameters in the sequence
        // -----------------------------------------------
        logger.info("set the pulse parameters in the sequence");
        setSequenceParamValue("Tx_att", "PULSE_ATT");                   // set PULSE_ATT to sequence  
        setSequenceTableFirstValue("Tx_length_90", "TX_LENGTH_90");     // set RF pulse length to sequence
        setSequenceTableFirstValue("Tx_amp_90", "TX_AMP_90");           // set 90° RF pulse amplitude to sequence

//        // -----------------------------------------------
//        // activate the external synchronization
//        // -----------------------------------------------
//        logger.fine("activate the external synchronization");
//
//        if (((BooleanParam) getParamFromName("SYNCHRO_ENABLED")).getValue()) {
//            getSequence().getPublicParam("Synchro_trigger").setValue(TimeElement.Trigger.External);
//        } else {
//            getSequence().getPublicParam("Synchro_trigger").setValue(TimeElement.Trigger.Timer);
//        }
    }

    // ------------ AFTER ROUTING -------------
    private void afterRouting() throws Exception {

        logger.info("------------ AFTER ROUTING -------------");
        // -----------------------------------------------
        // get user defined timing parameters from panel
        // -----------------------------------------------
        logger.info("get user defined timing parameters from panel");

//        double te = ((NumberParam) getParamFromName("ECHO_TIME")).getValue().doubleValue();
        double tx_length_90 = ((NumberParam) getParamFromName("TX_LENGTH_90")).getValue().doubleValue();
//        double observation_time = ((NumberParam) getParamFromName("ACQUISITION_TIME_PER_SCAN")).getValue().doubleValue();
        double grad_rise_time = ((NumberParam) getParamFromName("GRADIENT_RISE_TIME")).getValue().doubleValue();

        // -----------------------------------------------
        // get user defined matrix dimensions from panel
        // -----------------------------------------------
        logger.info("get user defined matrix dimensions from panel");

        int acquisition_matrix_dimension_1D = ((NumberParam) getParamFromName("ACQUISITION_MATRIX_DIMENSION_1D")).getValue().intValue();
        double slice_thickness = ((NumberParam) getParamFromName("SLICE_THICKNESS")).getValue().doubleValue();
        double slice_thickness_total = slice_thickness;

        // -----------------------------------------------
        // frequency OFF CENTER offset
        // -----------------------------------------------
        logger.fine("frequency OFF CENTER offset");

        // -----------------------------------------------
        // calculate gradient equivalent rise time
        // -----------------------------------------------
        logger.fine("calculate gradient equivalent rise time");

        setSequenceTableFirstValue("Grad_rise_time", grad_rise_time);
//        double grad_shape_rise_factor_up = Utility.voltageFillingFactor((Shape) getSequence().getPublicTable("Rise_shape_up"));
//        double grad_shape_rise_factor_down = Utility.voltageFillingFactor((Shape) getSequence().getPublicTable("Rise_shape_down"));
//        double grad_shape_rise_time = grad_shape_rise_factor_up * grad_rise_time + grad_shape_rise_factor_down * grad_rise_time;        // shape dependant equivalent rise time

        // -----------------------------------------------
        // calculate SLICE gradient amplitude
        // -----------------------------------------------
        logger.info("calculate SLICE gradient amplitude");

        double grad_amp_slice_slice;
        double tx_bandwidth_factor_90;
        double tx_bandwidth_90;

        //Measured at 90%
//        double bandwidth_factor_hard = 0.5;         // shape defined bandwidth factors -- used to adjust the slice gradient amplitude from the shape & length of RF pulses
//        double bandwidth_factor_gaussian = 1;
//        double bandwidth_factor_sinc3 = 2;
//        double bandwidth_factor_sinc5 = 4;
        // -----------------------------------------------
        // calculate RF pulses bandwidth
        // -----------------------------------------------
        logger.info("calculate RF pulses bandwidth");

//        if ("GAUSSIAN".equalsIgnoreCase((String) getParamFromName("TX_SHAPE_90").getValue())) {
//            tx_bandwidth_factor_90 = bandwidth_factor_gaussian;
//        } else if ("SINC3".equalsIgnoreCase((String) getParamFromName("TX_SHAPE_90").getValue())) {
//            tx_bandwidth_factor_90 = bandwidth_factor_sinc3;
//        } else if ("SINC5".equalsIgnoreCase((String) getParamFromName("TX_SHAPE_90").getValue())) {
//            tx_bandwidth_factor_90 = bandwidth_factor_sinc5;
//        } else {
//            tx_bandwidth_factor_90 = bandwidth_factor_hard;
//        }
        //for user calibration -- to be removed later
        tx_bandwidth_factor_90 = ((NumberParam) getParamFromName("TX_BANDWIDTH_FACTOR")).getValue().doubleValue();
        tx_bandwidth_90 = tx_bandwidth_factor_90 / tx_length_90;

        // ---------------------------------------------------------------------
        // calculate SLICE gradient amplitudes for 90° & 180° RF pulses
        // ---------------------------------------------------------------------
        logger.fine("calculate SLICE gradient amplitudes for 90 RF pulses");

        grad_amp_slice_slice = (tx_bandwidth_90 / (GradientMath.GAMMA * slice_thickness_total));
        grad_amp_slice_slice = grad_amp_slice_slice * 100.0 / gMax;
        setSequenceTableFirstValue("Grad_amp_slice", grad_amp_slice_slice);
        logger.log(Level.FINEST, "grad_amp_slice_slice in %gMax : {0}", grad_amp_slice_slice);

        // --------------------------------------------------------------------------------------------------------------------------------------------
        // TIMING --- TIMING --- TIMING --- TIMING --- TIMING --- TIMING --- TIMING --- TIMING --- TIMING --- TIMING --- TIMING --- TIMING --- TIMING
        // --------------------------------------------------------------------------------------------------------------------------------------------
        //
        //                                                          index of sequence events
        //
        // --------------------------------------------------------------------------------------------------------------------------------------------
        logger.info("TIMING --- TIMING --- TIMING");

        int eventStart = 0;
        int eventFirstPulse = 3;
        int eventSecondPulse = 6;
        int eventThirdPulse = 9;

        int eventDelay1 = 4;
        int eventDelay2 = 7;
//        int eventDelay3 = 15;
//        int eventAcq = 17;
        int eventEnd = 12;
        // ------------------------------------------
        // delays for sequence instructions
        // ------------------------------------------        
        logger.fine("delays for sequence instructions");
        double min_instruction_delay = 0.000010;     // single instruction minimal duration

        // ------------------------------------------
        // calculate delays adapted to current tau1 = 3.5 msec, tau2 = 4 * tau1 (f=4, patent US4983921)
        // ------------------------------------------
        double tau1 = ((NumberParam) this.getParamFromName("Tau")).getValue().doubleValue();

        double timeBetweenP1P2 = getTimeBetweenEvents(eventFirstPulse + 1, eventSecondPulse - 1);
        timeBetweenP1P2 = timeBetweenP1P2 - getTimeForEvent(eventDelay1);
        timeBetweenP1P2 += getTimeForEvent(eventFirstPulse) / 2d + getTimeForEvent(eventSecondPulse) / 2d;
        double delai1 = tau1 - timeBetweenP1P2;

        if (delai1 < min_instruction_delay) {
            double tau1Min = timeBetweenP1P2 + min_instruction_delay;
            delai1 = min_instruction_delay;
            this.getUnreachParamExceptionManager().addParam("Tau", tau1, tau1Min, ((NumberParam) getParamFromName("Tau")).getMaxValue(), "Tau is too short");
            tau1 = tau1Min;
        }

        setSequenceTableFirstValue("Delay_1", delai1);

        double tau2 = 4d * tau1;
        double timeBetweenP2P3 = getTimeBetweenEvents(eventSecondPulse + 1, eventThirdPulse - 1);
        timeBetweenP2P3 = timeBetweenP2P3 - getTimeForEvent(eventDelay2);

        // From the middle to middle of the pulse
        timeBetweenP2P3 += getTimeForEvent(eventSecondPulse) / 2d + getTimeForEvent(eventThirdPulse) / 2d;
        double delai2 = tau2 - tau1 - timeBetweenP2P3;

        // As tau2 is 4 tau, timing should always be good
        setSequenceTableFirstValue("Delay_2", delai2);

        double sequenceTime = getTimeBetweenEvents(eventStart, eventEnd - 1);
        double tr = ((NumberParam) this.getParamFromName(MriDefaultParams.REPETITION_TIME.name())).getValue().doubleValue();
        double lastDelay = tr - sequenceTime;
        double minLastDelay = acquisition_matrix_dimension_1D * min_time_per_acq_point;
        if (lastDelay < minLastDelay) {
            double tr_min = sequenceTime + min_time_per_acq_point;
            this.getUnreachParamExceptionManager().addParam("REPETITION_TIME", tr, tr_min, ((NumberParam) getParamFromName("REPETITION_TIME")).getMaxValue(), "TR too short for this TAU value (or SW, NBPoint)");
        }
        setSequenceTableFirstValue("Last_delay", lastDelay);

        logger.info("------------ END AFTER ROUTING -------------");

    }

// **************************************************************************************************
// *********************************** END OF SEQUENCE GENERATOR ************************************
// **************************************************************************************************
//
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
     * Get the duration of an event
     *
     * @param indexEvent The index of the time event
     * @return The corresponding time
     */
    public double getTimeForEvent(int indexEvent) {
        double time = ((TimeElement) getSequence().getTimeChannel().get(indexEvent)).getTime().getFirst().doubleValue();
        return time;
    }

    /**
     * Generate a table of element with a Sinus Cardinal generator
     *
     * @param table The table to be set
     * @param nbpoint The number of point of the generated sinus cardinal
     * @param nblobe The number of lob of the generated sinus cardinal
     * @param amp The amplitude of the generated sinus cardinal (in %)
     * @param abs true if you want the absolute values and false otherwise
     */
    private void setTableValuesFromSincGen(Table table, int nbpoint, int nblobe, double amp, Boolean abs, String window) throws Exception {
        TableGeneratorInterface gen = loadTableGenerator("Sinus Cardinal with Apodisation");
        table.setGenerator(gen);
        if (gen == null) {
            table.clear();
            table.setFirst(100);
        } else {
            gen.getParams().get(0).setValue(nbpoint);
            gen.getParams().get(1).setValue(nblobe);
            gen.getParams().get(2).setValue(amp);
            gen.getParams().get(3).setValue(abs);//abs
            gen.getParams().get(4).setValue(window);//abs
            gen.generate();
        }
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
        table.setGenerator(gen);
        if (gen == null) {
            table.clear();
            table.setFirst(100);
        } else {
            gen.getParams().get(0).setValue(nbpoint);
            gen.getParams().get(1).setValue(width);
            gen.getParams().get(2).setValue(amp);
            gen.getParams().get(3).setValue(abs);//abs

            gen.generate();
        }
    }

    private void generateTxShape(Shape tx_shape_90, Shape tx_phase_shape_90, int nb_shape_points, String window) throws Exception {
        if ("GAUSSIAN".equalsIgnoreCase((String) getParamFromName("TX_SHAPE_90").getValue())) {
            setTableValuesFromGaussGen(tx_shape_90, nb_shape_points, 0.250, 100, false);
        } else if ("SINC3".equalsIgnoreCase((String) getParamFromName("TX_SHAPE_90").getValue())) {
            setTableValuesFromSincGen(tx_shape_90, nb_shape_points, 2, 100, true, window);

            for (int i = 0; i < ((int) (nb_shape_points / 4)); i++) {
                tx_phase_shape_90.add(180);
            }
            for (int i = ((int) (nb_shape_points / 4)); i < (3 * ((int) (nb_shape_points / 4))); i++) {
                tx_phase_shape_90.add(0);
            }
            for (int i = (3 * ((int) (nb_shape_points / 4))); i < nb_shape_points; i++) {
                tx_phase_shape_90.add(180);
            }
        } else if ("SINC5".equalsIgnoreCase((String) getParamFromName("TX_SHAPE_90").getValue())) {
            setTableValuesFromSincGen(tx_shape_90, nb_shape_points, 3, 100, true, window);

            for (int i = 0; i < ((int) (nb_shape_points / 6)); i++) {
                tx_phase_shape_90.add(0);
            }
            for (int i = ((int) (nb_shape_points / 6)); i < (2 * (int) (nb_shape_points / 6)); i++) {
                tx_phase_shape_90.add(180);
            }
            for (int i = (2 * ((int) (nb_shape_points / 6))); i < (4 * ((int) (nb_shape_points / 6))); i++) {
                tx_phase_shape_90.add(0);
            }
            for (int i = (4 * ((int) (nb_shape_points / 6))); i < (5 * ((int) (nb_shape_points / 6))); i++) {
                tx_phase_shape_90.add(180);
            }
            for (int i = (5 * ((int) (nb_shape_points / 6))); i < nb_shape_points; i++) {
                tx_phase_shape_90.add(0);
            }
        } else if ("HARD".equalsIgnoreCase((String) getParamFromName("TX_SHAPE_90").getValue())) {
            tx_shape_90.clear();    // set to HARD pulse
            tx_shape_90.setFirst(100);
        }
        double power_factor_90 = Utility.powerFillingFactor(tx_shape_90);
        this.setParamValue("Pulse Feeling Factor", power_factor_90);
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
        this.initGradient_enable_slice();
        this.initGradient_rise_time();
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
        this.initOff_center_field_of_view_1d();
        this.initOff_center_field_of_view_2d();
        this.initOff_center_field_of_view_3d();
        this.initOffset_freq_1();
        this.initOffset_freq_2();
        this.initOffset_freq_3();
        this.initOffset_freq_4();
        this.initOrientation();
        this.initParopt_param();
        this.initPhase_cycling();
        this.initProbe();
        this.initPulse_att();
        this.initReceiver_count();
        this.initReceiver_gain();
        this.initRepetition_time();
        this.initResolution_frequency();
        this.initSequence_name();
        this.initSetup_mode();
        this.initSlice_thickness();
        this.initSpacing_between_slice();
        this.initSpecimen_position();
        this.initSpectral_width();
        this.initSquare_pixel();
        this.initSynchro_enabled();
        this.initTransform_plugin();
        this.initTx_amp_90();
        this.initTx_bandwidth_factor();
        this.initTx_length_90();
        this.initTx_route();
        this.initTx_shape_90();
        this.initUser_matrix_dimension_1d();
        this.initUser_matrix_dimension_2d();
        this.initUser_matrix_dimension_3d();
        this.initUser_matrix_dimension_4d();
        this.initUser_zero_filling_2d();
        this.initUser_zero_filling_3d();
    }

    private void initAccu_dim() {
        NumberParam accu_dim = new NumberParam();
        accu_dim.setMinValue(0);
        accu_dim.setMaxValue(3);
        accu_dim.setNumberEnum(NumberEnum.valueOf("Integer"));
        accu_dim.setDefaultValue(1);
        accu_dim.setValue(1);
        accu_dim.setRestrictedToSuggested(false);
        accu_dim.setName("ACCU_DIM");
        accu_dim.setDescription("ACCU_DIM.description");
        accu_dim.setLocked(false);
        accu_dim.setLockedToDefault(false);
        RoleEnum[] roles;
        roles = new RoleEnum[1];
        roles[0] = RoleEnum.valueOf("User");
        accu_dim.setRoles(roles);
        accu_dim.setGroup(EnumGroup.valueOf("Scan"));
        accu_dim.setCategory(Category.valueOf("Acquisition"));
        this.addParam(accu_dim);
    }

    private void initAcquisition_matrix_dimension_1d() {
        NumberParam acquisition_matrix_dimension_1d = new NumberParam();
        acquisition_matrix_dimension_1d.setMinValue(0);
        acquisition_matrix_dimension_1d.setMaxValue(65536);
        acquisition_matrix_dimension_1d.setNumberEnum(NumberEnum.valueOf("Scan"));
        acquisition_matrix_dimension_1d.setDefaultValue(128);
        acquisition_matrix_dimension_1d.setValue(512);
        acquisition_matrix_dimension_1d.setRestrictedToSuggested(false);
        acquisition_matrix_dimension_1d.setName("ACQUISITION_MATRIX_DIMENSION_1D");
        acquisition_matrix_dimension_1d.setDescription("The acquisition size of the first dimension");
        acquisition_matrix_dimension_1d.setLocked(false);
        acquisition_matrix_dimension_1d.setLockedToDefault(false);
        RoleEnum[] roles;
        roles = new RoleEnum[1];
        roles[0] = RoleEnum.valueOf("User");
        acquisition_matrix_dimension_1d.setRoles(roles);
        acquisition_matrix_dimension_1d.setGroup(EnumGroup.valueOf("Scan"));
        acquisition_matrix_dimension_1d.setCategory(Category.valueOf("Acquisition"));
        this.addParam(acquisition_matrix_dimension_1d);
    }

    private void initAcquisition_matrix_dimension_2d() {
        NumberParam acquisition_matrix_dimension_2d = new NumberParam();
        acquisition_matrix_dimension_2d.setMinValue(0);
        acquisition_matrix_dimension_2d.setMaxValue(65536);
        acquisition_matrix_dimension_2d.setNumberEnum(NumberEnum.valueOf("Scan"));
        acquisition_matrix_dimension_2d.setDefaultValue(128);
        acquisition_matrix_dimension_2d.setValue(1);
        acquisition_matrix_dimension_2d.setRestrictedToSuggested(false);
        acquisition_matrix_dimension_2d.setName("ACQUISITION_MATRIX_DIMENSION_2D");
        acquisition_matrix_dimension_2d.setDescription("The acquisition size of the second dimension");
        acquisition_matrix_dimension_2d.setLocked(false);
        acquisition_matrix_dimension_2d.setLockedToDefault(false);
        RoleEnum[] roles;
        roles = new RoleEnum[1];
        roles[0] = RoleEnum.valueOf("User");
        acquisition_matrix_dimension_2d.setRoles(roles);
        acquisition_matrix_dimension_2d.setGroup(EnumGroup.valueOf("Scan"));
        acquisition_matrix_dimension_2d.setCategory(Category.valueOf("Acquisition"));
        this.addParam(acquisition_matrix_dimension_2d);
    }

    private void initAcquisition_matrix_dimension_3d() {
        NumberParam acquisition_matrix_dimension_3d = new NumberParam();
        acquisition_matrix_dimension_3d.setMinValue(0);
        acquisition_matrix_dimension_3d.setMaxValue(65536);
        acquisition_matrix_dimension_3d.setNumberEnum(NumberEnum.valueOf("Scan"));
        acquisition_matrix_dimension_3d.setDefaultValue(1);
        acquisition_matrix_dimension_3d.setValue(1);
        acquisition_matrix_dimension_3d.setRestrictedToSuggested(false);
        acquisition_matrix_dimension_3d.setName("ACQUISITION_MATRIX_DIMENSION_3D");
        acquisition_matrix_dimension_3d.setDescription("The acquisition size of the third dimension");
        acquisition_matrix_dimension_3d.setLocked(false);
        acquisition_matrix_dimension_3d.setLockedToDefault(false);
        RoleEnum[] roles;
        roles = new RoleEnum[1];
        roles[0] = RoleEnum.valueOf("User");
        acquisition_matrix_dimension_3d.setRoles(roles);
        acquisition_matrix_dimension_3d.setGroup(EnumGroup.valueOf("Scan"));
        acquisition_matrix_dimension_3d.setCategory(Category.valueOf("Acquisition"));
        this.addParam(acquisition_matrix_dimension_3d);
    }

    private void initAcquisition_matrix_dimension_4d() {
        NumberParam acquisition_matrix_dimension_4d = new NumberParam();
        acquisition_matrix_dimension_4d.setMinValue(0);
        acquisition_matrix_dimension_4d.setMaxValue(65536);
        acquisition_matrix_dimension_4d.setNumberEnum(NumberEnum.valueOf("Scan"));
        acquisition_matrix_dimension_4d.setDefaultValue(1);
        acquisition_matrix_dimension_4d.setValue(1);
        acquisition_matrix_dimension_4d.setRestrictedToSuggested(false);
        acquisition_matrix_dimension_4d.setName("ACQUISITION_MATRIX_DIMENSION_4D");
        acquisition_matrix_dimension_4d.setDescription("The acquisition size of the fourth dimension");
        acquisition_matrix_dimension_4d.setLocked(false);
        acquisition_matrix_dimension_4d.setLockedToDefault(false);
        RoleEnum[] roles;
        roles = new RoleEnum[1];
        roles[0] = RoleEnum.valueOf("User");
        acquisition_matrix_dimension_4d.setRoles(roles);
        acquisition_matrix_dimension_4d.setGroup(EnumGroup.valueOf("Scan"));
        acquisition_matrix_dimension_4d.setCategory(Category.valueOf("Acquisition"));
        this.addParam(acquisition_matrix_dimension_4d);
    }

    private void initAcquisition_time_per_scan() {
        NumberParam acquisition_time_per_scan = new NumberParam();
        acquisition_time_per_scan.setMinValue(0.0);
        acquisition_time_per_scan.setMaxValue(1.0E9);
        acquisition_time_per_scan.setNumberEnum(NumberEnum.valueOf("Time"));
        acquisition_time_per_scan.setDefaultValue(1.0);
        acquisition_time_per_scan.setValue(0.012713984000000001);
        acquisition_time_per_scan.setRestrictedToSuggested(false);
        acquisition_time_per_scan.setName("ACQUISITION_TIME_PER_SCAN");
        acquisition_time_per_scan.setDescription("The acquisition time per scan");
        acquisition_time_per_scan.setLocked(false);
        acquisition_time_per_scan.setLockedToDefault(false);
        RoleEnum[] roles;
        roles = new RoleEnum[1];
        roles[0] = RoleEnum.valueOf("User");
        acquisition_time_per_scan.setRoles(roles);
        acquisition_time_per_scan.setGroup(EnumGroup.valueOf("Reception"));
        acquisition_time_per_scan.setCategory(Category.valueOf("Acquisition"));
        this.addParam(acquisition_time_per_scan);
    }

    private void initBase_freq_1() {
        NumberParam base_freq_1 = new NumberParam();
        base_freq_1.setMinValue(0.0);
        base_freq_1.setMaxValue(3.0E9);
        base_freq_1.setNumberEnum(NumberEnum.valueOf("Frequency"));
        base_freq_1.setDefaultValue(6.3E7);
        base_freq_1.setValue(6.3E7);
        base_freq_1.setRestrictedToSuggested(false);
        base_freq_1.setName("BASE_FREQ_1");
        base_freq_1.setDescription("The base frequency of the first sequence channel");
        base_freq_1.setLocked(false);
        base_freq_1.setLockedToDefault(true);
        RoleEnum[] roles;
        roles = new RoleEnum[1];
        roles[0] = RoleEnum.valueOf("User");
        base_freq_1.setRoles(roles);
        base_freq_1.setGroup(EnumGroup.valueOf("Emission"));
        base_freq_1.setCategory(Category.valueOf("Acquisition"));
        this.addParam(base_freq_1);
    }

    private void initBase_freq_2() {
        NumberParam base_freq_2 = new NumberParam();
        base_freq_2.setMinValue(0.0);
        base_freq_2.setMaxValue(3.0E9);
        base_freq_2.setNumberEnum(NumberEnum.valueOf("Frequency"));
        base_freq_2.setDefaultValue(0.0);
        base_freq_2.setValue(0.0);
        base_freq_2.setRestrictedToSuggested(false);
        base_freq_2.setName("BASE_FREQ_2");
        base_freq_2.setDescription("The base frequency of the second sequence channel");
        base_freq_2.setLocked(false);
        base_freq_2.setLockedToDefault(true);
        RoleEnum[] roles;
        roles = new RoleEnum[1];
        roles[0] = RoleEnum.valueOf("User");
        base_freq_2.setRoles(roles);
        base_freq_2.setGroup(EnumGroup.valueOf("Emission"));
        base_freq_2.setCategory(Category.valueOf("Acquisition"));
        this.addParam(base_freq_2);
    }

    private void initBase_freq_3() {
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

    private void initBase_freq_4() {
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

    private void initDigital_filter_removed() {
        BooleanParam digital_filter_removed = new BooleanParam();
        digital_filter_removed.setDefaultValue(false);
        digital_filter_removed.setValue(false);
        digital_filter_removed.setName("DIGITAL_FILTER_REMOVED");
        digital_filter_removed.setDescription("Data shift due to the digital filter are removed");
        digital_filter_removed.setLocked(false);
        digital_filter_removed.setLockedToDefault(true);
        RoleEnum[] roles;
        roles = new RoleEnum[1];
        roles[0] = RoleEnum.valueOf("User");
        digital_filter_removed.setRoles(roles);
        digital_filter_removed.setGroup(EnumGroup.valueOf("Reception"));
        digital_filter_removed.setCategory(Category.valueOf("Acquisition"));
        this.addParam(digital_filter_removed);
    }

    private void initDigital_filter_shift() {
        NumberParam digital_filter_shift = new NumberParam();
        digital_filter_shift.setMinValue(-2147483648);
        digital_filter_shift.setMaxValue(2147483647);
        digital_filter_shift.setNumberEnum(NumberEnum.valueOf("Integer"));
        digital_filter_shift.setDefaultValue(18);
        digital_filter_shift.setValue(18);
        digital_filter_shift.setRestrictedToSuggested(false);
        digital_filter_shift.setName("DIGITAL_FILTER_SHIFT");
        digital_filter_shift.setDescription("Data shift due to the digital filter");
        digital_filter_shift.setLocked(false);
        digital_filter_shift.setLockedToDefault(true);
        RoleEnum[] roles;
        roles = new RoleEnum[1];
        roles[0] = RoleEnum.valueOf("User");
        digital_filter_shift.setRoles(roles);
        digital_filter_shift.setGroup(EnumGroup.valueOf("Reception"));
        digital_filter_shift.setCategory(Category.valueOf("Acquisition"));
        this.addParam(digital_filter_shift);
    }

    private void initDummy_scan() {
        NumberParam dummy_scan = new NumberParam();
        dummy_scan.setMinValue(0);
        dummy_scan.setMaxValue(65536);
        dummy_scan.setNumberEnum(NumberEnum.valueOf("Scan"));
        dummy_scan.setDefaultValue(128);
        dummy_scan.setValue(128);
        dummy_scan.setRestrictedToSuggested(false);
        dummy_scan.setName("DUMMY_SCAN");
        dummy_scan.setDescription("Dummy Scan");
        dummy_scan.setLocked(false);
        dummy_scan.setLockedToDefault(false);
        RoleEnum[] roles;
        roles = new RoleEnum[1];
        roles[0] = RoleEnum.valueOf("User");
        dummy_scan.setRoles(roles);
        dummy_scan.setGroup(EnumGroup.valueOf("Scan"));
        dummy_scan.setCategory(Category.valueOf("Acquisition"));
        this.addParam(dummy_scan);
    }

    private void initEcho_time() {
        NumberParam echo_time = new NumberParam();
        echo_time.setMinValue(0.0);
        echo_time.setMaxValue(1.0E9);
        echo_time.setNumberEnum(NumberEnum.valueOf("Time"));
        echo_time.setDefaultValue(0.005);
        echo_time.setValue(0.0035);
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

    private void initField_of_view() {
        NumberParam field_of_view = new NumberParam();
        field_of_view.setMinValue(0.001);
        field_of_view.setMaxValue(1.7976931348623157E308);
        field_of_view.setNumberEnum(NumberEnum.valueOf("Length"));
        field_of_view.setDefaultValue(0.6);
        field_of_view.setValue(0.064);
        field_of_view.setRestrictedToSuggested(false);
        field_of_view.setName("FIELD_OF_VIEW");
        field_of_view.setDescription("Field of View in the frequency encoding direction");
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

    private void initFlip_angle() {
        NumberParam flip_angle = new NumberParam();
        flip_angle.setMinValue(-360.0);
        flip_angle.setMaxValue(360.0);
        flip_angle.setNumberEnum(NumberEnum.valueOf("Angle"));
        flip_angle.setDefaultValue(90.0);
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

    private void initGradient_enable_slice() {
        BooleanParam gradient_enable_slice = new BooleanParam();
        gradient_enable_slice.setDefaultValue(true);
        gradient_enable_slice.setValue(true);
        gradient_enable_slice.setName("GRADIENT_ENABLE_SLICE");
        gradient_enable_slice.setDescription("enable the slice encoding gradient");
        gradient_enable_slice.setLocked(false);
        gradient_enable_slice.setLockedToDefault(false);
        RoleEnum[] roles;
        roles = new RoleEnum[1];
        roles[0] = RoleEnum.valueOf("SequenceDesigner");
        gradient_enable_slice.setRoles(roles);
        gradient_enable_slice.setGroup(EnumGroup.valueOf("Gradient"));
        gradient_enable_slice.setCategory(Category.valueOf("Acquisition"));
        this.addParam(gradient_enable_slice);
    }

    private void initGradient_rise_time() {
        NumberParam gradient_rise_time = new NumberParam();
        gradient_rise_time.setMinValue(0.0);
        gradient_rise_time.setMaxValue(1.0);
        gradient_rise_time.setNumberEnum(NumberEnum.valueOf("Time"));
        gradient_rise_time.setDefaultValue(1.9999999999999998E-4);
        gradient_rise_time.setValue(2.5E-4);
        gradient_rise_time.setRestrictedToSuggested(false);
        gradient_rise_time.setName("GRADIENT_RISE_TIME");
        gradient_rise_time.setDescription("The rise time of the gradient");
        gradient_rise_time.setLocked(false);
        gradient_rise_time.setLockedToDefault(false);
        RoleEnum[] roles;
        roles = new RoleEnum[1];
        roles[0] = RoleEnum.valueOf("User");
        gradient_rise_time.setRoles(roles);
        gradient_rise_time.setGroup(EnumGroup.valueOf("Delay"));
        gradient_rise_time.setCategory(Category.valueOf("Acquisition"));
        this.addParam(gradient_rise_time);
    }

    private void initImage_orientation_specimen() {
        ListNumberParam image_orientation_specimen = new ListNumberParam();
        image_orientation_specimen.setMinValue(-1.7976931348623157E308);
        image_orientation_specimen.setMaxValue(1.7976931348623157E308);
        image_orientation_specimen.setNumberEnum(NumberEnum.valueOf("Double"));
        List<Number> listValue = new ArrayList<Number>();
        listValue.add(0.0);
        listValue.add(0.0);
        listValue.add(-1.0);
        listValue.add(1.0);
        listValue.add(0.0);
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
        image_orientation_specimen.setLocked(false);
        image_orientation_specimen.setLockedToDefault(false);
        RoleEnum[] roles;
        roles = new RoleEnum[1];
        roles[0] = RoleEnum.valueOf("User");
        image_orientation_specimen.setRoles(roles);
        image_orientation_specimen.setGroup(EnumGroup.valueOf("Dimension"));
        image_orientation_specimen.setCategory(Category.valueOf("Acquisition"));
        this.addParam(image_orientation_specimen);
    }

    private void initImage_position_specimen() {
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
        image_position_specimen.setLocked(false);
        image_position_specimen.setLockedToDefault(false);
        RoleEnum[] roles;
        roles = new RoleEnum[1];
        roles[0] = RoleEnum.valueOf("User");
        image_position_specimen.setRoles(roles);
        image_position_specimen.setGroup(EnumGroup.valueOf("Dimension"));
        image_position_specimen.setCategory(Category.valueOf("Acquisition"));
        this.addParam(image_position_specimen);
    }

    private void initIntermediate_frequency() {
        NumberParam intermediate_frequency = new NumberParam();
        intermediate_frequency.setMinValue(0.0);
        intermediate_frequency.setMaxValue(3.0E9);
        intermediate_frequency.setNumberEnum(NumberEnum.valueOf("Frequency"));
        intermediate_frequency.setDefaultValue(2.3E7);
        intermediate_frequency.setValue(2.3E7);
        intermediate_frequency.setRestrictedToSuggested(false);
        intermediate_frequency.setName("INTERMEDIATE_FREQUENCY");
        intermediate_frequency.setDescription("INTERMEDIATE_FREQUENCY.description");
        intermediate_frequency.setLocked(false);
        intermediate_frequency.setLockedToDefault(false);
        RoleEnum[] roles;
        roles = new RoleEnum[1];
        roles[0] = RoleEnum.valueOf("User");
        intermediate_frequency.setRoles(roles);
        intermediate_frequency.setGroup(EnumGroup.valueOf("Reception"));
        intermediate_frequency.setCategory(Category.valueOf("Acquisition"));
        this.addParam(intermediate_frequency);
    }

    private void initLast_put() {
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
        last_put.setLocked(false);
        last_put.setLockedToDefault(false);
        RoleEnum[] roles;
        roles = new RoleEnum[1];
        roles[0] = RoleEnum.valueOf("User");
        last_put.setRoles(roles);
        last_put.setGroup(EnumGroup.valueOf("Reception"));
        last_put.setCategory(Category.valueOf("Acquisition"));
        this.addParam(last_put);
    }

    private void initMagnetic_field_strength() {
        NumberParam magnetic_field_strength = new NumberParam();
        magnetic_field_strength.setMinValue(0.0);
        magnetic_field_strength.setMaxValue(100.0);
        magnetic_field_strength.setNumberEnum(NumberEnum.valueOf("Field"));
        magnetic_field_strength.setDefaultValue(1.5);
        magnetic_field_strength.setValue(1.5);
        magnetic_field_strength.setRestrictedToSuggested(false);
        magnetic_field_strength.setName("MAGNETIC_FIELD_STRENGTH");
        magnetic_field_strength.setDescription("The magnetic field tregth");
        magnetic_field_strength.setLocked(true);
        magnetic_field_strength.setLockedToDefault(true);
        RoleEnum[] roles;
        roles = new RoleEnum[1];
        roles[0] = RoleEnum.valueOf("User");
        magnetic_field_strength.setRoles(roles);
        magnetic_field_strength.setGroup(EnumGroup.valueOf("Miscellaneous"));
        magnetic_field_strength.setCategory(Category.valueOf("Acquisition"));
        this.addParam(magnetic_field_strength);
    }

    private void initModality() {
        TextParam modality = new TextParam();
        modality.setDefaultValue("MRI");
        modality.setValue("MRI");
        List<String> list23 = new ArrayList<String>();
        list23.add("NMR");
        list23.add("MRI");
        list23.add("DEFAULT");
        modality.setSuggestedValues(list23);
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

    private void initNucleus_1() {
        TextParam nucleus_1 = new TextParam();
        nucleus_1.setDefaultValue("1H");
        nucleus_1.setValue("1H");
        List<String> list24 = new ArrayList<String>();
        list24.add("Other");
        list24.add("Y");
        list24.add("X");
        list24.add("3H");
        list24.add("1H");
        list24.add("19F");
        list24.add("3He");
        list24.add("205Tl");
        list24.add("203Tl");
        list24.add("31P");
        list24.add("7Li");
        list24.add("119Sn");
        list24.add("117Sn");
        list24.add("87Rb");
        list24.add("115Sn");
        list24.add("11B");
        list24.add("125Te");
        list24.add("141Pr");
        list24.add("71Ga");
        list24.add("65Cu");
        list24.add("129Xe");
        list24.add("81Br");
        list24.add("63Cu");
        list24.add("23Na");
        list24.add("51V");
        list24.add("123Te");
        list24.add("27Al");
        list24.add("13C");
        list24.add("79Br");
        list24.add("151Eu");
        list24.add("55Mn");
        list24.add("93Nb");
        list24.add("45Sc");
        list24.add("159Tb");
        list24.add("69Ga");
        list24.add("121Sb");
        list24.add("59Co");
        list24.add("187Re");
        list24.add("185Re");
        list24.add("99Tc");
        list24.add("113Cd");
        list24.add("115In");
        list24.add("113In");
        list24.add("195Pt");
        list24.add("165Ho");
        list24.add("111Cd");
        list24.add("207Pb");
        list24.add("127I");
        list24.add("29Si");
        list24.add("77Se");
        list24.add("199Hg");
        list24.add("171Yb");
        list24.add("75As");
        list24.add("209Bi");
        list24.add("2H");
        list24.add("6Li");
        list24.add("139La");
        list24.add("9Be");
        list24.add("17O");
        list24.add("138La");
        list24.add("133Cs");
        list24.add("123Sb");
        list24.add("181Ta");
        list24.add("175Lu");
        list24.add("137Ba");
        list24.add("153Eu");
        list24.add("10B");
        list24.add("15N");
        list24.add("50V");
        list24.add("135Ba");
        list24.add("35Cl");
        list24.add("85Rb");
        list24.add("91Zr");
        list24.add("61Ni");
        list24.add("169Tm");
        list24.add("131Xe");
        list24.add("37Cl");
        list24.add("176Lu");
        list24.add("21Ne");
        list24.add("189Os");
        list24.add("33S");
        list24.add("14N");
        list24.add("43Ca");
        list24.add("97Mo");
        list24.add("201Hg");
        list24.add("95Mo");
        list24.add("67Zn");
        list24.add("25Mg");
        list24.add("40K");
        list24.add("53Cr");
        list24.add("49Ti");
        list24.add("47Ti");
        list24.add("143Nd");
        list24.add("101Ru");
        list24.add("89Y");
        list24.add("173Yb");
        list24.add("163Dy");
        list24.add("39K");
        list24.add("109Ag");
        list24.add("99Ru");
        list24.add("105Pd");
        list24.add("87Sr");
        list24.add("147Sm");
        list24.add("183W");
        list24.add("107Ag");
        list24.add("157Gd");
        list24.add("177Hf");
        list24.add("83Kr");
        list24.add("73Ge");
        list24.add("149Sm");
        list24.add("161Dy");
        list24.add("145Nd");
        list24.add("57Fe");
        list24.add("103Rh");
        list24.add("155Gd");
        list24.add("167Er");
        list24.add("41K");
        list24.add("179Hf");
        list24.add("187Os");
        list24.add("193Ir");
        list24.add("235U");
        list24.add("197Au");
        list24.add("191Ir");
        nucleus_1.setSuggestedValues(list24);
        nucleus_1.setName("NUCLEUS_1");
        nucleus_1.setDescription("The nucleus used for the first sequence channel");
        nucleus_1.setLocked(false);
        nucleus_1.setLockedToDefault(false);
        RoleEnum[] roles;
        roles = new RoleEnum[1];
        roles[0] = RoleEnum.valueOf("User");
        nucleus_1.setRoles(roles);
        nucleus_1.setGroup(EnumGroup.valueOf("Miscellaneous"));
        nucleus_1.setCategory(Category.valueOf("Acquisition"));
        this.addParam(nucleus_1);
    }

    private void initNucleus_2() {
        TextParam nucleus_2 = new TextParam();
        nucleus_2.setDefaultValue("1H");
        nucleus_2.setValue("1H");
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
        nucleus_2.setSuggestedValues(list25);
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

    private void initNucleus_3() {
        TextParam nucleus_3 = new TextParam();
        nucleus_3.setDefaultValue("1H");
        nucleus_3.setValue("1H");
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
        nucleus_3.setSuggestedValues(list26);
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

    private void initNucleus_4() {
        TextParam nucleus_4 = new TextParam();
        nucleus_4.setDefaultValue("1H");
        nucleus_4.setValue("1H");
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
        nucleus_4.setSuggestedValues(list27);
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

    private void initNumber_of_averages() {
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

    private void initObserved_frequency() {
        NumberParam observed_frequency = new NumberParam();
        observed_frequency.setMinValue(0.0);
        observed_frequency.setMaxValue(3.0E9);
        observed_frequency.setNumberEnum(NumberEnum.valueOf("Frequency"));
        observed_frequency.setDefaultValue(6.3E7);
        observed_frequency.setValue(6.3E7);
        observed_frequency.setRestrictedToSuggested(false);
        observed_frequency.setName("OBSERVED_FREQUENCY");
        observed_frequency.setDescription("The frequency of the acquisition");
        observed_frequency.setLocked(false);
        observed_frequency.setLockedToDefault(false);
        RoleEnum[] roles;
        roles = new RoleEnum[1];
        roles[0] = RoleEnum.valueOf("User");
        observed_frequency.setRoles(roles);
        observed_frequency.setGroup(EnumGroup.valueOf("Reception"));
        observed_frequency.setCategory(Category.valueOf("Acquisition"));
        this.addParam(observed_frequency);
    }

    private void initObserved_nucleus() {
        TextParam observed_nucleus = new TextParam();
        observed_nucleus.setDefaultValue("1H");
        observed_nucleus.setValue("1H");
        List<String> list30 = new ArrayList<String>();
        list30.add("Other");
        list30.add("Y");
        list30.add("X");
        list30.add("3H");
        list30.add("1H");
        list30.add("19F");
        list30.add("3He");
        list30.add("205Tl");
        list30.add("203Tl");
        list30.add("31P");
        list30.add("7Li");
        list30.add("119Sn");
        list30.add("117Sn");
        list30.add("87Rb");
        list30.add("115Sn");
        list30.add("11B");
        list30.add("125Te");
        list30.add("141Pr");
        list30.add("71Ga");
        list30.add("65Cu");
        list30.add("129Xe");
        list30.add("81Br");
        list30.add("63Cu");
        list30.add("23Na");
        list30.add("51V");
        list30.add("123Te");
        list30.add("27Al");
        list30.add("13C");
        list30.add("79Br");
        list30.add("151Eu");
        list30.add("55Mn");
        list30.add("93Nb");
        list30.add("45Sc");
        list30.add("159Tb");
        list30.add("69Ga");
        list30.add("121Sb");
        list30.add("59Co");
        list30.add("187Re");
        list30.add("185Re");
        list30.add("99Tc");
        list30.add("113Cd");
        list30.add("115In");
        list30.add("113In");
        list30.add("195Pt");
        list30.add("165Ho");
        list30.add("111Cd");
        list30.add("207Pb");
        list30.add("127I");
        list30.add("29Si");
        list30.add("77Se");
        list30.add("199Hg");
        list30.add("171Yb");
        list30.add("75As");
        list30.add("209Bi");
        list30.add("2H");
        list30.add("6Li");
        list30.add("139La");
        list30.add("9Be");
        list30.add("17O");
        list30.add("138La");
        list30.add("133Cs");
        list30.add("123Sb");
        list30.add("181Ta");
        list30.add("175Lu");
        list30.add("137Ba");
        list30.add("153Eu");
        list30.add("10B");
        list30.add("15N");
        list30.add("50V");
        list30.add("135Ba");
        list30.add("35Cl");
        list30.add("85Rb");
        list30.add("91Zr");
        list30.add("61Ni");
        list30.add("169Tm");
        list30.add("131Xe");
        list30.add("37Cl");
        list30.add("176Lu");
        list30.add("21Ne");
        list30.add("189Os");
        list30.add("33S");
        list30.add("14N");
        list30.add("43Ca");
        list30.add("97Mo");
        list30.add("201Hg");
        list30.add("95Mo");
        list30.add("67Zn");
        list30.add("25Mg");
        list30.add("40K");
        list30.add("53Cr");
        list30.add("49Ti");
        list30.add("47Ti");
        list30.add("143Nd");
        list30.add("101Ru");
        list30.add("89Y");
        list30.add("173Yb");
        list30.add("163Dy");
        list30.add("39K");
        list30.add("109Ag");
        list30.add("99Ru");
        list30.add("105Pd");
        list30.add("87Sr");
        list30.add("147Sm");
        list30.add("183W");
        list30.add("107Ag");
        list30.add("157Gd");
        list30.add("177Hf");
        list30.add("83Kr");
        list30.add("73Ge");
        list30.add("149Sm");
        list30.add("161Dy");
        list30.add("145Nd");
        list30.add("57Fe");
        list30.add("103Rh");
        list30.add("155Gd");
        list30.add("167Er");
        list30.add("41K");
        list30.add("179Hf");
        list30.add("187Os");
        list30.add("193Ir");
        list30.add("235U");
        list30.add("197Au");
        list30.add("191Ir");
        observed_nucleus.setSuggestedValues(list30);
        observed_nucleus.setName("OBSERVED_NUCLEUS");
        observed_nucleus.setDescription("The observed nucleus");
        observed_nucleus.setLocked(false);
        observed_nucleus.setLockedToDefault(false);
        RoleEnum[] roles;
        roles = new RoleEnum[1];
        roles[0] = RoleEnum.valueOf("SequenceDesigner");
        observed_nucleus.setRoles(roles);
        observed_nucleus.setGroup(EnumGroup.valueOf("Reception"));
        observed_nucleus.setCategory(Category.valueOf("Acquisition"));
        this.addParam(observed_nucleus);
    }

    private void initOff_center_field_of_view_1d() {
        NumberParam off_center_field_of_view_1d = new NumberParam();
        off_center_field_of_view_1d.setMinValue(-1.7976931348623157E308);
        off_center_field_of_view_1d.setMaxValue(1.7976931348623157E308);
        off_center_field_of_view_1d.setNumberEnum(NumberEnum.valueOf("Location"));
        off_center_field_of_view_1d.setDefaultValue(0.0);
        off_center_field_of_view_1d.setValue(0.0);
        off_center_field_of_view_1d.setRestrictedToSuggested(false);
        off_center_field_of_view_1d.setName("OFF_CENTER_FIELD_OF_VIEW_1D");
        off_center_field_of_view_1d.setDescription("The offcenter location of the Field of view in the first dimention");
        off_center_field_of_view_1d.setLocked(false);
        off_center_field_of_view_1d.setLockedToDefault(false);
        RoleEnum[] roles;
        roles = new RoleEnum[1];
        roles[0] = RoleEnum.valueOf("User");
        off_center_field_of_view_1d.setRoles(roles);
        off_center_field_of_view_1d.setGroup(EnumGroup.valueOf("Gradient"));
        off_center_field_of_view_1d.setCategory(Category.valueOf("Acquisition"));
        this.addParam(off_center_field_of_view_1d);
    }

    private void initOff_center_field_of_view_2d() {
        NumberParam off_center_field_of_view_2d = new NumberParam();
        off_center_field_of_view_2d.setMinValue(-1.7976931348623157E308);
        off_center_field_of_view_2d.setMaxValue(1.7976931348623157E308);
        off_center_field_of_view_2d.setNumberEnum(NumberEnum.valueOf("Location"));
        off_center_field_of_view_2d.setDefaultValue(0.0);
        off_center_field_of_view_2d.setValue(0.0);
        off_center_field_of_view_2d.setRestrictedToSuggested(false);
        off_center_field_of_view_2d.setName("OFF_CENTER_FIELD_OF_VIEW_2D");
        off_center_field_of_view_2d.setDescription("The offcenter location of the Field of view in the second dimention");
        off_center_field_of_view_2d.setLocked(false);
        off_center_field_of_view_2d.setLockedToDefault(false);
        RoleEnum[] roles;
        roles = new RoleEnum[1];
        roles[0] = RoleEnum.valueOf("User");
        off_center_field_of_view_2d.setRoles(roles);
        off_center_field_of_view_2d.setGroup(EnumGroup.valueOf("Gradient"));
        off_center_field_of_view_2d.setCategory(Category.valueOf("Acquisition"));
        this.addParam(off_center_field_of_view_2d);
    }

    private void initOff_center_field_of_view_3d() {
        NumberParam off_center_field_of_view_3d = new NumberParam();
        off_center_field_of_view_3d.setMinValue(-1.7976931348623157E308);
        off_center_field_of_view_3d.setMaxValue(1.7976931348623157E308);
        off_center_field_of_view_3d.setNumberEnum(NumberEnum.valueOf("Location"));
        off_center_field_of_view_3d.setDefaultValue(0.0);
        off_center_field_of_view_3d.setValue(0.0);
        off_center_field_of_view_3d.setRestrictedToSuggested(false);
        off_center_field_of_view_3d.setName("OFF_CENTER_FIELD_OF_VIEW_3D");
        off_center_field_of_view_3d.setDescription("The offcenter location of the Field of view in the third dimention");
        off_center_field_of_view_3d.setLocked(false);
        off_center_field_of_view_3d.setLockedToDefault(false);
        RoleEnum[] roles;
        roles = new RoleEnum[1];
        roles[0] = RoleEnum.valueOf("User");
        off_center_field_of_view_3d.setRoles(roles);
        off_center_field_of_view_3d.setGroup(EnumGroup.valueOf("Gradient"));
        off_center_field_of_view_3d.setCategory(Category.valueOf("Acquisition"));
        this.addParam(off_center_field_of_view_3d);
    }

    private void initOffset_freq_1() {
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

    private void initOffset_freq_2() {
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

    private void initOffset_freq_3() {
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

    private void initOffset_freq_4() {
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

    private void initOrientation() {
        TextParam orientation = new TextParam();
        orientation.setDefaultValue("AXIAL");
        orientation.setValue("CORONAL");
        List<String> list38 = new ArrayList<String>();
        list38.add("AXIAL");
        list38.add("CORONAL");
        list38.add("SAGITTAL");
        list38.add("OBLIQUE");
        orientation.setSuggestedValues(list38);
        orientation.setName("ORIENTATION");
        orientation.setDescription("Field of view orientation");
        orientation.setLocked(false);
        orientation.setLockedToDefault(false);
        RoleEnum[] roles;
        roles = new RoleEnum[1];
        roles[0] = RoleEnum.valueOf("User");
        orientation.setRoles(roles);
        orientation.setGroup(EnumGroup.valueOf("Dimension"));
        orientation.setCategory(Category.valueOf("Acquisition"));
        this.addParam(orientation);
    }

    private void initParopt_param() {
        TextParam paropt_param = new TextParam();
        paropt_param.setDefaultValue("PULSE_LENGTH");
        paropt_param.setValue("");
        paropt_param.setName("PAROPT_PARAM");
        paropt_param.setDescription("Name of the current optimised parameter");
        paropt_param.setLocked(true);
        paropt_param.setLockedToDefault(false);
        RoleEnum[] roles;
        roles = new RoleEnum[1];
        roles[0] = RoleEnum.valueOf("User");
        paropt_param.setRoles(roles);
        paropt_param.setGroup(EnumGroup.valueOf("Miscellaneous"));
        paropt_param.setCategory(Category.valueOf("Acquisition"));
        this.addParam(paropt_param);
    }

    private void initPhase_cycling() {
        BooleanParam phase_cycling = new BooleanParam();
        phase_cycling.setDefaultValue(true);
        phase_cycling.setValue(false);
        phase_cycling.setName("PHASE_CYCLING");
        phase_cycling.setDescription("Enable the phase cycling in the sequence");
        phase_cycling.setLocked(false);
        phase_cycling.setLockedToDefault(false);
        RoleEnum[] roles;
        roles = new RoleEnum[1];
        roles[0] = RoleEnum.valueOf("SequenceDesigner");
        phase_cycling.setRoles(roles);
        phase_cycling.setGroup(EnumGroup.valueOf("Reception"));
        phase_cycling.setCategory(Category.valueOf("Acquisition"));
        this.addParam(phase_cycling);
    }

    private void initProbe() {
        TextParam probe = new TextParam();
        probe.setDefaultValue("");
        probe.setValue("");
        probe.setName("PROBE");
        probe.setDescription("The probe used for the mr acquisition");
        probe.setLocked(false);
        probe.setLockedToDefault(false);
        RoleEnum[] roles;
        roles = new RoleEnum[1];
        roles[0] = RoleEnum.valueOf("User");
        probe.setRoles(roles);
        probe.setGroup(EnumGroup.valueOf("Miscellaneous"));
        probe.setCategory(Category.valueOf("Acquisition"));
        this.addParam(probe);
    }

    private void initPulse_att() {
        NumberParam pulse_att = new NumberParam();
        pulse_att.setMinValue(0.0);
        pulse_att.setMaxValue(63.0);
        pulse_att.setNumberEnum(NumberEnum.valueOf("TxAtt"));
        pulse_att.setDefaultValue(26.0);
        pulse_att.setValue(20.0);
        pulse_att.setRestrictedToSuggested(false);
        pulse_att.setName("PULSE_ATT");
        pulse_att.setDescription("Transmitter attenuation");
        pulse_att.setLocked(false);
        pulse_att.setLockedToDefault(false);
        RoleEnum[] roles;
        roles = new RoleEnum[1];
        roles[0] = RoleEnum.valueOf("User");
        pulse_att.setRoles(roles);
        pulse_att.setGroup(EnumGroup.valueOf("Emission"));
        pulse_att.setCategory(Category.valueOf("Acquisition"));
        this.addParam(pulse_att);
    }

    private void initReceiver_count() {
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

    private void initReceiver_gain() {
        NumberParam receiver_gain = new NumberParam();
        receiver_gain.setMinValue(0.0);
        receiver_gain.setMaxValue(120.0);
        receiver_gain.setNumberEnum(NumberEnum.valueOf("RxGain"));
        receiver_gain.setDefaultValue(10.0);
        receiver_gain.setValue(10.0);
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

    private void initRepetition_time() {
        NumberParam repetition_time = new NumberParam();
        repetition_time.setMinValue(0.0);
        repetition_time.setMaxValue(1.0E9);
        repetition_time.setNumberEnum(NumberEnum.valueOf("Time"));
        repetition_time.setDefaultValue(0.2);
        repetition_time.setValue(0.1);
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

    private void initResolution_frequency() {
        NumberParam resolution_frequency = new NumberParam();
        resolution_frequency.setMinValue(0.0);
        resolution_frequency.setMaxValue(10.0);
        resolution_frequency.setNumberEnum(NumberEnum.valueOf("Length"));
        resolution_frequency.setDefaultValue(0.5);
        resolution_frequency.setValue(2.5E-4);
        resolution_frequency.setRestrictedToSuggested(false);
        resolution_frequency.setName("RESOLUTION_FREQUENCY");
        resolution_frequency.setDescription("Pixel dimension in the frequency encoding direction");
        resolution_frequency.setLocked(false);
        resolution_frequency.setLockedToDefault(false);
        RoleEnum[] roles;
        roles = new RoleEnum[1];
        roles[0] = RoleEnum.valueOf("User");
        resolution_frequency.setRoles(roles);
        resolution_frequency.setGroup(EnumGroup.valueOf("Dimension"));
        resolution_frequency.setCategory(Category.valueOf("Acquisition"));
        this.addParam(resolution_frequency);
    }

    private void initSequence_name() {
        TextParam sequence_name = new TextParam();
        sequence_name.setDefaultValue("FastRFCalib_dev");
        sequence_name.setValue("FastRFCalib_dev");
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

    private void initSetup_mode() {
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

    private void initSlice_thickness() {
        NumberParam slice_thickness = new NumberParam();
        slice_thickness.setMinValue(5.0E-5);
        slice_thickness.setMaxValue(1.7976931348623157E308);
        slice_thickness.setNumberEnum(NumberEnum.valueOf("Length"));
        slice_thickness.setDefaultValue(0.005);
        slice_thickness.setValue(0.002);
        slice_thickness.setRestrictedToSuggested(false);
        slice_thickness.setName("SLICE_THICKNESS");
        slice_thickness.setDescription("The slice thickness");
        slice_thickness.setLocked(false);
        slice_thickness.setLockedToDefault(false);
        RoleEnum[] roles;
        roles = new RoleEnum[1];
        roles[0] = RoleEnum.valueOf("User");
        slice_thickness.setRoles(roles);
        slice_thickness.setGroup(EnumGroup.valueOf("Dimension"));
        slice_thickness.setCategory(Category.valueOf("Acquisition"));
        this.addParam(slice_thickness);
    }

    private void initSpacing_between_slice() {
        NumberParam spacing_between_slice = new NumberParam();
        spacing_between_slice.setMinValue(0.0);
        spacing_between_slice.setMaxValue(1.7976931348623157E308);
        spacing_between_slice.setNumberEnum(NumberEnum.valueOf("Length"));
        spacing_between_slice.setDefaultValue(5.0);
        spacing_between_slice.setValue(0.002);
        spacing_between_slice.setRestrictedToSuggested(false);
        spacing_between_slice.setName("SPACING_BETWEEN_SLICE");
        spacing_between_slice.setDescription("Spacing betwin slice");
        spacing_between_slice.setLocked(false);
        spacing_between_slice.setLockedToDefault(false);
        RoleEnum[] roles;
        roles = new RoleEnum[1];
        roles[0] = RoleEnum.valueOf("User");
        spacing_between_slice.setRoles(roles);
        spacing_between_slice.setGroup(EnumGroup.valueOf("Dimension"));
        spacing_between_slice.setCategory(Category.valueOf("Acquisition"));
        this.addParam(spacing_between_slice);
    }

    private void initSpecimen_position() {
        TextParam specimen_position = new TextParam();
        specimen_position.setDefaultValue("FeetFirstProne");
        specimen_position.setValue("FeetFirstProne");
        List<String> list51 = new ArrayList<String>();
        list51.add("HeadFirstProne");
        list51.add("HeadFirstSupine");
        list51.add("FeetFirstProne");
        list51.add("FeetFirstSupine");
        specimen_position.setSuggestedValues(list51);
        specimen_position.setName("SPECIMEN_POSITION");
        specimen_position.setDescription("Specimen position descriptor relative to the magnet.");
        specimen_position.setLocked(false);
        specimen_position.setLockedToDefault(false);
        RoleEnum[] roles;
        roles = new RoleEnum[1];
        roles[0] = RoleEnum.valueOf("User");
        specimen_position.setRoles(roles);
        specimen_position.setGroup(EnumGroup.valueOf("Dimension"));
        specimen_position.setCategory(Category.valueOf("Acquisition"));
        this.addParam(specimen_position);
    }

    private void initSpectral_width() {
        NumberParam spectral_width = new NumberParam();
        spectral_width.setMinValue(0.0);
        spectral_width.setMaxValue(1.0E8);
        spectral_width.setNumberEnum(NumberEnum.valueOf("SW"));
        spectral_width.setDefaultValue(12500.0);
        spectral_width.setValue(40000.0);
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

    private void initSquare_pixel() {
        BooleanParam square_pixel = new BooleanParam();
        square_pixel.setDefaultValue(false);
        square_pixel.setValue(false);
        square_pixel.setName("SQUARE_PIXEL");
        square_pixel.setDescription("Same pixel dimension in frequency and phase encoding direction, this will change Phase FOV");
        square_pixel.setLocked(false);
        square_pixel.setLockedToDefault(false);
        RoleEnum[] roles;
        roles = new RoleEnum[1];
        roles[0] = RoleEnum.valueOf("User");
        square_pixel.setRoles(roles);
        square_pixel.setGroup(EnumGroup.valueOf("Dimension"));
        square_pixel.setCategory(Category.valueOf("Acquisition"));
        this.addParam(square_pixel);
    }

    private void initSynchro_enabled() {
        BooleanParam synchro_enabled = new BooleanParam();
        synchro_enabled.setDefaultValue(false);
        synchro_enabled.setValue(false);
        synchro_enabled.setName("SYNCHRO_ENABLED");
        synchro_enabled.setDescription("Enable the synchronization of the sequence with the trigger");
        synchro_enabled.setLocked(false);
        synchro_enabled.setLockedToDefault(false);
        RoleEnum[] roles;
        roles = new RoleEnum[1];
        roles[0] = RoleEnum.valueOf("User");
        synchro_enabled.setRoles(roles);
        synchro_enabled.setGroup(EnumGroup.valueOf("Miscellaneous"));
        synchro_enabled.setCategory(Category.valueOf("Acquisition"));
        this.addParam(synchro_enabled);
    }

    private void initTransform_plugin() {
        TextParam transform_plugin = new TextParam();
        transform_plugin.setDefaultValue("none");
        transform_plugin.setValue("Bordered2DJump3D");
        List<String> list55 = new ArrayList<String>();
        list55.add("Bordered2DJump3D");
        list55.add("Bordered2D");
        list55.add("Bordered3D");
        list55.add("CardiacTransform");
        list55.add("Centered2DJump3D");
        list55.add("Centered2D");
        list55.add("Centered3D");
        list55.add("EPIInterleaved2D");
        list55.add("EPISequential2D");
        list55.add("EPISequential4D");
        list55.add("Interleaved2D");
        list55.add("Interleaved4D");
        list55.add("Jump3DSequential4D");
        list55.add("Jump3D");
        list55.add("Sequential2D");
        list55.add("Sequential3D");
        list55.add("Sequential4D");
        transform_plugin.setSuggestedValues(list55);
        transform_plugin.setName("TRANSFORM_PLUGIN");
        transform_plugin.setDescription("Transform the acquisition space to the k space");
        transform_plugin.setLocked(false);
        transform_plugin.setLockedToDefault(false);
        RoleEnum[] roles;
        roles = new RoleEnum[1];
        roles[0] = RoleEnum.valueOf("SequenceDesigner");
        transform_plugin.setRoles(roles);
        transform_plugin.setGroup(EnumGroup.valueOf("Scan"));
        transform_plugin.setCategory(Category.valueOf("Acquisition"));
        this.addParam(transform_plugin);
    }

    private void initTx_amp_90() {
        NumberParam tx_amp_90 = new NumberParam();
        tx_amp_90.setMinValue(0.0);
        tx_amp_90.setMaxValue(100.0);
        tx_amp_90.setNumberEnum(NumberEnum.valueOf("TxAmp"));
        tx_amp_90.setDefaultValue(0.0);
        tx_amp_90.setValue(20.0);
        tx_amp_90.setRestrictedToSuggested(false);
        tx_amp_90.setName("TX_AMP_90");
        tx_amp_90.setDescription("Amplitude of the transmitter");
        tx_amp_90.setLocked(false);
        tx_amp_90.setLockedToDefault(false);
        RoleEnum[] roles;
        roles = new RoleEnum[1];
        roles[0] = RoleEnum.valueOf("User");
        tx_amp_90.setRoles(roles);
        tx_amp_90.setCategory(Category.valueOf("Acquisition"));
        this.addParam(tx_amp_90);
    }

    private void initTx_bandwidth_factor() {
        NumberParam tx_bandwidth_factor = new NumberParam();
        tx_bandwidth_factor.setMinValue(-1.7976931348623157E308);
        tx_bandwidth_factor.setMaxValue(1.7976931348623157E308);
        tx_bandwidth_factor.setNumberEnum(NumberEnum.valueOf("Double"));
        tx_bandwidth_factor.setDefaultValue(1.0);
        tx_bandwidth_factor.setValue(1.0);
        tx_bandwidth_factor.setRestrictedToSuggested(false);
        tx_bandwidth_factor.setName("TX_BANDWIDTH_FACTOR");
        tx_bandwidth_factor.setDescription("The bandwidth factor of the RF pulse");
        tx_bandwidth_factor.setLocked(false);
        tx_bandwidth_factor.setLockedToDefault(false);
        RoleEnum[] roles;
        roles = new RoleEnum[1];
        roles[0] = RoleEnum.valueOf("User");
        tx_bandwidth_factor.setRoles(roles);
        tx_bandwidth_factor.setGroup(EnumGroup.valueOf("Emission"));
        tx_bandwidth_factor.setCategory(Category.valueOf("Acquisition"));
        this.addParam(tx_bandwidth_factor);
    }

    private void initTx_length_90() {
        NumberParam tx_length_90 = new NumberParam();
        tx_length_90.setMinValue(0.0);
        tx_length_90.setMaxValue(1.0E9);
        tx_length_90.setNumberEnum(NumberEnum.valueOf("Time"));
        tx_length_90.setDefaultValue(0.0);
        tx_length_90.setValue(0.001);
        tx_length_90.setRestrictedToSuggested(false);
        tx_length_90.setName("TX_LENGTH_90");
        tx_length_90.setDescription("length of RF pulse");
        tx_length_90.setLocked(false);
        tx_length_90.setLockedToDefault(false);
        RoleEnum[] roles;
        roles = new RoleEnum[1];
        roles[0] = RoleEnum.valueOf("User");
        tx_length_90.setRoles(roles);
        tx_length_90.setGroup(EnumGroup.valueOf("Emission"));
        tx_length_90.setCategory(Category.valueOf("Acquisition"));
        this.addParam(tx_length_90);
    }

    private void initTx_route() {
        ListNumberParam tx_route = new ListNumberParam();
        tx_route.setMinValue(-2147483648);
        tx_route.setMaxValue(2147483647);
        tx_route.setNumberEnum(NumberEnum.valueOf("Integer"));
        List<Number> listValue = new ArrayList<Number>();
        listValue.add(0);
        tx_route.setValue(listValue);
        tx_route.setName("TX_ROUTE");
        tx_route.setDescription("LogCh->PhysCh");
        tx_route.setLocked(false);
        tx_route.setLockedToDefault(false);
        RoleEnum[] roles;
        roles = new RoleEnum[1];
        roles[0] = RoleEnum.valueOf("User");
        tx_route.setRoles(roles);
        tx_route.setGroup(EnumGroup.valueOf("Emission"));
        tx_route.setCategory(Category.valueOf("Acquisition"));
        this.addParam(tx_route);
    }

    private void initTx_shape_90() {
        TextParam tx_shape_90 = new TextParam();
        tx_shape_90.setDefaultValue("HARD");
        tx_shape_90.setValue("GAUSSIAN");
        List<String> list60 = new ArrayList<String>();
        list60.add("HARD");
        list60.add("GAUSSIAN");
        list60.add("SINC3");
        list60.add("SINC5");
        tx_shape_90.setSuggestedValues(list60);
        tx_shape_90.setName("TX_SHAPE_90");
        tx_shape_90.setDescription("the shape of the rf pulse");
        tx_shape_90.setLocked(false);
        tx_shape_90.setLockedToDefault(false);
        RoleEnum[] roles;
        roles = new RoleEnum[2];
        roles[0] = RoleEnum.valueOf("Manager");
        roles[1] = RoleEnum.valueOf("SequenceDesigner");
        tx_shape_90.setRoles(roles);
        tx_shape_90.setGroup(EnumGroup.valueOf("Emission"));
        tx_shape_90.setCategory(Category.valueOf("Acquisition"));
        this.addParam(tx_shape_90);
    }

    private void initUser_matrix_dimension_1d() {
        NumberParam user_matrix_dimension_1d = new NumberParam();
        user_matrix_dimension_1d.setMinValue(1);
        user_matrix_dimension_1d.setMaxValue(65536);
        user_matrix_dimension_1d.setNumberEnum(NumberEnum.valueOf("Scan"));
        user_matrix_dimension_1d.setDefaultValue(1);
        user_matrix_dimension_1d.setValue(512);
        user_matrix_dimension_1d.setRestrictedToSuggested(false);
        user_matrix_dimension_1d.setName("USER_MATRIX_DIMENSION_1D");
        user_matrix_dimension_1d.setDescription("");
        user_matrix_dimension_1d.setLocked(false);
        user_matrix_dimension_1d.setLockedToDefault(false);
        RoleEnum[] roles;
        roles = new RoleEnum[1];
        roles[0] = RoleEnum.valueOf("User");
        user_matrix_dimension_1d.setRoles(roles);
        user_matrix_dimension_1d.setGroup(EnumGroup.valueOf("User"));
        user_matrix_dimension_1d.setCategory(Category.valueOf("Acquisition"));
        this.addParam(user_matrix_dimension_1d);
    }

    private void initUser_matrix_dimension_2d() {
        NumberParam user_matrix_dimension_2d = new NumberParam();
        user_matrix_dimension_2d.setMinValue(1);
        user_matrix_dimension_2d.setMaxValue(65536);
        user_matrix_dimension_2d.setNumberEnum(NumberEnum.valueOf("Scan"));
        user_matrix_dimension_2d.setDefaultValue(1);
        user_matrix_dimension_2d.setValue(1);
        user_matrix_dimension_2d.setRestrictedToSuggested(false);
        user_matrix_dimension_2d.setName("USER_MATRIX_DIMENSION_2D");
        user_matrix_dimension_2d.setDescription("");
        user_matrix_dimension_2d.setLocked(false);
        user_matrix_dimension_2d.setLockedToDefault(false);
        RoleEnum[] roles;
        roles = new RoleEnum[1];
        roles[0] = RoleEnum.valueOf("User");
        user_matrix_dimension_2d.setRoles(roles);
        user_matrix_dimension_2d.setGroup(EnumGroup.valueOf("User"));
        user_matrix_dimension_2d.setCategory(Category.valueOf("Acquisition"));
        this.addParam(user_matrix_dimension_2d);
    }

    private void initUser_matrix_dimension_3d() {
        NumberParam user_matrix_dimension_3d = new NumberParam();
        user_matrix_dimension_3d.setMinValue(1);
        user_matrix_dimension_3d.setMaxValue(65536);
        user_matrix_dimension_3d.setNumberEnum(NumberEnum.valueOf("Scan"));
        user_matrix_dimension_3d.setDefaultValue(1);
        user_matrix_dimension_3d.setValue(1);
        user_matrix_dimension_3d.setRestrictedToSuggested(false);
        user_matrix_dimension_3d.setName("USER_MATRIX_DIMENSION_3D");
        user_matrix_dimension_3d.setDescription("");
        user_matrix_dimension_3d.setLocked(false);
        user_matrix_dimension_3d.setLockedToDefault(false);
        RoleEnum[] roles;
        roles = new RoleEnum[1];
        roles[0] = RoleEnum.valueOf("User");
        user_matrix_dimension_3d.setRoles(roles);
        user_matrix_dimension_3d.setGroup(EnumGroup.valueOf("User"));
        user_matrix_dimension_3d.setCategory(Category.valueOf("Acquisition"));
        this.addParam(user_matrix_dimension_3d);
    }

    private void initUser_matrix_dimension_4d() {
        NumberParam user_matrix_dimension_4d = new NumberParam();
        user_matrix_dimension_4d.setMinValue(1);
        user_matrix_dimension_4d.setMaxValue(65536);
        user_matrix_dimension_4d.setNumberEnum(NumberEnum.valueOf("Scan"));
        user_matrix_dimension_4d.setDefaultValue(1);
        user_matrix_dimension_4d.setValue(1);
        user_matrix_dimension_4d.setRestrictedToSuggested(false);
        user_matrix_dimension_4d.setName("USER_MATRIX_DIMENSION_4D");
        user_matrix_dimension_4d.setDescription("");
        user_matrix_dimension_4d.setLocked(false);
        user_matrix_dimension_4d.setLockedToDefault(false);
        RoleEnum[] roles;
        roles = new RoleEnum[1];
        roles[0] = RoleEnum.valueOf("User");
        user_matrix_dimension_4d.setRoles(roles);
        user_matrix_dimension_4d.setGroup(EnumGroup.valueOf("User"));
        user_matrix_dimension_4d.setCategory(Category.valueOf("Acquisition"));
        this.addParam(user_matrix_dimension_4d);
    }

    private void initUser_zero_filling_2d() {
        NumberParam user_zero_filling_2d = new NumberParam();
        user_zero_filling_2d.setMinValue(0.0);
        user_zero_filling_2d.setMaxValue(100.0);
        user_zero_filling_2d.setNumberEnum(NumberEnum.valueOf("PERCENT"));
        user_zero_filling_2d.setDefaultValue(0.0);
        user_zero_filling_2d.setValue(0.0);
        user_zero_filling_2d.setRestrictedToSuggested(false);
        user_zero_filling_2d.setName("USER_ZERO_FILLING_2D");
        user_zero_filling_2d.setDescription("Percentage of zero filling");
        user_zero_filling_2d.setLocked(true);
        user_zero_filling_2d.setLockedToDefault(false);
        RoleEnum[] roles;
        roles = new RoleEnum[1];
        roles[0] = RoleEnum.valueOf("User");
        user_zero_filling_2d.setRoles(roles);
        user_zero_filling_2d.setGroup(EnumGroup.valueOf("Scan"));
        user_zero_filling_2d.setCategory(Category.valueOf("Acquisition"));
        this.addParam(user_zero_filling_2d);
    }

    private void initUser_zero_filling_3d() {
        NumberParam user_zero_filling_3d = new NumberParam();
        user_zero_filling_3d.setMinValue(0.0);
        user_zero_filling_3d.setMaxValue(100.0);
        user_zero_filling_3d.setNumberEnum(NumberEnum.valueOf("PERCENT"));
        user_zero_filling_3d.setDefaultValue(0.0);
        user_zero_filling_3d.setValue(0.0);
        user_zero_filling_3d.setRestrictedToSuggested(false);
        user_zero_filling_3d.setName("USER_ZERO_FILLING_3D");
        user_zero_filling_3d.setDescription("Percentage of zero filing");
        user_zero_filling_3d.setLocked(true);
        user_zero_filling_3d.setLockedToDefault(false);
        RoleEnum[] roles;
        roles = new RoleEnum[1];
        roles[0] = RoleEnum.valueOf("User");
        user_zero_filling_3d.setRoles(roles);
        user_zero_filling_3d.setGroup(EnumGroup.valueOf("Scan"));
        user_zero_filling_3d.setCategory(Category.valueOf("Acquisition"));
        this.addParam(user_zero_filling_3d);
    }

    public float getVersion() {
        return 0.01f;
    }

    public String getName() {
        return "FastRFCalib_dev";
    }// </editor-fold>

//GEN
}
