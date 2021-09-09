package rs2d.process;

import org.junit.Assert;
import org.junit.Test;
import rs2d.spinlab.data.DataSetInterface;
import rs2d.spinlab.data.simulation.MriEllipsoidDatasetSimulator;
import rs2d.spinlab.tools.param.MriDefaultParams;
import rs2d.spinlab.tools.param.Param;

public class ZFTest {
    static final float[][] EXPECTED_SYM_ROW_REAL = {
            { 0.f,  0.f,  0.f,  0.f,  0.01771601f, -0.00074609f, -0.02056877f, -0.02574607f, -0.02516436f, -0.01978794f, -0.00196333f,  0.01325822f,  0.f,  0.f,  0.f, 0.f},
            { 0.f,  0.f,  0.f,  0.f, -0.02380113f, -0.00527014f,  0.04251435f,  0.05582454f,  0.05483467f,  0.0414671f, -0.00420404f, -0.02246468f,  0.f,  0.f,  0.f, 0.f},
            { 0.f,  0.f,  0.f,  0.f, -0.01136151f, 0.0351473f, -0.04064043f, -0.07207167f, -0.07398822f, -0.04273221f, 0.03795968f, -0.00531629f,  0.f,  0.f,  0.f, 0.f},
            { 0.f,  0.f,  0.f,  0.f,  0.01809494f, -0.04593593f, -0.03417296f,  0.43855313f,  0.43759201f, -0.03519578f, -0.04435016f,  0.02159757f,  0.f,  0.f,  0.f, 0.f},
            { 0.f,  0.f,  0.f,  0.f,  0.02159757f, -0.04435016f, -0.03519578f,  0.43759201f,  0.43855313f, -0.03417296f, -0.04593593f,  0.01809494f,  0.f,  0.f,  0.f, 0.f},
            { 0.f,  0.f,  0.f,  0.f, -0.00531629f, 0.03795968f, -0.04273221f, -0.07398822f, -0.07207167f, -0.04064043f, 0.0351473f, -0.01136151f,  0.f,  0.f,  0.f, 0.f},
            { 0.f,  0.f,  0.f,  0.f, -0.02246468f, -0.00420404f,  0.0414671f,  0.05483467f,  0.05582454f,  0.04251435f, -0.00527014f, -0.02380113f,  0.f,  0.f,  0.f, 0.f},
            { 0.f,  0.f,  0.f,  0.f,  0.01325822f, -0.00196333f, -0.01978794f, -0.02516436f, -0.02574607f, -0.02056877f, -0.00074609f,  0.01771601f,  0.f,  0.f,  0.f, 0.f},
    };

    static final float[][] EXPECTED_SYM_ROW_IMAG = {
            { 0.f,  0.f,  0.f,  0.f,  0.00318877f, -0.00338688f, -0.00714831f, -0.00656743f, -0.00398197f, -0.00164541f, 0.00089527f,  0.0043982f,  0.f,  0.f,  0.f, 0.f},
            { 0.f,  0.f,  0.f,  0.f, -0.00211421f, 0.00534411f,  0.00779983f,  0.00456752f,  0.00650407f,  0.01226499f, 0.00942437f, -0.0006808f,  0.f,  0.f,  0.f, 0.f},
            { 0.f,  0.f,  0.f,  0.f,  0.0072571f, 0.01629048f,  0.00941044f,  0.00276145f, -0.00018498f,  0.00408789f, 0.01401126f,  0.00773301f,  0.f,  0.f,  0.f, 0.f},
            { 0.f,  0.f,  0.f,  0.f,  0.00593454f, 0.01125974f,  0.01590398f,  0.022145f,  0.01418099f,  0.00034334f, 0.00195461f,  0.00497956f,  0.f,  0.f,  0.f, 0.f},
            { 0.f,  0.f,  0.f,  0.f, -0.00497956f, -0.00195461f, -0.00034334f, -0.01418099f, -0.022145f, -0.01590398f, -0.01125974f, -0.00593454f,  0.f,  0.f,  0.f, 0.f},
            { 0.f,  0.f,  0.f,  0.f, -0.00773301f, -0.01401126f, -0.00408789f,  0.00018498f, -0.00276145f, -0.00941044f, -0.01629048f, -0.0072571f,  0.f,  0.f,  0.f, 0.f},
            { 0.f,  0.f,  0.f,  0.f,  0.0006808f, -0.00942437f, -0.01226499f, -0.00650407f, -0.00456752f, -0.00779983f, -0.00534411f,  0.00211421f,  0.f,  0.f,  0.f, 0.f},
            { 0.f,  0.f,  0.f,  0.f, -0.0043982f, -0.00089527f,  0.00164541f,  0.00398197f,  0.00656743f,  0.00714831f, 0.00338688f, -0.00318877f,  0.f,  0.f,  0.f, 0.f},
    };

