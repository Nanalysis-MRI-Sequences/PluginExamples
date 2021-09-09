package rs2d.process;

import org.junit.Assert;
import org.junit.Test;
import rs2d.spinlab.data.DataSetInterface;
import rs2d.spinlab.data.simulation.MriEllipsoidDatasetSimulator;
import rs2d.spinlab.tools.param.MriDefaultParams;
import rs2d.spinlab.tools.param.Param;

public class ZIPTest {
    @Test
    public void testZIPWithTwiceMatrixDimensions() {
        MriEllipsoidDatasetSimulator simulator = new MriEllipsoidDatasetSimulator();
        simulator.getAcquisitionParameters().get(MriDefaultParams.ACQUISITION_MATRIX_DIMENSION_1D.name()).setValue(8);
        simulator.getAcquisitionParameters().get(MriDefaultParams.ACQUISITION_MATRIX_DIMENSION_2D.name()).setValue(8);
        DataSetInterface dataset = simulator.simulate();

        ZIP process = new ZIP(dataset);
        Param[] params = process.getParam();
        params[0].setValue(true);
        process.execute(params);

        float[][] real = dataset.getData(0).getRealPart()[0][0];
        float[][] imag = dataset.getData(0).getImaginaryPart()[0][0];

        Assert.assertEquals("Test ZIF process number of lines.",
                ZFTest.EXPECTED_SYM_ROW_REAL.length, real.length);
        Assert.assertEquals("Test ZIF process number of elements.",
                ZFTest.EXPECTED_SYM_ROW_REAL[0].length, real[0].length);

        for (int j = 0; j < real.length; j++) {
            Assert.assertArrayEquals(String.format("Test ZIP process with twice dimensions for real part (line %d)", j),
                    ZFTest.EXPECTED_SYM_ROW_REAL[j], real[j], 1e-7f);
            Assert.assertArrayEquals(String.format("Test ZIP process with twice dimensions for imaginary part (line %d)", j),
                    ZFTest.EXPECTED_SYM_ROW_IMAG[j], imag[j], 1e-7f);
        }
    }

    @Test
    public void testZIPWithOnceMatrixDimensions() {
        MriEllipsoidDatasetSimulator simulator = new MriEllipsoidDatasetSimulator();
        simulator.getAcquisitionParameters().get(MriDefaultParams.ACQUISITION_MATRIX_DIMENSION_1D.name()).setValue(8);
        simulator.getAcquisitionParameters().get(MriDefaultParams.ACQUISITION_MATRIX_DIMENSION_2D.name()).setValue(8);
        DataSetInterface dataset = simulator.simulate();

        ZIP process = new ZIP(dataset);
        Param[] params = process.getParam();
        params[0].setValue(false);
        process.execute(params);

        float[][] real = dataset.getData(0).getRealPart()[0][0];
        float[][] imag = dataset.getData(0).getImaginaryPart()[0][0];

        Assert.assertEquals("Test ZIF process number of lines.",
                ZFTest.EXPECTED_ASYM_ROW_REAL.length, real.length);
        Assert.assertEquals("Test ZIF process number of elements.",
                ZFTest.EXPECTED_ASYM_ROW_REAL[0].length - 8, real[0].length);

        for (int j = 0; j < real.length; j++) {
            for (int i = 0; i < real[0].length; i++) {
                Assert.assertEquals(String.format("Test ZIP process for real part (line %d, element %d)", j, i),
                        ZFTest.EXPECTED_ASYM_ROW_REAL[j][i], real[j][i], 1e-7f);
                Assert.assertEquals(String.format("Test ZIP process for imaginary part (line %d, element %d)", j, i),
                        ZFTest.EXPECTED_ASYM_ROW_IMAG[j][i], imag[j][i], 1e-7f);
            }
        }
    }
}
