package rs2d.process;

import rs2d.spinlab.data.Data;
import rs2d.spinlab.data.Header;
import rs2d.spinlab.plugins.process.ProcessPluginAbstract;
import rs2d.spinlab.tools.param.BooleanParam;
import rs2d.spinlab.tools.param.DataState;
import rs2d.spinlab.tools.param.DefaultParams;
import rs2d.spinlab.tools.param.Param;
import rs2d.spinlab.tools.utility.MathUtility;
import rs2d.spinlab.tools.utility.maths.ComplexDataUtil;

public class FFT extends ProcessPluginAbstract {
    //
    // Constructors
    //

    public FFT() {
        super("FFT", "Fast Fourier Transform");
    }

    //
    // Implementation of ProcessPlugin
    //

    @Override
    public Param[] getParam() {
        BooleanParam withShift = new BooleanParam("Shift", true, "Shift data after fft");
        BooleanParam sweepData = new BooleanParam("Sweep data", true, "Sweep data to fit nmr standard");
        BooleanParam phaseCorrection = new BooleanParam("Phase Correction", false, "Applied order 1 phase correction");
        return new Param[]{withShift, sweepData, phaseCorrection};
    }

    @Override
    public Param[] execute(Param... params) {
        this.checkParameters(params);
        boolean shift = ((Boolean) params[0].getValue());
        boolean sweep = ((Boolean) params[1].getValue());
        boolean phase = ((Boolean) params[2].getValue());
        this.executeFFT(shift, sweep, phase);
        return new Param[0];
    }

    //
    // FFT process
    //

    /**
     * Execute the FFT process.
     * @param shift Flag indicating if shift is needed after FFT.
     * @param sweep Flag indicating if data must be swept to fit NMR standard.
     * @param phase Flag indicating if phase correction is applied.
     */
    private void executeFFT(boolean shift, boolean sweep, boolean phase) {
        Header header = this.getDataset().getHeader();
        int receiversCount = ((Number) header.getParam(DefaultParams.RECEIVER_COUNT.name()).getValue()).intValue();
        int matrix4D = ((Number) header.getParam(DefaultParams.MATRIX_DIMENSION_4D.name()).getValue()).intValue();
        int matrix3D = ((Number) header.getParam(DefaultParams.MATRIX_DIMENSION_3D.name()).getValue()).intValue();
        int matrix2D = ((Number) header.getParam(DefaultParams.MATRIX_DIMENSION_2D.name()).getValue()).intValue();

        int digitalPhase = header.hasParam(DefaultParams.DIGITAL_FILTER_SHIFT.name()) ?
                header.getNumberParam(DefaultParams.DIGITAL_FILTER_SHIFT.name()).getValue().intValue() : 0;

        float applied = (float) (digitalPhase * 2f * Math.PI);

        boolean shiftData = header.hasParam(DefaultParams.DIGITAL_FILTER_REMOVED.name()) ?
                header.getBooleanParam(DefaultParams.DIGITAL_FILTER_REMOVED).getValue() : false;


        int finalDataSize = header.getNumberParam(DefaultParams.MATRIX_DIMENSION_1D).getValue().intValue();

        for (int r = 0; r < receiversCount; r++) {
            Data data = this.getDataset().getData(r);
            finalDataSize = FFT.fillToNextPowerOf2(data);

            float[][][][] real = data.getRealPart();
            float[][][][] imag = data.getImaginaryPart();

            for (int l = 0; l < matrix4D; l++) {
                for (int k = 0; k < matrix3D; k++) {
                    for (int j = 0; j < matrix2D; j++) {
                        this.executeFFTOnArray(real[l][k][j], imag[l][k][j], phase, shift, shiftData, applied, sweep, digitalPhase);
                    }
                }
            }
        }

        header.getNumberParam(DefaultParams.MATRIX_DIMENSION_1D.name()).setValue(finalDataSize);
        inverseState(header);
    }

    /**
     * Fill the data with zeros until data size is the next power of 2.
     * @param data Data to fill-in.
     * @return The final data size, either the initial size if it is already a power of 2, or the next one.
     */
    static int fillToNextPowerOf2(Data data) {
        float[][][][] real = data.getRealPart();
        float[][][][] imag = data.getImaginaryPart();

        if (!MathUtility.isPowerOf2(real[0][0][0].length)) {
            int nextPowerOf2 = MathUtility.nextPowerOf2(real[0][0][0].length);

            float[][][][] newReal = new float[real.length][real[0].length][real[0][0].length][nextPowerOf2];
            float[][][][] newImag = new float[imag.length][imag[0].length][imag[0][0].length][nextPowerOf2];

            for (int l = 0; l < real.length; l++) {
                for (int k = 0; k < real[0].length; k++) {
                    for (int j = 0; j < real[0][0].length; j++) {
                        System.arraycopy(real[l][k][j], 0, newReal[l][k][j], 0, real[l][k][j].length);
                        System.arraycopy(imag[l][k][j], 0, newImag[l][k][j], 0, imag[l][k][j].length);
                    }
                }
            }

            data.setRealPart(newReal);
            data.setImaginaryPart(newImag);
        }

        return data.getRealPart()[0][0][0].length;
    }

    /**
     * Execute the FFT process on an array, eventually with shifting and phase correction.
     * @param real Input real data.
     * @param imag Input imaginary data.
     * @param phase Flag indicating if the phase will be corrected or not.
     * @param shift Flag indicating if the data will be shifted or not.
     * @param shiftData Flag indicating a horizontal shifting (only if phase correction is activated).
     * @param applied Calculated value of the Phase 1 for phase correction.
     * @param sweep Flag indicating if the data will be swept or not.
     * @param digitalPhase Value of the digital phase.
     */
    private void executeFFTOnArray(float[] real, float[] imag, boolean phase, boolean shift, boolean shiftData, float applied, boolean sweep,
                                   int digitalPhase) {
        if (phase && shiftData) {
            MathUtility.matrixHorizontalshift(real, imag, digitalPhase, false);
        }

        MathUtility.executeFFT(real, imag, shift, sweep);

        if (phase) {
            ComplexDataUtil.phaseShift(real, imag, 0, -applied);
        }
    }

    //--

    public static void inverseState(Header header) {
        if (!header.hasParam(DefaultParams.STATE.name())) {
            DefaultParams defaultParams = new DefaultParams();
            Param param = defaultParams.getParam(DefaultParams.STATE);
            header.putParam(param);
        }

        int state = header.getListNumberParam(DefaultParams.STATE)
            .getValueAt(0, DataState.FID.getValue()).intValue();

        if(state == DataState.FID.getValue()) {
            state = DataState.SPECTRUM.getValue();
        } else if(state == DataState.SPECTRUM.getValue()) {
            state = DataState.FID.getValue();
        }

        header.getListNumberParam(DefaultParams.STATE.name()).getValue().set(0, state);
    }
}