    static final float[][] EXPECTED_ASYM_ROW_REAL = {
            { 0.01771601f, -0.00074609f, -0.02056877f, -0.02574607f, -0.02516436f,-0.01978794f, -0.00196333f,  0.01325822f,  0.f,  0.f, 0.f,  0.f, 0.f,  0.f,  0.f, 0.f},
            {-0.02380113f, -0.00527014f,  0.04251435f,  0.05582454f,  0.05483467f, 0.0414671f, -0.00420404f, -0.02246468f,  0.f,  0.f, 0.f,  0.f,  0.f,  0.f,  0.f, 0.f},
            {-0.01136151f,  0.0351473f, -0.04064043f, -0.07207167f, -0.07398822f,-0.04273221f,  0.03795968f, -0.00531629f,  0.f,  0.f, 0.f,  0.f, 0.f,  0.f,  0.f, 0.f},
            { 0.01809494f, -0.04593593f, -0.03417296f,  0.43855313f,  0.43759201f,-0.03519578f, -0.04435016f,  0.02159757f,  0.f,  0.f, 0.f,  0.f, 0.f,  0.f,  0.f, 0.f},
            { 0.02159757f, -0.04435016f, -0.03519578f,  0.43759201f,  0.43855313f,-0.03417296f, -0.04593593f,  0.01809494f,  0.f,  0.f, 0.f,  0.f, 0.f,  0.f,  0.f, 0.f},
            {-0.00531629f,  0.03795968f, -0.04273221f, -0.07398822f, -0.07207167f,-0.04064043f,  0.0351473f, -0.01136151f,  0.f,  0.f, 0.f,  0.f, 0.f,  0.f,  0.f, 0.f},
            {-0.02246468f, -0.00420404f,  0.0414671f,  0.05483467f,  0.05582454f, 0.04251435f, -0.00527014f, -0.02380113f,  0.f,  0.f, 0.f,  0.f,  0.f,  0.f,  0.f, 0.f},
            { 0.01325822f, -0.00196333f, -0.01978794f, -0.02516436f, -0.02574607f,-0.02056877f, -0.00074609f,  0.01771601f,  0.f,  0.f, 0.f,  0.f, 0.f,  0.f,  0.f, 0.f}
    };

