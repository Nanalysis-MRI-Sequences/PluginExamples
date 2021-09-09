package rs2d.process;

import org.junit.Assert;
import org.junit.Test;
import rs2d.spinlab.data.Data;
import rs2d.spinlab.data.DataSet;
import rs2d.spinlab.tools.param.BooleanParam;
import rs2d.spinlab.tools.param.ModalityEnum;
import rs2d.spinlab.tools.param.Param;

public class AmplitudeTest {
    private float simulatedFunction1D(int element, int size) {
        float fElement = element * 10f / size - 5f;
        return (float) (Math.pow(fElement, 4) - 16 * Math.pow(fElement, 2) + 5 * fElement);
    }

    private float simulatedFunction3D(int x, int y, int z, int matrix1D, int matrix2D, int matrix3D) {
        return 0.5f * (simulatedFunction1D(x, matrix1D) + simulatedFunction1D(y, matrix2D) + simulatedFunction1D(z, matrix3D));
    }

    private float[][][] simulatedData(int matrix1D, int matrix2D, int matrix3D, float offset) {
        float[][][] output = new float[matrix3D][matrix2D][matrix1D];

        for (int z = 0; z < matrix3D; z++) {
            for (int y = 0; y < matrix2D; y++) {
                for (int x = 0; x < matrix1D; x++) {
                    output[z][y][x] = offset - this.simulatedFunction3D(x, y, z, matrix1D, matrix2D, matrix3D);
                }
            }
        }

        return output;
    }

    @Test
    public void testAmplitudeSingleReceiver() {
        int size = 100;
        DataSet dataset = new DataSet(size, size, size, 1, 1, ModalityEnum.MRI);
        dataset.setData(
                new float[][][][]{this.simulatedData(size, size, size, 0f)},
                new float[][][][]{this.simulatedData(size, size, size, 1f)},
                0);

        Amplitude amplitudeProcess = new Amplitude(dataset);
        Param[] parameters = amplitudeProcess.getParam();
        parameters[0].setValue(false);
        amplitudeProcess.execute(parameters);

        Data data = dataset.getData(0);
        float[][][][] real = data.getRealPart();
        float[][][][] imag = data.getImaginaryPart();

        for (int z = 0; z < size; z++) {
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    float s = this.simulatedFunction3D(x, y, z, size, size, size);
                    Assert.assertEquals(String.format("Error for position (%d, %d, %d)", x, y, z), real[0][z][y][x],
                            Math.sqrt(s * s + (1 - s) * (1 - s)), 1e-4);
                    Assert.assertEquals("Test amplitude from single receiver.", 0f, imag[0][z][y][x], 1e-4);
                }
            }
        }
    }

    @Test
    public void testAmplitudeMultipleReceivers() {
        int size = 100;
        DataSet dataset = new DataSet(size, size, size, 1, 2, ModalityEnum.MRI);
        dataset.setData(
                new float[][][][]{this.simulatedData(size, size, size, 0f)},
                new float[][][][]{this.simulatedData(size, size, size, 1f)},
                0);
        dataset.setData(
                new float[][][][]{this.simulatedData(size, size, size, 1f)},
                new float[][][][]{this.simulatedData(size, size, size, 0f)},
                1);

        Amplitude amplitudeProcess = new Amplitude(dataset);
        Param[] parameters = amplitudeProcess.getParam();
        parameters[0].setValue(false);
        amplitudeProcess.execute(parameters);

        Assert.assertEquals("Test number of receivers.", 2, dataset.getData().size());

        dataset.getData().forEach(data -> {
            float[][][][] real = data.getRealPart();
            float[][][][] imag = data.getImaginaryPart();

            for (int z = 0; z < size; z++) {
                for (int y = 0; y < size; y++) {
                    for (int x = 0; x < size; x++) {
                        float s = this.simulatedFunction3D(x, y, z, size, size, size);
                        Assert.assertEquals(String.format("Test amplitude from multiple receivers for real part at (%d, %d, %d)", x, y, z),
                                Math.sqrt(s * s + (1 - s) * (1 - s)), real[0][z][y][x], 1e-4);
                        Assert.assertEquals(String.format("Test amplitude from multiple receivers for imaginary part at (%d, %d, %d)", x, y, z),
                                0f, imag[0][z][y][x], 1e-4);
                    }
                }
            }
        });
    }

    @Test
    public void testAmplitudeMultipleReceiversWithSumOfSquare() {
        int size = 100;
        DataSet dataset = new DataSet(size, size, size, 1, 2, ModalityEnum.MRI);
        dataset.setData(
                new float[][][][]{this.simulatedData(size, size, size, 0f)},
                new float[][][][]{this.simulatedData(size, size, size, 1f)},
                0);
        dataset.setData(
                new float[][][][]{this.simulatedData(size, size, size, 1f)},
                new float[][][][]{this.simulatedData(size, size, size, 0f)},
                1);

        Amplitude amplitudeProcess = new Amplitude(dataset);
        Param[] parameters = amplitudeProcess.getParam();
        Assert.assertTrue("Test input parameter of amplitude process when multiple receivers", ((BooleanParam) parameters[0]).getValue());
        amplitudeProcess.execute(parameters);

        Assert.assertEquals("", 1, dataset.getData().size());

        Data data = dataset.getData(0);
        float[][][][] real = data.getRealPart();
        float[][][][] imag = data.getImaginaryPart();

        for (int z = 0; z < size; z++) {
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    float s = this.simulatedFunction3D(x, y, z, size, size, size);
                    Assert.assertEquals(
                            String.format("Test amplitude from multiple receivers (sum of square) for real part at (%d, %d, %d)", x, y, z),
                            real[0][z][y][x], Math.sqrt(2f * (s * s + (1 - s) * (1 - s))), 1e-4);
                    Assert.assertEquals(
                            String.format("Test amplitude from multiple receivers (sum of square) for imaginary part at (%d, %d, %d)", x, y, z),
                            0f, imag[0][z][y][x], 1e-4);
                }
            }
        }
    }
}
