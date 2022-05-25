/**
 * Class : rs2d.spinlab.data.transformPlugin.SequentialTransform.java Project :
 * Author : jb Date : 30 mars 2010
 */
package rs2d.spinlab.data.transformPlugin;

import rs2d.spinlab.tools.param.*;
import rs2d.spinlab.tools.param.exception.UncompatibleParamsException;

import java.util.List;

/**
 * Transformation of acquistion with ETL.
 *
 * @author jb
 * @version 1.0, Created on 30 mars 2010
 * @since 1.0
 */
public class ExampleSequential4D extends TransformPluginAbstract {

    final static int JUMP_SIZE = 2;//marche qu'avec 2 ne pas changer pour l'instant

    private int etl;
    private int matrix1D;
    private int matrix3D;
//    private int matrix4D;
    private int nbshoot;
    private boolean multiplanar;

    @Override
    public String getDescription() {
        return "---------->----------\n"
                + "---------->----------\n"
                + "---------->----------\n"
                + "........>..........\n"
                + "........>..........\n"
                + "........>..........";
    }

    public void setParameters(List<Param> params) throws UncompatibleParamsException {
        matrix1D = ((NumberParam) Param.getParamFromName(params, DefaultParams.ACQUISITION_MATRIX_DIMENSION_1D.name())).getValue().intValue();
//        matrix2D = ((NumberParam) Param.getParamFromName(params, DefaultParams.ACQUISITION_MATRIX_DIMENSION_2D.name())).getValue().intValue();
        matrix3D = ((NumberParam) Param.getParamFromName(params, DefaultParams.ACQUISITION_MATRIX_DIMENSION_3D.name())).getValue().intValue();
//        matrix4D = ((NumberParam) Param.getParamFromName(params, DefaultParams.ACQUISITION_MATRIX_DIMENSION_4D.name())).getValue().intValue();
        Param param = Param.getParamFromName(params, MriDefaultParams.ECHO_TRAIN_LENGTH.name());
        if (param != null) {
            etl = ((NumberParam) param).getValue().intValue();
        } else {
            etl = 1;
        }

        param = Param.getParamFromName(params, MriDefaultParams.NUMBER_OF_SHOOT_3D.name());
        if (param != null) {
            nbshoot = ((NumberParam) param).getValue().intValue();
        } else {
            nbshoot = 1;
        }

        param = Param.getParamFromName(params, MriDefaultParams.MULTI_PLANAR_EXCITATION.name());
        if (param != null) {
            multiplanar = ((BooleanParam) param).getValue();
        } else {
            multiplanar = false;
        }
    }

    @Override
    public int[] putData(float[] dataReal, float[] dataImaginary, int j, int k, int l, int receiver) {
        for (int i = 0; i < dataReal.length; i++) {
            int[] transf = this.transf(i, j, k, l);
            this.dataset.getData(receiver).setData(dataReal[i], dataImaginary[i], transf[0], transf[1], transf[2], transf[3]);
        }
        return transf(0, j, k, l);
    }

    @Override
    public int[] transf(int scan1D, int scan2D, int scan3D, int scan4D) {
        if (multiplanar) {
            int i = scan1D % matrix1D;
            int index3d = (scan1D / (etl * matrix1D));
            int k;
            if (nbshoot == 1) {
                int index3DJumped = index3d * JUMP_SIZE;
                int lastPut = (matrix3D - 1) / JUMP_SIZE;
                k = index3DJumped < matrix3D ? index3DJumped : 1 + (JUMP_SIZE * (index3d - 1 - lastPut));
            } else {
                k = index3d * nbshoot + scan3D;
            }
            int l = (scan1D / matrix1D) % etl + scan4D * etl;
            return new int[]{i, scan2D, k, l};
        } else {
            int i = scan1D % matrix1D;
            int l = (scan1D / matrix1D) + scan4D * etl;
            return new int[]{i, scan2D, scan3D, l};
        }
    }

    @Override
    public int[] invTransf(int i, int j, int k, int l) {
        int scan4D = l / etl;
        if (multiplanar) {
            int scan1D;
            int scan3D;
            if (nbshoot == 1) {
                int transSlicePos = k / JUMP_SIZE + (k % 2 != 0 ? matrix3D / 2 + (matrix3D % 2 != 0 ? 1 : 0) : 0);
                scan1D = i + matrix1D * (l % etl) + transSlicePos * matrix1D * etl;
                scan3D = 0;
            } else {
                scan3D = k % nbshoot;
                scan1D = i + (k / nbshoot) * matrix1D * etl + (l % etl) * matrix1D;
            }
            return new int[]{scan1D, j, scan3D, scan4D};
        } else {
            int scan1D = (l % etl) * matrix1D + i;
            return new int[]{scan1D, j, k, scan4D};
        }
    }

    @Override
    public String getName() {
        return "Sequential4D";
    }

    @Override
    public int getSignificantEcho() {
        return 1;
    }

    @Override
    public int getScanOrder() {
        return 4;
    }

    public static void main(String args[]) {
        test(3, 2, 5, 4, 1, 1, true);
        test(3, 2, 5, 4, 2, 1, true);
        test(3, 2, 5, 4, 4, 1, true);

        test(3, 2, 3, 4, 1, 3, true);
        test(3, 2, 9, 4, 2, 3, true);
        test(3, 2, 4, 4, 4, 2, true);

        test(3, 2, 1, 4, 1, 1, false);
        test(3, 2, 1, 4, 2, 1, false);
        test(3, 2, 1, 4, 4, 1, false);
    }

    private static void test(int mx1D, int mx2D, int mx3D, int mx4D, int netl, int nshoot, boolean multiplanar) {
        int acqu1D = mx1D * (multiplanar ? mx3D / nshoot : 1) * netl;
        int acqu2D = mx2D;
        int acqu3D = multiplanar ? nshoot : mx3D;
        int acqu4D = mx4D / netl;

        System.out.println("Sequential4D " + " Multiplanar " + multiplanar + " " + mx1D + "/" + mx2D + "/" + mx3D + "/" + mx4D + " Etl " + netl + " Nshoot " + nshoot);
        TransformUtility.test(new ExampleSequential4D(), mx1D, mx2D, mx3D, mx4D, netl, nshoot, multiplanar, acqu1D, acqu2D, acqu3D, acqu4D);
    }
}
