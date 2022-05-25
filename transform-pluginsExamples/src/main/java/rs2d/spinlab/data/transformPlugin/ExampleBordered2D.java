package rs2d.spinlab.data.transformPlugin;

import rs2d.spinlab.tools.param.*;
import rs2d.spinlab.tools.param.exception.UncompatibleParamsException;

import java.util.List;

/**
 * /**
 *
 * Tha acqu1D size must be etl*matrix1D The matrix2D must be a entier multiple
 * of the etl no restriction on matrix3D (number of slices). matrix3D=acqu3D
 * size.
 *
 * The first echo of the train is put in the border of the current slice The
 * last echo is in the center of the same slice.
 *
 * @author jm
 * @version 1.0, Created on 26 sept 2011
 * @since 1.0
 */
public class ExampleBordered2D extends TransformPluginAbstract {

//    public final static String FIRST_ECHO_POSITION = "FIRST_ECHO_POSITION";
//    public final static String POSTION_CENTER = "center";
//    public final static String POSTION_BORDER = "border";
    private final static int JUMP_SIZE = 2;
    private static final float VERSION = 1.0f;

    private int etl;
    private int matrix1D;
    private int matrix2D;
    private int matrix3D;
    private int nbshoot;
    private boolean multiplanar;

    /**
     * The default constructor.
     */
    public ExampleBordered2D() {
        super();

    }

    @Override
    public String getDescription() {
        return "---------->----------\n"
                + "........>..........\n"
                + "********>**********\n"
                + "********>**********\n"
                + "........>..........\n"
                + "---------->----------";
    }

