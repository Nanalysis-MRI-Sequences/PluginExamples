package rs2d.process;

import rs2d.spinlab.data.DataSetInterface;
import rs2d.spinlab.data.util.iterators.PointDatasetIterator;
import rs2d.spinlab.plugins.process.ProcessPluginAbstract;
import rs2d.spinlab.tools.param.BooleanParam;
import rs2d.spinlab.tools.param.Param;

public class PhaseImage extends ProcessPluginAbstract {
    //
    // Constructors
    //

    public PhaseImage() {
        super("Phase", "Display the phase");
    }

    public PhaseImage(DataSetInterface dataset) {
        this();
        this.setDataset(dataset);
    }

    //
    // Implementation of ProcessPlugin
    //

    @Override
    public Param[] getParam() {
        return new Param[]{new BooleanParam("Four quadrant", true, "Use the four quadrant arc-tangent")};
    }

    @Override
    public Param[] execute(Param... params) {
        this.checkParameters(params);
        boolean fourQuadrant = ((BooleanParam) params[0]).getValue();
        this.executePhaseImage(fourQuadrant);
        return new Param[0];
    }

    //
    // PhaseImage process
    //

    /**
     * Execute PhaseImage process.
     * @param fourQuadrant Flag to indicate if four quadrants are used or not.
     */
    private void executePhaseImage(boolean fourQuadrant) {
        new PointDatasetIterator(this.getDataset()).map(point -> {
            point.real = PhaseImage.calculatePhase(point.real, point.imag, fourQuadrant);
            point.imag = 0;
            return point;
        });
    }

    /**
     * Compute phase of a complex number.
     * @param real Real part of the number.
     * @param imag Imaginary part of the number.
     * @param fourQuadrant Flag to indicate if four quadrants are used or not.
     * @return The phase of the input complex number.
     */
    private static float calculatePhase(float real, float imag, boolean fourQuadrant) {
        float phase;

        if (fourQuadrant) {
            phase = (float) Math.atan2(imag, real);
        } else {
            phase = (float) Math.atan(imag / real);

            if (Float.compare(real, 0) < 0) {
                phase += Math.PI;
            }
        }

        return phase;
    }
}
