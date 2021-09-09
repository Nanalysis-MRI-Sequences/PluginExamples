package rs2d.process;

import rs2d.spinlab.data.Data;
import rs2d.spinlab.data.Header;
import rs2d.spinlab.plugins.process.ProcessPluginAbstract;
import rs2d.spinlab.tools.param.BooleanParam;
import rs2d.spinlab.tools.param.DefaultParams;
import rs2d.spinlab.tools.param.Param;
import rs2d.spinlab.tools.utility.MathUtility;
import rs2d.spinlab.tools.utility.maths.ComplexDataUtil;

public class IFFT extends ProcessPluginAbstract {
    //
    // Constructors
    //

    public IFFT() {
        super("IFFT", "Fast Fourier Transform");
    }

    //
    // Implementation of ProcessPlugin
    //

    @Override
    public Param[] getParam() {
        return new Param[]{new BooleanParam("Phase Correction", false, "Apply order 1 phase correction")};
    }

    @Override
    public Param[] execute(Param... params) {
        this.checkParameters(params);
        boolean phaseCorrection = ((Boolean) params[0].getValue());
        this.executeIFFT(phaseCorrection);
        return new Param[0];
    }

    //
    // IFFT Process
    //

    /**
     * Execute IFFT process.
     * @param phaseCorrection Flag indicating if a phase correction will be applied or not.
     */
    private void executeIFFT(boolean phaseCorrection) {
        Header header = this.getDataset().getHeader();
        int receiversCount = ((Number) header.getParam(DefaultParams.RECEIVER_COUNT.name()).getValue()).intValue();
        int matrix4D = ((Number) header.getParam(DefaultParams.MATRIX_DIMENSION_4D.name()).getValue()).intValue();
        int matrix3D = ((Number) header.getParam(DefaultParams.MATRIX_DIMENSION_3D.name()).getValue()).intValue();
        int matrix2D = ((Number) header.getParam(DefaultParams.MATRIX_DIMENSION_2D.name()).getValue()).intValue();

        int digitalPhase = header.hasParam(DefaultParams.DIGITAL_FILTER_SHIFT.name()) ?
                header.getNumberParam(DefaultParams.DIGITAL_FILTER_SHIFT.name()).getValue().intValue() : 0;

        float applied = (float) (digitalPhase * 2f * Math.PI);

        int finalDataSize = header.getNumberParam(DefaultParams.MATRIX_DIMENSION_1D).getValue().intValue();

        for (int r = 0; r < receiversCount; r++) {
            Data data = this.getDataset().getData(r);
            finalDataSize = FFT.fillToNextPowerOf2(data);

            float[][][][] real = data.getRealPart();
            float[][][][] imag = data.getImaginaryPart();

            for (int l = 0; l < matrix4D; l++) {
                for (int k = 0; k < matrix3D; k++) {
                    for (int j = 0; j < matrix2D; j++) {
                        this.executeIFFTOnArray(real[l][k][j], imag[l][k][j], phaseCorrection, applied);
                    }
                }
            }
        }

        header.getNumberParam(DefaultParams.MATRIX_DIMENSION_1D.name()).setValue(finalDataSize);
        FFT.inverseState(header);
    }

    /**
     * Execute IFFT process on an array.
     * @param real Real part of the data.
     * @param imag Imaginary par of the data.
     * @param phaseCorrection Flag indicating if a phase correction will be applied or not.
     * @param applied Calculated value of the Phase 1 for phase correction.
     */
    private void executeIFFTOnArray(float[] real, float[] imag, boolean phaseCorrection, float applied) {
        MathUtility.executeIFFT(real, imag);

        if (phaseCorrection) {
            ComplexDataUtil.phaseShift(real, imag, 0, -applied);
        }
    }
}