    @Override
    public void setParameters(List<Param> params) throws UncompatibleParamsException {

        matrix1D = ((NumberParam) Param.getParamFromName(params, DefaultParams.ACQUISITION_MATRIX_DIMENSION_1D.name())).getValue().intValue();
        matrix2D = ((NumberParam) Param.getParamFromName(params, DefaultParams.ACQUISITION_MATRIX_DIMENSION_2D.name())).getValue().intValue();
        matrix3D = ((NumberParam) Param.getParamFromName(params, DefaultParams.ACQUISITION_MATRIX_DIMENSION_3D.name())).getValue().intValue();
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
//        matrix1D = ((NumberParam) Param.getParamFromName(params, DefaultParams.ACQUISITION_MATRIX_DIMENSION_1D.name())).getValue().intValue();
//        matrix2D = ((NumberParam) Param.getParamFromName(params, DefaultParams.ACQUISITION_MATRIX_DIMENSION_2D.name())).getValue().intValue();
//
//        Param param = Param.getParamFromName(params, MriDefaultParams.ECHO_TRAIN_LENGTH.name());
//        if (param != null) {
//            etl = ((NumberParam) param).getValue().intValue();
//        } else {
//            etl = 1;
//        }
//        int matrix2DMin = 1;
//        int etlMin = 1;
//        if (matrix2D < (etl * 2) && matrix2D != 1) {
//            matrix2DMin = etl * 2;
//            throw new UncompatibleParamsException(DefaultParams.ACQUISITION_MATRIX_DIMENSION_2D.name(), matrix2D, matrix2DMin);
//        } else if (matrix2D == 1 && etl!=1) {
//            etlMin = 1;
//            throw new UncompatibleParamsException(MriDefaultParams.ECHO_TRAIN_LENGTH.name(), etl, etlMin);
//        }
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
            int index = (scan1D / (matrix1D * etl));
            int k;
            if (nbshoot == 1) {
                int index3DJumped = index * JUMP_SIZE;
                int lastPut = (matrix3D - 1) / JUMP_SIZE;
                k = index3DJumped < matrix3D ? index3DJumped : 1 + (JUMP_SIZE * (index - 1 - lastPut));
            } else {
                k = index * nbshoot + scan3D;
            }
            //System.out.println("transf " + sliceNumber + "->" + transSlice);
            //ATTENTION ACQ1D CHANGE!! on divise par matrix 3D donc nbSlice pour avoir l'equivalent de la tranform centreée 2D
            scan1D = scan1D % (matrix1D * etl);
            int i = scan1D % matrix1D;
            int echoNumber = scan1D / matrix1D;
            echoNumber = etl - echoNumber - 1;
            int j;
            if (scan2D < matrix2D / (etl * 2)) {
                j = (matrix2D / 2) - (matrix2D * (echoNumber + 1) / (2 * etl)) + scan2D;

            } else {
                j = (matrix2D / 2) + (matrix2D * (echoNumber - 1) / (2 * etl)) + scan2D;
            }
            //System.out.println("echo number="+echoNumber+" new acqu1D="+acq1D+" slice ="+sliceNumber+"->"+finalSliceNumber);
            return new int[]{i, j, k, scan4D};
        } else {

            int i = scan1D % matrix1D;
            int echoNumber = scan1D / matrix1D;
            echoNumber = etl - echoNumber - 1;
            int j;
            if (scan2D < matrix2D / (etl * 2)) {
                j = (matrix2D / 2) - (matrix2D * (echoNumber + 1) / (2 * etl)) + scan2D;
            } else {
                j = (matrix2D / 2) + (matrix2D * (echoNumber - 1) / (2 * etl)) + scan2D;
            }
            return new int[]{i, j, scan3D, scan4D};
        }
    }

    @Override
    public int[] invTransf(int i, int j, int k, int l) {
        if (multiplanar) {
            int scan1D;
            int scan2D;
            int scan3D;
            int tb = matrix2D / (2 * etl);
            if (j < matrix2D / 2) {
                scan1D = (j / tb) * matrix1D + i;
            } else {
                scan1D = ((etl - 1) - (j - matrix2D / 2) / tb) * matrix1D + i;
            }
            scan2D = (j % tb) + (j / (matrix2D / 2)) * tb;
            if (nbshoot == 1) {
                int transSlicePos = k / JUMP_SIZE + (k % 2 != 0 ? matrix3D / 2 + (matrix3D % 2 != 0 ? 1 : 0) : 0);
                scan1D += transSlicePos * matrix1D * etl;
                scan3D = 0;
            } else {
                scan1D += (k / nbshoot) * matrix1D * etl;
                scan3D = k % nbshoot;
            }
            return new int[]{scan1D, scan2D, scan3D, l};
        } else {
            //OK à simplifier.
            int tb = matrix2D / (2 * etl);
            int scan1D;
            if (j < matrix2D / 2) {
                scan1D = (j / tb) * matrix1D + i;
            } else {
                scan1D = ((etl - 1) - (j - matrix2D / 2) / tb) * matrix1D + i;
            }

            int scan2D = (j % tb) + (j / (matrix2D / 2)) * tb;

            return new int[]{scan1D, scan2D, k, l};
        }
    }

    @Override
    public String getName() {
        return "Bordered2D";
    }

    public static void main(String args[]) {
        test(3, 6, 4, 2, 3, 1, true);
        test(3, 6, 5, 2, 3, 1, true);
        test(3, 4, 3, 2, 2, 3, true);
        test(3, 4, 9, 2, 2, 3, true);
        test(3, 4, 4, 2, 2, 2, true);
        test(3, 8, 2, 1, 2, 1, false);
    }

    private static void test(int mx1D, int mx2D, int mx3D, int mx4D, int netl, int nshoot, boolean multiplanar) {
        int acqu1D = mx1D * (multiplanar ? mx3D / nshoot : 1) * netl;
        int acqu2D = mx2D / netl;
        int acqu3D = multiplanar ? nshoot : mx3D;
        int acqu4D = mx4D;

        System.out.println("Borderer2D " + " Multiplanar " + multiplanar + " " + mx1D + "/" + mx2D + "/" + mx3D + "/" + mx4D + " Etl " + netl + " Nshoot " + nshoot);
        TransformUtility.test(new ExampleBordered2D(), mx1D, mx2D, mx3D, mx4D, netl, nshoot, multiplanar, acqu1D, acqu2D, acqu3D, acqu4D);
    }

    public int getSignificantEcho() {
        return etl;
    }

    public int getScanOrder() {
        return 2;
    }
}