    static final float[][] EXPECTED_ASYM_ROW_IMAG = {
            { 0.00318877f, -0.00338688f, -0.00714831f, -0.00656743f, -0.00398197f,-0.00164541f,  0.00089527f,  0.00439820f,  0.f,  0.f, 0.f,  0.f,  0.f,  0.f,  0.f, 0.f },
            {-0.00211421f,  0.00534411f,  0.00779983f,  0.00456752f,  0.00650407f, 0.01226499f,  0.00942437f, -0.00068080f,  0.f,  0.f, 0.f,  0.f,  0.f,  0.f,  0.f, 0.f },
            { 0.00725710f,  0.01629048f,  0.00941044f,  0.00276145f, -0.00018498f, 0.00408789f,  0.01401126f,  0.00773301f,  0.f,  0.f, 0.f,  0.f,  0.f,  0.f,  0.f, 0.f },
            { 0.00593454f,  0.01125974f,  0.01590398f,  0.02214500f,  0.01418099f, 0.00034334f,  0.00195461f,  0.00497956f,  0.f,  0.f, 0.f,  0.f, 0.f,  0.f,  0.f, 0.f },
            {-0.00497956f, -0.00195461f, -0.00034334f, -0.01418099f, -0.02214500f,-0.01590398f, -0.01125974f, -0.00593454f,  0.f,  0.f, 0.f,  0.f, 0.f,  0.f,  0.f, 0.f },
            {-0.00773301f, -0.01401126f, -0.00408789f,  0.00018498f, -0.00276145f,-0.00941044f, -0.01629048f, -0.00725710f,  0.f,  0.f, 0.f,  0.f,  0.f,  0.f,  0.f, 0.f },
            { 0.00068080f, -0.00942437f, -0.01226499f, -0.00650407f, -0.00456752f,-0.00779983f, -0.00534411f,  0.00211421f,  0.f,  0.f, 0.f,  0.f,  0.f,  0.f,  0.f, 0.f },
            {-0.00439820f, -0.00089527f,  0.00164541f,  0.00398197f,  0.00656743f, 0.00714831f,  0.00338688f, -0.00318877f,  0.f,  0.f, 0.f,  0.f,  0.f,  0.f,  0.f, 0.f }
    };

    //
    // Utilities
    //

    private static void executeWithParameters(DataSetInterface dataset, Integer reconstructionSize, Boolean symmetric) {
        ZF process = new ZF(dataset);
        Param[] params = process.getParam();

        if (reconstructionSize != null) {
            params[0].setValue(reconstructionSize);
        }

        if (symmetric != null) {
            params[1].setValue(symmetric);
        }

        process.execute(params);
    }

    private static DataSetInterface createDataset() {
        MriEllipsoidDatasetSimulator simulator = new MriEllipsoidDatasetSimulator();
        simulator.getAcquisitionParameters().get(MriDefaultParams.ACQUISITION_MATRIX_DIMENSION_1D.name()).setValue(8);
        simulator.getAcquisitionParameters().get(MriDefaultParams.ACQUISITION_MATRIX_DIMENSION_2D.name()).setValue(8);
        return simulator.simulate();
    }

    private static void testOutput(DataSetInterface dataset, float[][] expectedReal, float[][] expectedImag) {
        float[][] real = dataset.getData(0).getRealPart()[0][0];
        float[][] imag = dataset.getData(0).getImaginaryPart()[0][0];

        Assert.assertEquals("Test ZF process number of lines.", expectedReal.length, real.length);
        Assert.assertEquals("Test ZF process number of elements.", expectedReal[0].length, real[0].length);

        for (int j = 0; j < real.length; j++) {
            Assert.assertArrayEquals(String.format("Test symmetric ZF process real part (line %d)", j), expectedReal[j], real[j], 1e-7f);
            Assert.assertArrayEquals(String.format("Test symmetric ZF process for imaginary part (line %d)", j), expectedImag[j], imag[j], 1e-7f);
        }
    }

    private static void testWithParameters(float[][] expectedReal, float[][] expectedImag, Integer reconstructionSize, Boolean symmetric) {
        DataSetInterface dataset = createDataset();
        executeWithParameters(dataset, reconstructionSize, symmetric);
        testOutput(dataset, expectedReal, expectedImag);
    }

    //
    // Tests
    //

    @Test
    public void testSymmetricZeroFilling() {
        testWithParameters(EXPECTED_SYM_ROW_REAL, EXPECTED_SYM_ROW_IMAG, EXPECTED_SYM_ROW_REAL[0].length, true);
    }

    @Test
    public void testAsymmetricZeroFilling() {
        testWithParameters(EXPECTED_ASYM_ROW_REAL, EXPECTED_ASYM_ROW_IMAG, EXPECTED_ASYM_ROW_REAL[0].length, null);
    }
}
