package rs2d.spinlab.data.transformPlugin;

import rs2d.spinlab.tools.param.Param;
import rs2d.spinlab.tools.param.exception.UncompatibleParamsException;

import java.util.List;

public class EchoAntiechoTransform extends TransformPluginAbstract {
    @Override
    public String getName() {
        return "EchoAntiechoTransform";
    }

    @Override
    public String getDescription() {
        return "Echo/Anti-echo sequence transform.";
    }

    @Override
    public int[] putData(float[] real, float[] imag, int scan2D, int scan3D, int scan4D, int receiver) {
        for (int j = 0; j < real.length; j++) {
            int[] transCoord = transf(j, scan2D, scan3D, scan4D);
           dataset.getData(receiver).setData(real[j], imag[j], transCoord[0], transCoord[1], transCoord[2], transCoord[3]);
        }

        return transf(0, scan2D, scan3D, scan4D);
    }

    @Override
    public int[] transf(int scan1D, int scan2D, int scan3D, int scan4D) {
        return new int[]{scan1D, 2 * scan3D + scan2D, 0, 0};
    }

    @Override
    public int[] invTransf(int j, int i, int k, int l) {
        return new int[]{j, i%2, i/2, 0};
    }

    @Override
    public int getSignificantEcho() {
        return 0;
    }

    @Override
    public int getScanOrder() {
        return 0;
    }

    @Override
    public void setParameters(List<Param> list) throws UncompatibleParamsException {
        // nothing to do here
    }
}
