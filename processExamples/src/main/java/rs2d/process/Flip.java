package rs2d.process;

import rs2d.spinlab.plugins.process.ProcessPluginAbstract;
import rs2d.spinlab.tools.param.NumberEnum;
import rs2d.spinlab.tools.param.NumberParam;
import rs2d.spinlab.tools.param.Param;

import java.util.Arrays;

public class Flip extends ProcessPluginAbstract {
    //
    // Constructors
    //

    public Flip() {
        super("Flip", "Flip data");
    }

    //
    // Implementation of ProcessPlugin
    //

    @Override
    public Param[] getParam() {
        NumberParam dimension = new NumberParam("Dimension", 4, NumberEnum.Integer, "Dimension to flip");
        dimension.setSuggestedValues(Arrays.asList(2, 3, 4));
        dimension.setRestrictedToSuggested(true);

        NumberParam init = new NumberParam("Init", 0, NumberEnum.Integer, "Start flipping at index");
        init.setMinValue(0);

        NumberParam step = new NumberParam("Step", 2, NumberEnum.Integer, "Flip for each step");
        step.setMinValue(1);

        return new Param[]{dimension, init, step};
    }

    @Override
    public Param[] execute(Param... params) {
        this.checkParameters(params);
        int dimension = ((NumberParam) params[0]).getValue().intValue();
        int init = ((NumberParam) params[1]).getValue().intValue();
        int step = ((NumberParam) params[2]).getValue().intValue();
        this.executeFlip(dimension, init, step);
        return new Param[0];
    }

    //
    // Flip process
    //

    /**
     * Execute Flip process.
     * @param dimension Dimension to flip.
     * @param init First index to flip.
     * @param step Step between indices to flip.
     * @throws IllegalArgumentException If arguments are bad
     */
    private void executeFlip(int dimension, int init, int step) {
        if(init < 0 || step < 1) {
            throw new IllegalArgumentException("Illegal parameters: init=" + init + ", step=" + step);
        }

        if(dimension == 2) {
            this.executeFlipOn2ndDimension(init, step);
        } else if(dimension == 3) {
            this.executeFlipOn3rdDimension(init, step);
        } else if(dimension == 4) {
            this.executeFlipOn4thDimension(init, step);
        } else {
            throw new IllegalArgumentException("Illegal flip dimension: " + dimension);
        }
    }

    /**
     * Execute the flip process on the 4th dimension.
     * @param init First index to flip.
     * @param step Step between indices to flip.
     */
    private void executeFlipOn4thDimension(int init, int step) {
        this.getDataset().getData().forEach(data -> {
            float[][][][] real = data.getRealPart();
            float[][][][] imag = data.getImaginaryPart();

            for (int l = init; l < real.length; l += step) {
                Flip.flip3D(real[l]);
                Flip.flip3D(imag[l]);
            }
        });
    }

    /**
     * Execute the flip process on the 3rd dimension.
     * @param init First index to flip.
     * @param step Step between indices to flip.
     */
    private void executeFlipOn3rdDimension(int init, int step) {
        this.getDataset().getData().forEach(data -> {
            float[][][][] real = data.getRealPart();
            float[][][][] imag = data.getImaginaryPart();

            for (int l = 0; l < real.length; l++) {
                for (int k = init; k < real[0].length; k += step) {
                    Flip.flip2D(real[l][k]);
                    Flip.flip2D(imag[l][k]);
                }
            }
        });
    }

    /**
     * Execute the flip process on the 2nd dimension.
     * @param init First index to flip.
     * @param step Step between indices to flip.
     */
    private void executeFlipOn2ndDimension(int init, int step) {
        this.getDataset().getData().forEach(data ->  {
            float[][][][] real = data.getRealPart();
            float[][][][] imag = data.getImaginaryPart();

            for (int l = 0; l < real.length; l++) {
                for (int k = 0; k < real[0].length; k++) {
                    for (int j = init; j < real[0][0].length; j += step) {
                        Flip.flip1D(real[l][k][j]);
                        Flip.flip1D(imag[l][k][j]);
                    }
                }
            }
        });
    }

    /**
     * Flip an array against its center.
     * @param values Array to flip.
     */
    private static void flip1D(float[] values) {
        for (int start = 0, end = values.length-1; start < end; start++, end--) {
            float temp = values[start];
            values[start] = values[end];
            values[end] = temp;
        }
    }

    /**
     * Flip a 2D array against the center of its first dimension.
     * @param values 2D array to flip.
     */
    private static void flip2D(float[][] values) {
        for (int start = 0, end = values.length-1; start < end; start++, end--) {
            float[] temp = values[start];
            values[start] = values[end];
            values[end] = temp;
        }
    }

    /**
     * Flip 3D array against the center of its first dimension.
     * @param values 3D array to flip.
     */
    private static void flip3D(float[][][] values) {
        for (int start = 0, end = values.length-1; start < end; start++, end--) {
            float[][] temp = values[start];
            values[start] = values[end];
            values[end] = temp;
        }
    }
}
