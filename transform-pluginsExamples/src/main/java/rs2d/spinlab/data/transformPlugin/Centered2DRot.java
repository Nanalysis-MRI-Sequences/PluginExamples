package rs2d.spinlab.data.transformPlugin;

import rs2d.spinlab.tools.param.*;
import rs2d.spinlab.tools.param.exception.UncompatibleParamsException;

import java.util.List;

/**
 * Transformation for sequential acquisition.
 *
 * @author jb
 * @version 1.0, Created on 30 mars 2010
 * @since 1.0
 */
public class Centered2DRot extends TransformPluginAbstract {

//    public final static String FIRST_ECHO_POSITION = "FIRST_ECHO_POSITION";
//    public final static String POSTION_CENTER = "center";
//    public final static String POSTION_BORDER = "border";
    private final static int JUMP_SIZE = 2;
    private static final float VERSION = 1.0f;
    private int etl;
    private int nb2d;
    private int matrix1D;
    private int matrix2D;
    private int matrix3D;
    private int echoEffective;
    private int shift2d;
    private int nbshoot;
    private int intSlice;
    private boolean multiplanar;

    /**
     * The default constructor.
     */
    public Centered2DRot() {
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
        nb2d = matrix2D / etl;

        param = Param.getParamFromName(params, "ECHO_EFFECTIVE");
        if (param != null) {
            echoEffective = ((NumberParam) param).getValue().intValue();
        } else {
            echoEffective = 1;
        }
        if (echoEffective < etl){
            shift2d = echoEffective == 1 ? 0 : (int) Math.round((echoEffective - 0.5) * (nb2d / 2));// half shift to avoid T2 artefact
        }else {
            shift2d = Math.round((echoEffective) * (nb2d / 2));
        }

        param = Param.getParamFromName(params, MriDefaultParams.NUMBER_OF_SHOOT_3D.name());
        if (param != null) {
            nbshoot = ((NumberParam) param).getValue().intValue();
        } else {
            nbshoot = 1;
        }
        intSlice = matrix3D / nbshoot;

        param = Param.getParamFromName(params, MriDefaultParams.MULTI_PLANAR_EXCITATION.name());
        if (param != null) {
            multiplanar = ((BooleanParam) param).getValue();
        } else {
            multiplanar = false;
        }
    }

    @Override
    public int[] putData(float[] dataReal, float[] dataImaginary, int scan2D, int scan3D, int scan4D, int receiver) {
        for (int i = 0; i < dataReal.length; i++) {
            int[] transf = this.transf(i, scan2D, scan3D, scan4D);
            this.dataset.getData(receiver).setData(dataReal[i], dataImaginary[i], transf[0], transf[1], transf[2], transf[3]);
        }
        return transf(0, scan2D, scan3D, scan4D);
    }

    @Override
    public int[] transf(int scan1D, int scan2D, int scan3D, int scan4D) {

        if (multiplanar) {
            int intSliceIndex = (scan1D / (matrix1D * etl));// interleaved-slice index
            // calculate 3D k position
            int k;
            if (nbshoot == 1) { // interleaved slice only : the second half-slices goes in between the others
//                int index3DJumped = intSliceIndex * JUMP_SIZE;
//                int halfMatrix3D = (matrix3D - 1) / JUMP_SIZE;
//                int k2 = index3DJumped < matrix3D ? index3DJumped : 1 + (JUMP_SIZE * (intSliceIndex - 1 - halfMatrix3D));
//                0 2 4 6 1 3 5 7
                k = intSliceIndex * JUMP_SIZE + ((intSliceIndex < (int) Math.ceil(matrix3D / 2.0)) ? 0 : 1 - (JUMP_SIZE * (int) Math.ceil(matrix3D / 2.0)));
            } else { // interleaved slice only 
                k = intSliceIndex * nbshoot + scan3D;
            }
            scan1D = scan1D % (matrix1D * etl);
            int i = scan1D % matrix1D;
            int echoNumber = scan1D / matrix1D;
            int j;
            if (scan2D < matrix2D / (etl * 2)) {
//                j = (matrix2D / 2) - ( matrix2D * (echoNumber + 1) / (2 * etl)) + scan2D;
                j = (matrix2D / 2 - shift2d) - (matrix2D * (echoNumber + 1) / (2 * etl)) + scan2D;
                if (scan1D == 8 && scan2D == 0) {
//                    System.out.println(" i " +i+ "    shift2d " +shift2d);
                }
                if (j < 0) {
                    j = matrix2D + j;
                }

            } else {
//                j = (matrix2D / 2) + (matrix2D * (echoNumber - 1) / (2 * etl)) + scan2D;
                j = (matrix2D / 2 - shift2d) + (matrix2D * (echoNumber - 1) / (2 * etl)) + scan2D;
            }
//            if (scan1D == 8 && scan2D == 0){
//                System.out.println(scan1D+ "  " +scan2D);
//                System.out.println(" i" +i+ "  j" +j);
//            }
            return new int[]{i, j, k, scan4D};

        } else {
            int i = scan1D % matrix1D;
            int echoNumber = scan1D / matrix1D;

            int j;
            if (scan2D < matrix2D / (etl * 2)) {
//                j = (matrix2D / 2) - (matrix2D * (echoNumber + 1) / (2 * etl)) + scan2D;
                j = (matrix2D / 2 - shift2d) - (matrix2D * (echoNumber + 1) / (2 * etl)) + scan2D;
                if (j < 0) {
                    j = matrix2D + j;
                }
            } else {
//                j = (matrix2D / 2) + (matrix2D * (echoNumber - 1) / (2 * etl)) + scan2D;
                j = (matrix2D / 2 - shift2d) + (matrix2D * (echoNumber - 1) / (2 * etl)) + scan2D;
            }
            return new int[]{i, j, scan3D, scan4D};
        }
    }

