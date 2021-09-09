package rs2d.process;

import rs2d.spinlab.plugins.process.ProcessPluginAbstract;
import rs2d.spinlab.tools.param.DefaultParams;
import rs2d.spinlab.tools.param.Param;
import rs2d.spinlab.tools.param.TextParam;
import rs2d.spinlab.tools.utility.MathUtility;

import java.util.ArrayList;

public class BC extends ProcessPluginAbstract {
    //
    // Constructors
    //

    public BC() {
        super("BC", "FID baseline correction");
    }

    //
    // Implementation of ProcessPlugin
    //

    @Override
    public Param[] getParam() {
        TextParam noiseLocation = new TextParam(
                "Noise location", MathUtility.BC_END,
                "The noise location : End=the last 1/4 points ; Start=the first 1/4 points ;  Both=the first and last 1/8 points");

        ArrayList<String> arrayList = new ArrayList<>();
        arrayList.add(MathUtility.BC_END);
        arrayList.add(MathUtility.BC_START);
        arrayList.add(MathUtility.BC_BOTH);
        noiseLocation.setSuggestedValues(arrayList);
        noiseLocation.setRestrictedToSuggested(true);

        return new Param[]{noiseLocation};
    }

    @Override
    public Param[] execute(Param... params) {
        this.checkParameters(params);
        String noiseLocation = ((TextParam) params[0]).getValue();
        this.executeBC(noiseLocation);
        return new Param[0];
    }

    //
    // BC process
    //

    /**
     * Execute the baseline correction process.
     * @param noiseLocation Noise location to pass to the baseline corrector.
     */
    private void executeBC(String noiseLocation) {
        int receiverCounts = ((Number) this.getDataset().getHeader().getParam(DefaultParams.RECEIVER_COUNT.name()).getValue()).intValue();
        int matrix4D = ((Number) this.getDataset().getHeader().getParam(DefaultParams.MATRIX_DIMENSION_4D.name()).getValue()).intValue();
        int matrix3D = ((Number) this.getDataset().getHeader().getParam(DefaultParams.MATRIX_DIMENSION_3D.name()).getValue()).intValue();
        int matrix2D = ((Number) this.getDataset().getHeader().getParam(DefaultParams.MATRIX_DIMENSION_2D.name()).getValue()).intValue();

        for (int receiver = 0; receiver < receiverCounts; receiver++) {
            for (int l = 0; l < matrix4D; l++) {
                for (int k = 0; k < matrix3D; k++) {
                    for (int j = 0; j < matrix2D; j++) {
                        MathUtility.executeBC(
                                this.getDataset().getData(receiver).getRealPart()[l][k][j],
                                this.getDataset().getData(receiver).getImaginaryPart()[l][k][j],
                                noiseLocation);
                    }
                }
            }
        }
    }
}
