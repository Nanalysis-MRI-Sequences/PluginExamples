package rs2d.setup;

import rs2d.commons.log.Log;
import rs2d.spinlab.data.DataSetInterface;
import rs2d.spinlab.data.io.DatasetIO;
import rs2d.spinlab.distrib.Distrib;
import rs2d.spinlab.plugins.setup.SetUpPluginAbstract;
import rs2d.spinlab.tools.param.MriDefaultParams;
import rs2d.spinlab.tools.param.Param;
import rs2d.spinlab.tools.param.ParamLibrary;
import rs2d.spinlab.tools.param.exception.ParamsExceptionSolver;
import rs2d.spinlab.tools.param.exception.UncompatibleParam;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

public class DynamicSetup extends SetUpPluginAbstract {
    private static final List<String> MANDATORY_PARAMS = Arrays.asList(
            MriDefaultParams.DYNAMIC_SEQUENCE.name(), "USER_PARTIAL_SLICE", "USER_PARTIAL_PHASE");

    @Override
    public Param[] getParam() {
        return null;
    }

    @Override
    public Param[] getResult() {
        return null;
    }

    @Override
    public String getDescription() {
        return "Get the high resolution data before a dynamic acquisition";
    }

    @Override
    public Param[] execute(Param... params) throws Exception {
        if(!userParams.getParams().keySet().containsAll(MANDATORY_PARAMS)) {
            Log.warning(getClass(), "Trying to call DynamicSetup without mandatory parameters, aborting: " + MANDATORY_PARAMS);
            return null;
        }

        float dyn2D = 100f;
        float dyn3D = 100f;

        //on travaille sur une copie des parametres de l'application
        ParamLibrary paramLibrary = userParams.clone();
        paramLibrary.getParams().get("USER_PARTIAL_PHASE").setValue(dyn2D);
        paramLibrary.getParams().get("USER_PARTIAL_SLICE").setValue(dyn3D);
        paramLibrary.getParams().get(MriDefaultParams.DYNAMIC_SEQUENCE.name()).setValue(false);

        setSequence(paramLibrary);
        Distrib.getInstance().startSequencer(true);

        DataSetInterface dataset = Distrib.getInstance().getDataset();
        if (serie != null) {
            File setupOutputDir = new File(serie.getOutputDirectory(), "Setup");
            Log.info(getClass(), "Data will be saved in " + setupOutputDir);
            DatasetIO.saveDataset(dataset, setupOutputDir);
            Log.info(getClass(), "Done.");
        }

        //l'appel a setSequence remet les paramètres initiaux dans l'application
        //y compris ceux modifiés par unreach/uncompatible params
        //nécessaire car Distrib.setSequence les modifie...
        setSequence(userParams);
        return null;
    }

    /**
     * Calls Distrib.setSequence recursively to solve unreach/uncompatible params
     */
    private void setSequence(ParamLibrary paramLibrary) throws Exception {
        //le bout de code qui sera exécuté
        Callable<Void> callable = () -> {
            Distrib.getInstance().setSequence(sequenceGeneratorName, paramLibrary, txRoute, true);
            return null;
        };

        //appelle le callable récursivement, en changeant les userParams si nécessaire
        ParamsExceptionSolver<Void> solver = new ParamsExceptionSolver<>(callable);
        solver.execute(paramLibrary);

        //affiche les params modifiés
        for(UncompatibleParam param: solver.getChangedParameters()) {
            Log.debug(getClass(), "changed param: " + param);
        }
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }
}
