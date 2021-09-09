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
public class Sequential4DCine extends TransformPluginAbstract {

    final static int JUMP_SIZE = 2;//marche qu'avec 2 ne pas changer pour l'instant

  //  private int etl;
    private int cycleInScan;
    private int phasePerCycle;
    private int matrix1D;
    private int matrix3D;
//    private int matrix4D;
//    private int nbshoot;
//    private boolean multiplanar;

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
        
        
        
        phasePerCycle = ((NumberParam) Param.getParamFromName(params,"CARDIAC_NB_OF_PHASE_PER_CYCLE")).getValue().intValue();
       
        boolean cardiac_loop_or_nb4d = ((BooleanParam) Param.getParamFromName(params,"CARDIAC_LOOP_OR_NB4D")).getValue();
         
        if (cardiac_loop_or_nb4d) {
           cycleInScan =  ((NumberParam) Param.getParamFromName(params,"CARDIAC_NB_OF_CYCLE")).getValue().intValue();
        } else {
            cycleInScan = 1;
        }
 /*       if (param != null) {
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
        */
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

            int i = scan1D % matrix1D;
    
  /*  CycleInScan = 2
    PhasePerCycle = 4
    scan1D                              0   16  32  48  64  80  96  112
    Math.floor(scan1D / matrix1D)       0   1   2   3   4   5   6   7
        % PhasePerCycle                 0   1   2   3   0   1   2   3     
        * CycleInScan                   0   2   4   6   0   2   4   6 
            
    Math.floor(scan1D / matrix1D)       0   1   2   3   4   5   6   7
        / PhasePerCycle                 0   0   0   0   1   1   1   1

           l                            0   2   4   6   1   3   5   7
*/
            int l = (int)( ( Math.floor(scan1D / (double)matrix1D) % phasePerCycle) * cycleInScan + Math.floor( scan1D / ((double)matrix1D * phasePerCycle)  + scan4D * phasePerCycle * cycleInScan));
            return new int[]{i, scan2D, scan3D, l};
    }

    @Override
    public int[] invTransf(int i, int j, int k, int l) {
/*                                        l             0   1   2   3   4   5   6   7   8   9   10    
                                        scan4D          0   0   0   0   0   0   0   0   1   1   1      
    can4D_rem =  l % (PhasePerCycle * CycleInScan)      0   1   2   3   4   5   6   7   0   1   2
        
        scan4D_rem % CycleInScan                        0   1   0   1   0   1   0   1
  (a)   scan4D_rem % CycleInScan * PhasePerCycle        0   4   0   4   0   4   0   4   
  (b)   scan4D_rem / CycleInScan                        0   0   1   1   2   2   3   3
              (a) + (b)                                 0   4   1   5   2   6   3   7   
  scan1D  =   (a) + (b)* matrix1D                       0   64  16  80  32  96  48  112  0   64  16  80  32  96  48  112

        */
        
        int scan4D =  (int) Math.floor( l / ((double)phasePerCycle * cycleInScan));
        int scan4D_rem =  l % (phasePerCycle * cycleInScan);
        int scan1D =  ((scan4D_rem % cycleInScan) * phasePerCycle + scan4D_rem / cycleInScan) * matrix1D + i ;
        
        return new int[]{scan1D, j, k, scan4D};
        
    }

    @Override
    public String getName() {
        return "Sequential4DCine";
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
        test(3, 2, 5, 4, 1, 4, true);
        test(3, 2, 5, 4, 2, 4, true);
        
        test(3, 2, 5, 4, 1, 1, false);
        test(3, 2, 5, 4, 1, 4, false);
        test(3, 2, 5, 4, 2, 4, false);

    }

    
 
    private static void test(int mx1D, int mx2D, int mx3D, int mx4D, int Cycle, int PhPCy, boolean loop_or_nb4d) {
        int acqu1D = mx1D * PhPCy  * (loop_or_nb4d ? Cycle : 1);
        int acqu2D = mx2D;
        int acqu3D = mx3D;
        int acqu4D = loop_or_nb4d ? 1 : Cycle; 
        /*for (int i = 0; i < acqu4D; i++) {
            for (int j = 0; j < acqu3D; j++) {
                for (int k = 0; k < acqu2D; k++) {
                    for (int l = 0; l < acqu1D; l++) {
                          
                        public int[] transf( i, j, k, l);
                        int scan1D, int scan2D, int scan3D, int scan4D) {
                        public int[] invTransf(int i, int j, int k, int l) 
                                  System.err.println("");
                    }
                }
             }
        }*/
        System.out.println("Sequential4DCine " + " " + mx1D + "/" + mx2D + "/" + mx3D + "/" + mx4D + " Cycle " + Cycle + " PhPCy " + PhPCy+ "  " + loop_or_nb4d);
        
        TransformUtility.testCardiac(new Sequential4DCine(), mx1D, mx2D, mx3D, mx4D, acqu1D, acqu2D, acqu3D, acqu4D, PhPCy, Cycle, loop_or_nb4d);
    }
}