    @Override
    public int[] invTransf(int i, int j, int k, int l) {
        if (multiplanar) {
            int scan1D;
            int scan3D;
            int tb = nb2d / (2); // number of scan2D per half k-space
            if (j < matrix2D / 2 - shift2d) {
                scan1D = ((etl - 1) - (j + shift2d) / tb) * matrix1D + i;
            } else if (j >= matrix2D - shift2d) {
                scan1D = ((etl - 1) - (j - (matrix2D - shift2d)) / tb) * matrix1D + i;
            } else {
                scan1D = (((j + shift2d) - matrix2D / 2) / tb) * matrix1D + i;
            }

            if (nbshoot == 1) {
                int transSlicePos = k / JUMP_SIZE + (k % 2 != 0 ? matrix3D / 2 + (matrix3D % 2 != 0 ? 1 : 0) : 0);
                scan1D += transSlicePos * matrix1D * etl;
                scan3D = 0;
            } else {
                scan1D += (k / nbshoot) * matrix1D * etl;
                scan3D = k % nbshoot;
            }

            // int scan2D = (j % tb) + (j / (matrix2D / 2)) * tb;
            int scan2D = ((j + shift2d) % tb) + (((j + shift2d) / (matrix2D / 2)) % 2) * tb;

//            if (i == 0 && j == 14) {
//                System.out.println(scan1D + "  " + scan2D);
//            }
            return new int[]{scan1D, scan2D, scan3D, l};

        } else {
//OK à simplifier.
            int scan1D;
            int tb = nb2d / (2); // number of scan2D per half k-space
            if (j < matrix2D / 2 - shift2d) {
                scan1D = ((etl - 1) - (j + shift2d) / tb) * matrix1D + i;
            } else if (j >= matrix2D - shift2d) {
                scan1D = ((etl - 1) - (j - (matrix2D - shift2d)) / tb) * matrix1D + i;
            } else {
                scan1D = (((j + shift2d) - matrix2D / 2) / tb) * matrix1D + i;
            }
            //int scan2D = (j % tb) + (j / (matrix2D / 2)) * tb;
            int scan2D = ((j + shift2d) % tb) + (((j + shift2d) / (matrix2D / 2)) % 2) * tb;
            return new int[]{scan1D, scan2D, k, l};
        }
    }

    @Override
    public String getName() {
        return "Centered2DRot";
    }

    public static void main(String args[]) {
        test(4, 18, 1, 1, 3, 1, true);
        test(4, 18, 2, 1, 3, 1, true);

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

        System.out.println("Centered2D " + " Multiplanar " + multiplanar + " " + mx1D + "/" + mx2D + "/" + mx3D + "/" + mx4D + " Etl " + netl + " Nshoot " + nshoot);
        TransformUtility.test(new Centered2DRot(), mx1D, mx2D, mx3D, mx4D, netl, nshoot, multiplanar, acqu1D, acqu2D, acqu3D, acqu4D);
    }

    @Override
    public int getSignificantEcho() {
        return echoEffective;
    }

    @Override
    public int getScanOrder() {
        return 2;
    }
}
