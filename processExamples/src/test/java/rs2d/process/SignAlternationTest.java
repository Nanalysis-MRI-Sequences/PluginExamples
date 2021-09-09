package rs2d.process;

import org.junit.Assert;
import org.junit.Test;
import rs2d.spinlab.data.DataSetInterface;
import rs2d.spinlab.data.simulation.MriEllipsoidDatasetSimulator;
import rs2d.spinlab.tools.param.BooleanParam;
import rs2d.spinlab.tools.param.MriDefaultParams;

public class SignAlternationTest {
    private static final float[][][] expectedSignAlternationReal = {
            {{-1.17416709e-02f,  5.21570871e-03f,  6.00714374e-03f, -1.21530185e-02f,  1.18013157e-02f, -5.75955387e-03f, -4.73848615e-03f,  1.07565861e-02f},{-7.64426461e-03f,  1.94075671e-02f, -1.30653587e-02f,  4.56870564e-03f, -4.72686528e-03f,  1.32789461e-02f, -1.92092196e-02f,  7.14968977e-03f},{ 2.21215604e-02f, -3.10708714e-03f, -2.46853375e-02f,  3.63060083e-02f, -3.54973038e-02f,  2.40770670e-02f,  2.07285187e-03f, -2.07273680e-02f},{-1.62094541e-02f, -1.72013602e-02f,  3.66940322e-02f, -3.69721115e-02f,  3.64500052e-02f, -3.63121658e-02f,  1.77951054e-02f,  1.55536095e-02f},{ 1.54781702e-02f,  1.78406779e-02f, -3.63210153e-02f,  3.64172811e-02f, -3.68953507e-02f,  3.65733694e-02f, -1.70395490e-02f, -1.64072108e-02f},{-2.07854730e-02f,  2.15042375e-03f,  2.39725574e-02f, -3.53596563e-02f,  3.61308291e-02f, -2.44704479e-02f, -3.36140291e-03f,  2.24124902e-02f},{ 7.33258803e-03f, -1.93987143e-02f,  1.34819707e-02f, -4.94998566e-03f,  4.81751231e-03f, -1.33439244e-02f,  1.97180149e-02f, -7.98648371e-03f},{ 1.04761287e-02f, -4.46454288e-03f, -6.03232948e-03f,  1.20788211e-02f, -1.24411155e-02f,  6.31106715e-03f,  4.89189633e-03f, -1.13955323e-02f}},
            {{-4.98573485e-03f,  1.65329468e-02f, -1.27562989e-02f,  5.86886332e-03f, -4.79621159e-03f,  1.07942954e-02f, -1.54376443e-02f,  5.11295774e-03f},{ 1.30912612e-02f,  1.84247259e-02f, -3.30494839e-02f,  2.85894136e-02f, -2.86233145e-02f,  3.38424488e-02f, -1.99433369e-02f, -1.21873280e-02f},{ 1.99957686e-02f, -2.33331754e-02f, -1.84785746e-02f,  4.29216294e-02f, -4.47344479e-02f,  2.09898747e-02f,  2.33003844e-02f, -2.12277702e-02f},{-2.78917516e-02f, -1.64165815e-02f,  5.66624577e-02f, -4.40448867e-02f,  4.65956244e-02f, -6.17372993e-02f,  1.92910090e-02f,  2.82345298e-02f},{ 2.69731935e-02f,  1.93884352e-02f, -6.02459781e-02f,  4.73420710e-02f, -4.91535366e-02f,  6.48290707e-02f, -2.33491325e-02f, -2.58953615e-02f},{-1.89981496e-02f,  1.81728265e-02f,  2.42445942e-02f, -4.71184137e-02f,  4.77191612e-02f, -2.58702941e-02f, -1.62781322e-02f,  1.73767976e-02f},{-1.36261025e-02f, -1.50685716e-02f,  3.03279375e-02f, -2.85332766e-02f,  2.88852399e-02f, -3.14587045e-02f,  1.58921304e-02f,  1.42486133e-02f},{ 6.68277299e-03f, -1.84735652e-02f,  1.36926279e-02f, -5.23067424e-03f,  4.61816536e-03f, -1.21063282e-02f,  1.69542252e-02f, -5.81258095e-03f}},
            {{ 1.59200771e-02f, -1.08339497e-03f, -1.83059068e-02f,  2.51136877e-02f, -2.63362412e-02f,  2.10502092e-02f, -1.22998555e-03f, -1.48735987e-02f},{ 2.30700474e-02f, -2.28880356e-02f, -2.30771122e-02f,  5.02730381e-02f, -4.99521477e-02f,  2.16960734e-02f,  2.44350012e-02f, -2.31655714e-02f},{-1.98439801e-02f, -4.80301913e-02f,  1.27444747e-02f,  6.24467063e-02f, -6.07875112e-02f, -1.59243268e-02f,  5.00811693e-02f,  1.90368786e-02f},{-2.18401276e-02f,  2.67406915e-02f,  1.31712512e-01f, -1.48032977e-01f,  1.44754409e-01f, -1.24287291e-01f, -3.23067497e-02f,  2.27427516e-02f},{ 2.67661478e-02f, -3.50570655e-02f, -1.23441259e-01f,  1.40199300e-01f, -1.36607185e-01f,  1.15760081e-01f,  4.02618016e-02f, -2.71527609e-02f},{ 1.23361130e-02f,  6.18031042e-02f, -2.55704533e-02f, -5.19390429e-02f,  4.97326869e-02f,  2.92738982e-02f, -6.33045213e-02f, -1.25024782e-02f},{-2.03647297e-02f,  1.65966980e-02f,  2.73081058e-02f, -5.04723125e-02f,  5.03820245e-02f, -2.63092111e-02f, -1.81369626e-02f,  2.07648807e-02f},{-1.65639694e-02f,  2.35613078e-03f,  1.84361912e-02f, -2.79350139e-02f,  2.92329455e-02f, -2.10604925e-02f, -5.73062338e-04f,  1.63651011e-02f}},
            {{-1.05239999e-02f, -1.58412353e-02f,  2.67644289e-02f, -2.17716426e-02f,  2.21468073e-02f, -2.80578846e-02f,  1.74759399e-02f,  9.10199867e-03f},{-3.20848232e-02f, -1.53214924e-02f,  6.32604119e-02f, -5.81372642e-02f,  5.76167075e-02f, -6.21389512e-02f,  1.50720936e-02f,  3.08681198e-02f},{-1.69710274e-02f,  2.68043855e-02f,  1.19219259e-01f, -1.30111836e-01f,  1.30271661e-01f, -1.18445020e-01f, -2.92332687e-02f,  1.97278764e-02f},{ 4.50409610e-02f,  1.10608830e-01f,  7.45498730e-03f, -8.38943819e-01f,  8.39999861e-01f, -1.07041859e-02f, -1.07046223e-01f, -4.66737093e-02f},{-4.93598500e-02f, -1.04836788e-01f, -1.21219850e-02f,  8.43317011e-01f, -8.45676922e-01f,  1.63652174e-02f,  1.03031574e-01f,  4.84147872e-02f},{ 2.42381012e-02f, -3.63952168e-02f, -1.12088133e-01f,  1.24166538e-01f, -1.22070035e-01f,  1.09358769e-01f,  3.58504549e-02f, -2.18228818e-02f},{ 2.89941274e-02f,  1.93821387e-02f, -6.53317593e-02f,  5.83454031e-02f, -5.87047142e-02f,  6.54108930e-02f, -1.82307532e-02f, -3.04555260e-02f},{ 1.00506911e-02f,  1.56983806e-02f, -2.69424197e-02f,  2.32063452e-02f, -2.40549327e-02f,  2.82684651e-02f, -1.60908825e-02f, -1.08431159e-02f}},
            {{ 1.08431159e-02f,  1.60908825e-02f, -2.82684651e-02f,  2.40549327e-02f, -2.32063452e-02f,  2.69424197e-02f, -1.56983806e-02f, -1.00506911e-02f},{ 3.04555260e-02f,  1.82307532e-02f, -6.54108930e-02f,  5.87047142e-02f, -5.83454031e-02f,  6.53317593e-02f, -1.93821387e-02f, -2.89941274e-02f},{ 2.18228818e-02f, -3.58504549e-02f, -1.09358769e-01f,  1.22070035e-01f, -1.24166538e-01f,  1.12088133e-01f,  3.63952168e-02f, -2.42381012e-02f},{-4.84147872e-02f, -1.03031574e-01f, -1.63652174e-02f,  8.45676922e-01f, -8.43317011e-01f,  1.21219850e-02f,  1.04836788e-01f,  4.93598500e-02f},{ 4.66737093e-02f,  1.07046223e-01f,  1.07041859e-02f, -8.39999861e-01f,  8.38943819e-01f, -7.45498730e-03f, -1.10608830e-01f, -4.50409610e-02f},{-1.97278764e-02f,  2.92332687e-02f,  1.18445020e-01f, -1.30271661e-01f,  1.30111836e-01f, -1.19219259e-01f, -2.68043855e-02f,  1.69710274e-02f},{-3.08681198e-02f, -1.50720936e-02f,  6.21389512e-02f, -5.76167075e-02f,  5.81372642e-02f, -6.32604119e-02f,  1.53214924e-02f,  3.20848232e-02f},{-9.10199867e-03f, -1.74759399e-02f,  2.80578846e-02f, -2.21468073e-02f,  2.17716426e-02f, -2.67644289e-02f,  1.58412353e-02f,  1.05239999e-02f}},
            {{-1.63651011e-02f,  5.73062338e-04f,  2.10604925e-02f, -2.92329455e-02f,  2.79350139e-02f, -1.84361912e-02f, -2.35613078e-03f,  1.65639694e-02f},{-2.07648807e-02f,  1.81369626e-02f,  2.63092111e-02f, -5.03820245e-02f,  5.04723125e-02f, -2.73081058e-02f, -1.65966980e-02f,  2.03647297e-02f},{ 1.25024782e-02f,  6.33045213e-02f, -2.92738982e-02f, -4.97326869e-02f,  5.19390429e-02f,  2.55704533e-02f, -6.18031042e-02f, -1.23361130e-02f},{ 2.71527609e-02f, -4.02618016e-02f, -1.15760081e-01f,  1.36607185e-01f, -1.40199300e-01f,  1.23441259e-01f,  3.50570655e-02f, -2.67661478e-02f},{-2.27427516e-02f,  3.23067497e-02f,  1.24287291e-01f, -1.44754409e-01f,  1.48032977e-01f, -1.31712512e-01f, -2.67406915e-02f,  2.18401276e-02f},{-1.90368786e-02f, -5.00811693e-02f,  1.59243268e-02f,  6.07875112e-02f, -6.24467063e-02f, -1.27444747e-02f,  4.80301913e-02f,  1.98439801e-02f},{ 2.31655714e-02f, -2.44350012e-02f, -2.16960734e-02f,  4.99521477e-02f, -5.02730381e-02f,  2.30771122e-02f,  2.28880356e-02f, -2.30700474e-02f},{ 1.48735987e-02f,  1.22998555e-03f, -2.10502092e-02f,  2.63362412e-02f, -2.51136877e-02f,  1.83059068e-02f,  1.08339497e-03f, -1.59200771e-02f}},
            {{ 5.81258095e-03f, -1.69542252e-02f,  1.21063282e-02f, -4.61816536e-03f,  5.23067424e-03f, -1.36926279e-02f,  1.84735652e-02f, -6.68277299e-03f},{-1.42486133e-02f, -1.58921304e-02f,  3.14587045e-02f, -2.88852399e-02f,  2.85332766e-02f, -3.03279375e-02f,  1.50685716e-02f,  1.36261025e-02f},{-1.73767976e-02f,  1.62781322e-02f,  2.58702941e-02f, -4.77191612e-02f,  4.71184137e-02f, -2.42445942e-02f, -1.81728265e-02f,  1.89981496e-02f},{ 2.58953615e-02f,  2.33491325e-02f, -6.48290707e-02f,  4.91535366e-02f, -4.73420710e-02f,  6.02459781e-02f, -1.93884352e-02f, -2.69731935e-02f},{-2.82345298e-02f, -1.92910090e-02f,  6.17372993e-02f, -4.65956244e-02f,  4.40448867e-02f, -5.66624577e-02f,  1.64165815e-02f,  2.78917516e-02f},{ 2.12277702e-02f, -2.33003844e-02f, -2.09898747e-02f,  4.47344479e-02f, -4.29216294e-02f,  1.84785746e-02f,  2.33331754e-02f, -1.99957686e-02f},{ 1.21873280e-02f,  1.99433369e-02f, -3.38424488e-02f,  2.86233145e-02f, -2.85894136e-02f,  3.30494839e-02f, -1.84247259e-02f, -1.30912612e-02f},{-5.11295774e-03f,  1.54376443e-02f, -1.07942954e-02f,  4.79621159e-03f, -5.86886332e-03f,  1.27562989e-02f, -1.65329468e-02f,  4.98573485e-03f}},
            {{ 1.13955323e-02f, -4.89189633e-03f, -6.31106715e-03f,  1.24411155e-02f, -1.20788211e-02f,  6.03232948e-03f,  4.46454288e-03f, -1.04761287e-02f},{ 7.98648371e-03f, -1.97180149e-02f,  1.33439244e-02f, -4.81751231e-03f,  4.94998566e-03f, -1.34819707e-02f,  1.93987143e-02f, -7.33258803e-03f},{-2.24124902e-02f,  3.36140291e-03f,  2.44704479e-02f, -3.61308291e-02f,  3.53596563e-02f, -2.39725574e-02f, -2.15042375e-03f,  2.07854730e-02f},{ 1.64072108e-02f,  1.70395490e-02f, -3.65733694e-02f,  3.68953507e-02f, -3.64172811e-02f,  3.63210153e-02f, -1.78406779e-02f, -1.54781702e-02f},{-1.55536095e-02f, -1.77951054e-02f,  3.63121658e-02f, -3.64500052e-02f,  3.69721115e-02f, -3.66940322e-02f,  1.72013602e-02f,  1.62094541e-02f},{ 2.07273680e-02f, -2.07285187e-03f, -2.40770670e-02f,  3.54973038e-02f, -3.63060083e-02f,  2.46853375e-02f,  3.10708714e-03f, -2.21215604e-02f},{-7.14968977e-03f,  1.92092196e-02f, -1.32789461e-02f,  4.72686528e-03f, -4.56870564e-03f,  1.30653587e-02f, -1.94075671e-02f,  7.64426461e-03f},{-1.07565861e-02f,  4.73848615e-03f,  5.75955387e-03f, -1.18013157e-02f,  1.21530185e-02f, -6.00714374e-03f, -5.21570871e-03f,  1.17416709e-02f}}
    };

    private static final float[][][] expectedSignAlternationImag = {
            {{-2.34394053e-03f, -7.70277879e-04f,  2.84740070e-03f, -3.39683439e-03f,  2.61012205e-03f, -1.24084859e-03f, -2.86901873e-04f,  2.51223601e-03f},{-1.72397135e-03f,  9.64824174e-04f,  7.98216923e-04f, -1.50628205e-03f,  1.77502971e-03f, -1.97770257e-03f,  3.51260088e-04f,  1.69865187e-03f},{ 4.43188355e-04f,  3.47458601e-03f, -4.64244105e-03f,  3.49005372e-03f, -2.48474707e-03f,  3.07170068e-03f, -2.94481402e-03f, -6.01296694e-04f},{ 6.20504048e-04f, -3.42148799e-03f,  3.70808304e-03f, -1.93260394e-03f,  6.36349067e-05f,  4.90954630e-04f,  5.16684402e-04f, -4.37175168e-04f},{-1.72147857e-04f,  1.39782526e-04f, -1.18331983e-03f,  6.51497302e-04f,  1.20907128e-03f, -2.99101204e-03f,  2.72546051e-03f,  4.09339001e-05f},{ 1.07515280e-03f,  2.41813783e-03f, -2.49956841e-03f,  1.87697642e-03f, -2.85846468e-03f,  4.00025947e-03f, -2.83574474e-03f, -1.06479963e-03f},{-1.97633348e-03f, -2.28300572e-05f,  1.60106843e-03f, -1.35538683e-03f,  1.05128502e-03f, -3.17609407e-04f, -1.45973200e-03f,  2.22093927e-03f},{-2.45911742e-03f,  1.92358100e-04f,  1.37924319e-03f, -2.79233814e-03f,  3.62030196e-03f, -3.10710802e-03f,  1.05904901e-03f,  2.03501049e-03f}},
            {{-3.28094626e-03f,  2.75441858e-03f,  3.17473016e-04f, -2.36585249e-03f,  2.76261734e-03f, -1.53595782e-03f, -1.38321916e-03f,  2.27662177e-03f},{-3.09641937e-03f,  5.97767474e-03f, -2.58220993e-03f, -1.40102336e-03f,  1.04170779e-03f,  3.57882818e-03f, -6.50424332e-03f,  2.28906064e-03f},{ 8.19907214e-03f, -5.53498613e-03f, -3.68694469e-03f,  1.00103485e-02f, -1.02152624e-02f,  4.78856215e-03f,  3.58736317e-03f, -6.23942701e-03f},{-7.28577135e-03f,  5.93972654e-03f,  5.52738547e-04f, -7.16455785e-03f,  8.45126249e-03f, -4.12728724e-03f, -2.48216233e-03f,  6.03467372e-03f},{ 3.68185079e-03f, -4.35674009e-03f,  8.72410802e-05f,  7.40315033e-03f, -9.62369137e-03f,  4.12399667e-03f,  2.22919478e-03f, -4.16981514e-03f},{ 9.81942863e-04f, -1.37915573e-03f,  7.51717526e-04f, -3.06148843e-03f,  4.80562817e-03f, -3.02071744e-03f,  1.08311458e-03f,  5.16375389e-04f},{ 4.82960661e-05f,  6.02047190e-03f, -6.84975918e-03f,  5.06152400e-03f, -5.12975982e-03f,  6.36118444e-03f, -4.75758679e-03f, -9.76826453e-04f},{-2.99271148e-03f,  1.62582426e-03f,  1.44932018e-03f, -2.28827533e-03f,  1.34803372e-03f,  1.41683704e-04f, -2.30740262e-03f,  2.53829028e-03f}},
            {{ 2.86577244e-03f,  2.47860423e-03f, -5.52263162e-03f,  5.56492677e-03f, -4.95439837e-03f,  4.70881437e-03f, -2.42965283e-03f, -2.13042622e-03f},{ 6.67400890e-03f,  6.65826883e-04f, -1.16791381e-02f,  1.61752724e-02f, -1.58657804e-02f,  1.15511376e-02f, -1.70427538e-03f, -5.02652205e-03f},{-5.66095095e-03f, -3.40825952e-03f,  7.38844945e-03f, -7.79598320e-03f,  6.27300485e-03f, -5.79947177e-03f,  4.75705163e-03f,  2.87626645e-03f},{ 9.54281494e-03f, -6.96871802e-03f,  1.53739122e-04f,  9.39999849e-03f, -7.99556232e-03f, -2.24273582e-03f,  7.09292312e-03f, -8.14641928e-03f},{-1.20195941e-02f,  8.39055235e-03f,  4.43748846e-03f, -1.28322939e-02f,  1.27410186e-02f, -3.29173904e-03f, -1.03832211e-02f,  1.32601620e-02f},{ 6.26108513e-03f, -8.02568166e-03f, -4.65768266e-03f,  1.73871069e-02f, -1.80895896e-02f,  4.85734945e-03f,  9.89415229e-03f, -8.80830866e-03f},{ 2.26063334e-03f,  8.45158712e-05f, -2.23758285e-03f,  6.23537381e-04f, -1.77879618e-04f,  1.44190617e-03f, -2.18956996e-04f, -9.89025819e-04f},{ 3.80473460e-03f,  2.17213288e-03f, -5.73913519e-03f,  6.31936148e-03f, -6.30158415e-03f,  6.20204216e-03f, -3.14045252e-03f, -2.67671558e-03f}},
            {{ 9.14816293e-04f, -6.43022976e-03f,  6.63893535e-03f, -4.63119269e-03f,  3.21560745e-03f, -3.82012824e-03f,  4.47843568e-03f, -6.01769984e-04f},{-3.36924375e-03f, -8.07902046e-03f,  1.23891799e-02f, -8.69653022e-03f,  8.69552964e-03f, -1.33100937e-02f,  9.95528923e-03f,  2.20053143e-03f},{-4.69525075e-03f,  7.70298412e-03f, -2.18641977e-03f,  5.73109844e-03f, -3.22683239e-03f, -1.77550034e-03f, -6.69746967e-03f,  5.89524809e-03f},{-1.84029640e-03f,  7.51068035e-04f,  1.09367231e-02f, -3.33189927e-02f,  2.95072387e-02f, -3.06197388e-03f, -5.67091714e-03f,  1.63016656e-03f},{ 1.01833881e-02f, -3.83492187e-03f,  3.69888786e-03f, -2.08907365e-02f,  2.40828005e-02f, -1.11191160e-02f,  9.63982145e-03f, -1.12657242e-02f},{-1.09809714e-02f,  8.62878516e-03f,  7.00187660e-03f, -8.78494493e-03f,  7.34395250e-03f, -3.91290877e-03f, -1.11761980e-02f,  1.24165256e-02f},{-9.33068190e-04f, -8.68924177e-03f,  7.80466058e-03f,  8.68084570e-05f, -4.74893886e-04f, -6.40745280e-03f,  7.41054215e-03f,  5.96979878e-04f},{-4.45076369e-04f, -4.28964123e-03f,  4.50697728e-03f, -4.27721247e-03f,  5.42434735e-03f, -7.19107452e-03f,  6.64902022e-03f, -6.83151861e-04f}},
            {{-6.83151861e-04f,  6.64902022e-03f, -7.19107452e-03f,  5.42434735e-03f, -4.27721247e-03f,  4.50697728e-03f, -4.28964123e-03f, -4.45076369e-04f},{ 5.96979878e-04f,  7.41054215e-03f, -6.40745280e-03f, -4.74893886e-04f,  8.68084570e-05f,  7.80466058e-03f, -8.68924177e-03f, -9.33068190e-04f},{ 1.24165256e-02f, -1.11761980e-02f, -3.91290877e-03f,  7.34395250e-03f, -8.78494493e-03f,  7.00187660e-03f,  8.62878516e-03f, -1.09809714e-02f},{-1.12657242e-02f,  9.63982145e-03f, -1.11191160e-02f,  2.40828005e-02f, -2.08907365e-02f,  3.69888786e-03f, -3.83492187e-03f,  1.01833881e-02f},{ 1.63016656e-03f, -5.67091714e-03f, -3.06197388e-03f,  2.95072387e-02f, -3.33189927e-02f,  1.09367231e-02f,  7.51068035e-04f, -1.84029640e-03f},{ 5.89524809e-03f, -6.69746967e-03f, -1.77550034e-03f, -3.22683239e-03f,  5.73109844e-03f, -2.18641977e-03f,  7.70298412e-03f, -4.69525075e-03f},{ 2.20053143e-03f,  9.95528923e-03f, -1.33100937e-02f,  8.69552964e-03f, -8.69653022e-03f,  1.23891799e-02f, -8.07902046e-03f, -3.36924375e-03f},{-6.01769984e-04f,  4.47843568e-03f, -3.82012824e-03f,  3.21560745e-03f, -4.63119269e-03f,  6.63893535e-03f, -6.43022976e-03f,  9.14816293e-04f}},
            {{-2.67671558e-03f, -3.14045252e-03f,  6.20204216e-03f, -6.30158415e-03f,  6.31936148e-03f, -5.73913519e-03f,  2.17213288e-03f,  3.80473460e-03f},{-9.89025819e-04f, -2.18956996e-04f,  1.44190617e-03f, -1.77879618e-04f,  6.23537381e-04f, -2.23758285e-03f,  8.45158712e-05f,  2.26063334e-03f},{-8.80830866e-03f,  9.89415229e-03f,  4.85734945e-03f, -1.80895896e-02f,  1.73871069e-02f, -4.65768266e-03f, -8.02568166e-03f,  6.26108513e-03f},{ 1.32601620e-02f, -1.03832211e-02f, -3.29173904e-03f,  1.27410186e-02f, -1.28322939e-02f,  4.43748846e-03f,  8.39055235e-03f, -1.20195941e-02f},{-8.14641928e-03f,  7.09292312e-03f, -2.24273582e-03f, -7.99556232e-03f,  9.39999849e-03f,  1.53739122e-04f, -6.96871802e-03f,  9.54281494e-03f},{ 2.87626645e-03f,  4.75705163e-03f, -5.79947177e-03f,  6.27300485e-03f, -7.79598320e-03f,  7.38844945e-03f, -3.40825952e-03f, -5.66095095e-03f},{-5.02652205e-03f, -1.70427538e-03f,  1.15511376e-02f, -1.58657804e-02f,  1.61752724e-02f, -1.16791381e-02f,  6.65826883e-04f,  6.67400890e-03f},{-2.13042622e-03f, -2.42965283e-03f,  4.70881437e-03f, -4.95439837e-03f,  5.56492677e-03f, -5.52263162e-03f,  2.47860423e-03f,  2.86577244e-03f}},
            {{ 2.53829028e-03f, -2.30740262e-03f,  1.41683704e-04f,  1.34803372e-03f, -2.28827533e-03f,  1.44932018e-03f,  1.62582426e-03f, -2.99271148e-03f},{-9.76826453e-04f, -4.75758679e-03f,  6.36118444e-03f, -5.12975982e-03f,  5.06152400e-03f, -6.84975918e-03f,  6.02047190e-03f,  4.82960661e-05f},{ 5.16375389e-04f,  1.08311458e-03f, -3.02071744e-03f,  4.80562817e-03f, -3.06148843e-03f,  7.51717526e-04f, -1.37915573e-03f,  9.81942863e-04f},{-4.16981514e-03f,  2.22919478e-03f,  4.12399667e-03f, -9.62369137e-03f,  7.40315033e-03f,  8.72410802e-05f, -4.35674009e-03f,  3.68185079e-03f},{ 6.03467372e-03f, -2.48216233e-03f, -4.12728724e-03f,  8.45126249e-03f, -7.16455785e-03f,  5.52738547e-04f,  5.93972654e-03f, -7.28577135e-03f},{-6.23942701e-03f,  3.58736317e-03f,  4.78856215e-03f, -1.02152624e-02f,  1.00103485e-02f, -3.68694469e-03f, -5.53498613e-03f,  8.19907214e-03f},{ 2.28906064e-03f, -6.50424332e-03f,  3.57882818e-03f,  1.04170779e-03f, -1.40102336e-03f, -2.58220993e-03f,  5.97767474e-03f, -3.09641937e-03f},{ 2.27662177e-03f, -1.38321916e-03f, -1.53595782e-03f,  2.76261734e-03f, -2.36585249e-03f,  3.17473016e-04f,  2.75441858e-03f, -3.28094626e-03f}},
            {{ 2.03501049e-03f,  1.05904901e-03f, -3.10710802e-03f,  3.62030196e-03f, -2.79233814e-03f,  1.37924319e-03f,  1.92358100e-04f, -2.45911742e-03f},{ 2.22093927e-03f, -1.45973200e-03f, -3.17609407e-04f,  1.05128502e-03f, -1.35538683e-03f,  1.60106843e-03f, -2.28300572e-05f, -1.97633348e-03f},{-1.06479963e-03f, -2.83574474e-03f,  4.00025947e-03f, -2.85846468e-03f,  1.87697642e-03f, -2.49956841e-03f,  2.41813783e-03f,  1.07515280e-03f},{ 4.09339001e-05f,  2.72546051e-03f, -2.99101204e-03f,  1.20907128e-03f,  6.51497302e-04f, -1.18331983e-03f,  1.39782526e-04f, -1.72147857e-04f},{-4.37175168e-04f,  5.16684402e-04f,  4.90954630e-04f,  6.36349067e-05f, -1.93260394e-03f,  3.70808304e-03f, -3.42148799e-03f,  6.20504048e-04f},{-6.01296694e-04f, -2.94481402e-03f,  3.07170068e-03f, -2.48474707e-03f,  3.49005372e-03f, -4.64244105e-03f,  3.47458601e-03f,  4.43188355e-04f},{ 1.69865187e-03f,  3.51260088e-04f, -1.97770257e-03f,  1.77502971e-03f, -1.50628205e-03f,  7.98216923e-04f,  9.64824174e-04f, -1.72397135e-03f},{ 2.51223601e-03f, -2.86901873e-04f, -1.24084859e-03f,  2.61012205e-03f, -3.39683439e-03f,  2.84740070e-03f, -7.70277879e-04f, -2.34394053e-03f}}
    };

    private static final float[][][] expectedSignAlternationMultiPlanarReal = {
            {{0.0f, -0.0f, 0.0f, -0.0f, 0.0f, -0.0f, 0.0f, -0.0f},{-0.0f, 0.0f, -0.0f, 0.0f, -0.0f, 0.0f, -0.0f, 0.0f},{0.0f, -0.0f, 0.0f, -0.0f, 0.0f, -0.0f, 0.0f, -0.0f},{-0.0f, 0.0f, -0.0f, 0.0f, -0.0f, 0.0f, -0.0f, 0.0f},{0.0f, -0.0f, 0.0f, -0.0f, 0.0f, -0.0f, 0.0f, -0.0f},{-0.0f, 0.0f, -0.0f, 0.0f, -0.0f, 0.0f, -0.0f, 0.0f},{0.0f, -0.0f, 0.0f, -0.0f, 0.0f, -0.0f, 0.0f, -0.0f},{-0.0f, 0.0f, -0.0f, 0.0f, -0.0f, 0.0f, -0.0f, 0.0f},},{{0.0093751075f, -0.008388853f, 0.0021173926f, 0.0028959191f, -0.0028959191f, -0.0021173926f, 0.008388853f, -0.0093751075f},{-0.0015624389f, -0.016732952f, 0.027147524f, -0.02721215f, 0.02721215f, -0.027147524f, 0.016732952f, 0.0015624389f},{-0.019830387f, 0.025798341f, 0.011030954f, -0.056670867f, 0.056670867f, -0.011030954f, -0.025798341f, 0.019830387f},{0.028219286f, 0.0017905179f, -0.10646776f, 0.20858905f, -0.20858905f, 0.10646776f, -0.0017905179f, -0.028219286f},{-0.028219286f, -0.0017905179f, 0.10646776f, -0.20858905f, 0.20858905f, -0.10646776f, 0.0017905179f, 0.028219286f},{0.019830387f, -0.025798341f, -0.011030954f, 0.056670867f, -0.056670867f, 0.011030954f, 0.025798341f, -0.019830387f},{0.0015624389f, 0.016732952f, -0.027147524f, 0.02721215f, -0.02721215f, 0.027147524f, -0.016732952f, -0.0015624389f},{-0.0093751075f, 0.008388853f, -0.0021173926f, -0.0028959191f, 0.0028959191f, 0.0021173926f, -0.008388853f, 0.0093751075f},},{{-0.011561761f, 0.013322032f, -0.0012311605f, -0.008282878f, 0.008282878f, 0.0012311605f, -0.013322032f, 0.011561761f},{0.00027831364f, 0.02713238f, -0.020051232f, -0.00019023148f, 0.00019023148f, 0.020051232f, -0.02713238f, -0.00027831364f},{0.02914492f, 0.011786791f, -0.065783694f, 0.039811943f, -0.039811943f, 0.065783694f, -0.011786791f, -0.02914492f},{-0.018905059f, -0.06259561f, -0.045009617f, 0.35189214f, -0.35189214f, 0.045009617f, 0.06259561f, 0.018905059f},{0.018905059f, 0.06259561f, 0.045009617f, -0.35189214f, 0.35189214f, -0.045009617f, -0.06259561f, -0.018905059f},{-0.02914492f, -0.011786791f, 0.065783694f, -0.039811943f, 0.039811943f, -0.065783694f, 0.011786791f, 0.02914492f},{-0.00027831364f, -0.02713238f, 0.020051232f, 0.00019023148f, -0.00019023148f, -0.020051232f, 0.02713238f, 0.00027831364f},{0.011561761f, -0.013322032f, 0.0012311605f, 0.008282878f, -0.008282878f, -0.0012311605f, 0.013322032f, -0.011561761f},},{{0.010189014f, 0.010234979f, -0.02090707f, 0.019293725f, -0.019293725f, 0.02090707f, -0.010234979f, -0.010189014f},{0.02008424f, 0.007812752f, -0.040949166f, 0.04080168f, -0.04080168f, 0.040949166f, -0.007812752f, -0.02008424f},{0.013480414f, -0.03188209f, -0.059683338f, 0.085069895f, -0.085069895f, 0.059683338f, 0.03188209f, -0.013480414f},{-0.036770485f, -0.048031986f, 0.023407225f, 0.39738014f, -0.39738014f, -0.023407225f, 0.048031986f, 0.036770485f},{0.036770485f, 0.048031986f, -0.023407225f, -0.39738014f, 0.39738014f, 0.023407225f, -0.048031986f, -0.036770485f},{-0.013480414f, 0.03188209f, 0.059683338f, -0.085069895f, 0.085069895f, -0.059683338f, -0.03188209f, 0.013480414f},{-0.02008424f, -0.007812752f, 0.040949166f, -0.04080168f, 0.04080168f, -0.040949166f, 0.007812752f, 0.02008424f},{-0.010189014f, -0.010234979f, 0.02090707f, -0.019293725f, 0.019293725f, -0.02090707f, 0.010234979f, 0.010189014f},},{{0.018003784f, -0.0018737867f, -0.020986361f, 0.02725247f, -0.02094812f, 0.02035826f, -0.006020328f, -0.01833882f},{0.025420422f, -0.00352027f, -0.04746077f, 0.05257446f, -0.05517328f, 0.048359f, 0.008173686f, -0.024460094f},{-0.0089926105f, -0.031325974f, -0.03498463f, 0.06773379f, -0.07973716f, 0.03801554f, 0.044034436f, 0.011847875f},{-0.026250925f, -0.047018506f, 0.037396412f, 0.44781712f, -0.43813953f, -0.040128604f, 0.03602145f, 0.023786072f},{0.023786072f, 0.03602145f, -0.040128604f, -0.43813953f, 0.44781712f, 0.037396412f, -0.047018506f, -0.026250925f},{0.011847875f, 0.044034436f, 0.03801554f, -0.07973716f, 0.06773379f, -0.03498463f, -0.031325974f, -0.0089926105f},{-0.024460094f, 0.008173686f, 0.048359f, -0.05517328f, 0.05257446f, -0.04746077f, -0.00352027f, 0.025420422f},{-0.01833882f, -0.006020328f, 0.02035826f, -0.02094812f, 0.02725247f, -0.020986361f, -0.0018737867f, 0.018003784f},},{{0.010189014f, 0.010234979f, -0.02090707f, 0.019293725f, -0.019293725f, 0.02090707f, -0.010234979f, -0.010189014f},{0.02008424f, 0.007812752f, -0.040949166f, 0.04080168f, -0.04080168f, 0.040949166f, -0.007812752f, -0.02008424f},{0.013480414f, -0.03188209f, -0.059683338f, 0.085069895f, -0.085069895f, 0.059683338f, 0.03188209f, -0.013480414f},{-0.036770485f, -0.048031986f, 0.023407225f, 0.39738014f, -0.39738014f, -0.023407225f, 0.048031986f, 0.036770485f},{0.036770485f, 0.048031986f, -0.023407225f, -0.39738014f, 0.39738014f, 0.023407225f, -0.048031986f, -0.036770485f},{-0.013480414f, 0.03188209f, 0.059683338f, -0.085069895f, 0.085069895f, -0.059683338f, -0.03188209f, 0.013480414f},{-0.02008424f, -0.007812752f, 0.040949166f, -0.04080168f, 0.04080168f, -0.040949166f, 0.007812752f, 0.02008424f},{-0.010189014f, -0.010234979f, 0.02090707f, -0.019293725f, 0.019293725f, -0.02090707f, 0.010234979f, 0.010189014f},},{{-0.011561761f, 0.013322032f, -0.0012311605f, -0.008282878f, 0.008282878f, 0.0012311605f, -0.013322032f, 0.011561761f},{0.00027831364f, 0.02713238f, -0.020051232f, -0.00019023148f, 0.00019023148f, 0.020051232f, -0.02713238f, -0.00027831364f},{0.02914492f, 0.011786791f, -0.065783694f, 0.039811943f, -0.039811943f, 0.065783694f, -0.011786791f, -0.02914492f},{-0.018905059f, -0.06259561f, -0.045009617f, 0.35189214f, -0.35189214f, 0.045009617f, 0.06259561f, 0.018905059f},{0.018905059f, 0.06259561f, 0.045009617f, -0.35189214f, 0.35189214f, -0.045009617f, -0.06259561f, -0.018905059f},{-0.02914492f, -0.011786791f, 0.065783694f, -0.039811943f, 0.039811943f, -0.065783694f, 0.011786791f, 0.02914492f},{-0.00027831364f, -0.02713238f, 0.020051232f, 0.00019023148f, -0.00019023148f, -0.020051232f, 0.02713238f, 0.00027831364f},{0.011561761f, -0.013322032f, 0.0012311605f, 0.008282878f, -0.008282878f, -0.0012311605f, 0.013322032f, -0.011561761f},},{{0.0093751075f, -0.008388853f, 0.0021173926f, 0.0028959191f, -0.0028959191f, -0.0021173926f, 0.008388853f, -0.0093751075f},{-0.0015624389f, -0.016732952f, 0.027147524f, -0.02721215f, 0.02721215f, -0.027147524f, 0.016732952f, 0.0015624389f},{-0.019830387f, 0.025798341f, 0.011030954f, -0.056670867f, 0.056670867f, -0.011030954f, -0.025798341f, 0.019830387f},{0.028219286f, 0.0017905179f, -0.10646776f, 0.20858905f, -0.20858905f, 0.10646776f, -0.0017905179f, -0.028219286f},{-0.028219286f, -0.0017905179f, 0.10646776f, -0.20858905f, 0.20858905f, -0.10646776f, 0.0017905179f, 0.028219286f},{0.019830387f, -0.025798341f, -0.011030954f, 0.056670867f, -0.056670867f, 0.011030954f, 0.025798341f, -0.019830387f},{0.0015624389f, 0.016732952f, -0.027147524f, 0.02721215f, -0.02721215f, 0.027147524f, -0.016732952f, -0.0015624389f},{-0.0093751075f, 0.008388853f, -0.0021173926f, -0.0028959191f, 0.0028959191f, 0.0021173926f, -0.008388853f, 0.0093751075f}}};

    private static final float[][][] expectedSignAlternationMultiPlanarImag = {
            {{0.0f, -0.0f, 0.0f, -0.0f, 0.0f, -0.0f, 0.0f, -0.0f},{-0.0f, 0.0f, -0.0f, 0.0f, -0.0f, 0.0f, -0.0f, 0.0f},{0.0f, -0.0f, 0.0f, -0.0f, 0.0f, -0.0f, 0.0f, -0.0f},{-0.0f, 0.0f, -0.0f, 0.0f, -0.0f, 0.0f, -0.0f, 0.0f},{0.0f, -0.0f, 0.0f, -0.0f, 0.0f, -0.0f, 0.0f, -0.0f},{-0.0f, 0.0f, -0.0f, 0.0f, -0.0f, 0.0f, -0.0f, 0.0f},{0.0f, -0.0f, 0.0f, -0.0f, 0.0f, -0.0f, 0.0f, -0.0f},{-0.0f, 0.0f, -0.0f, 0.0f, -0.0f, 0.0f, -0.0f, 0.0f},},{{0.00090304605f, 0.0013050764f, -0.0030322191f, 0.0038285288f, -0.0038285288f, 0.0030322191f, -0.0013050764f, -0.00090304605f},{0.0023033572f, -0.0035182312f, 0.002921089f, -0.0018089708f, 0.0018089708f, -0.002921089f, 0.0035182312f, -0.0023033572f},{-0.0021234332f, 0.0006157486f, 0.002907515f, -0.005889289f, 0.005889289f, -0.002907515f, -0.0006157486f, 0.0021234332f},{0.00055184186f, 0.0007596671f, -0.0029283604f, 0.004611725f, -0.004611725f, 0.0029283604f, -0.0007596671f, -0.00055184186f},{0.00055184186f, 0.0007596671f, -0.0029283604f, 0.004611725f, -0.004611725f, 0.0029283604f, -0.0007596671f, -0.00055184186f},{-0.0021234332f, 0.0006157486f, 0.002907515f, -0.005889289f, 0.005889289f, -0.002907515f, -0.0006157486f, 0.0021234332f},{0.0023033572f, -0.0035182312f, 0.002921089f, -0.0018089708f, 0.0018089708f, -0.002921089f, 0.0035182312f, -0.0023033572f},{0.00090304605f, 0.0013050764f, -0.0030322191f, 0.0038285288f, -0.0038285288f, 0.0030322191f, -0.0013050764f, -0.00090304605f},},{{-0.003539544f, 0.0007324007f, 0.0027822223f, -0.004437316f, 0.004437316f, -0.0027822223f, -0.0007324007f, 0.003539544f},{-0.0022647344f, 0.0040123835f, -0.00065332133f, -0.0026185294f, 0.0026185294f, 0.00065332133f, -0.0040123835f, 0.0022647344f},{0.0021898826f, 0.0026148036f, -0.0049596485f, 0.0014183391f, -0.0014183391f, 0.0049596485f, -0.0026148036f, -0.0021898826f},{-8.226336e-06f, -0.0017081676f, -0.0019880133f, 0.00905419f, -0.00905419f, 0.0019880133f, 0.0017081676f, 8.226336e-06f},{-8.226336e-06f, -0.0017081676f, -0.0019880133f, 0.00905419f, -0.00905419f, 0.0019880133f, 0.0017081676f, 8.226336e-06f},{0.0021898826f, 0.0026148036f, -0.0049596485f, 0.0014183391f, -0.0014183391f, 0.0049596485f, -0.0026148036f, -0.0021898826f},{-0.0022647344f, 0.0040123835f, -0.00065332133f, -0.0026185294f, 0.0026185294f, 0.00065332133f, -0.0040123835f, 0.0022647344f},{-0.003539544f, 0.0007324007f, 0.0027822223f, -0.004437316f, 0.004437316f, -0.0027822223f, -0.0007324007f, 0.003539544f},},{{-0.0014958216f, 0.00631957f, -0.006497381f, 0.004972985f, -0.004972985f, 0.006497381f, -0.00631957f, 0.0014958216f},{0.002862282f, 0.003018449f, -0.005577489f, 0.0038468074f, -0.0038468074f, 0.005577489f, -0.003018449f, -0.002862282f},{0.0051462776f, -0.0046944083f, -0.0019340408f, 0.0017777355f, -0.0017777355f, 0.0019340408f, 0.0046944083f, -0.0051462776f},{-0.00298092f, 0.00068410137f, -0.0030490167f, 0.013662526f, -0.013662526f, 0.0030490167f, -0.00068410137f, 0.00298092f},{-0.00298092f, 0.00068410137f, -0.0030490167f, 0.013662526f, -0.013662526f, 0.0030490167f, -0.00068410137f, 0.00298092f},{0.0051462776f, -0.0046944083f, -0.0019340408f, 0.0017777355f, -0.0017777355f, 0.0019340408f, 0.0046944083f, -0.0051462776f},{0.002862282f, 0.003018449f, -0.005577489f, 0.0038468074f, -0.0038468074f, 0.005577489f, -0.003018449f, -0.002862282f},{-0.0014958216f, 0.00631957f, -0.006497381f, 0.004972985f, -0.004972985f, 0.006497381f, -0.00631957f, 0.0014958216f},},{{0.0014153217f, 0.003964689f, -0.005365085f, 0.009514325f, -0.0053839907f, -0.0024409029f, -0.002589234f, -0.0011702033f},{0.0019169734f, 0.0010307732f, -0.009720922f, 0.006878411f, -0.00043240562f, 0.014009555f, -0.014111472f, -0.007576803f},{0.011431021f, -0.014246976f, 0.0042476305f, -0.008652771f, -0.00050011877f, 0.0027790056f, 0.020074993f, -0.009594305f},{-0.005167688f, 0.017357308f, -0.01000556f, 0.019781569f, -0.022032246f, -0.0040006866f, 0.0018523976f, 0.012864025f},{-0.012864025f, -0.0018523976f, 0.0040006866f, 0.022032246f, -0.019781569f, 0.01000556f, -0.017357308f, 0.005167688f},{0.009594305f, -0.020074993f, -0.0027790056f, 0.00050011877f, 0.008652771f, -0.0042476305f, 0.014246976f, -0.011431021f},{0.007576803f, 0.014111472f, -0.014009555f, 0.00043240562f, -0.006878411f, 0.009720922f, -0.0010307732f, -0.0019169734f},{0.0011702033f, 0.002589234f, 0.0024409029f, 0.0053839907f, -0.009514325f, 0.005365085f, -0.003964689f, -0.0014153217f},},{{-0.0014958216f, 0.00631957f, -0.006497381f, 0.004972985f, -0.004972985f, 0.006497381f, -0.00631957f, 0.0014958216f},{0.002862282f, 0.003018449f, -0.005577489f, 0.0038468074f, -0.0038468074f, 0.005577489f, -0.003018449f, -0.002862282f},{0.0051462776f, -0.0046944083f, -0.0019340408f, 0.0017777355f, -0.0017777355f, 0.0019340408f, 0.0046944083f, -0.0051462776f},{-0.00298092f, 0.00068410137f, -0.0030490167f, 0.013662526f, -0.013662526f, 0.0030490167f, -0.00068410137f, 0.00298092f},{-0.00298092f, 0.00068410137f, -0.0030490167f, 0.013662526f, -0.013662526f, 0.0030490167f, -0.00068410137f, 0.00298092f},{0.0051462776f, -0.0046944083f, -0.0019340408f, 0.0017777355f, -0.0017777355f, 0.0019340408f, 0.0046944083f, -0.0051462776f},{0.002862282f, 0.003018449f, -0.005577489f, 0.0038468074f, -0.0038468074f, 0.005577489f, -0.003018449f, -0.002862282f},{-0.0014958216f, 0.00631957f, -0.006497381f, 0.004972985f, -0.004972985f, 0.006497381f, -0.00631957f, 0.0014958216f},},{{-0.003539544f, 0.0007324007f, 0.0027822223f, -0.004437316f, 0.004437316f, -0.0027822223f, -0.0007324007f, 0.003539544f},{-0.0022647344f, 0.0040123835f, -0.00065332133f, -0.0026185294f, 0.0026185294f, 0.00065332133f, -0.0040123835f, 0.0022647344f},{0.0021898826f, 0.0026148036f, -0.0049596485f, 0.0014183391f, -0.0014183391f, 0.0049596485f, -0.0026148036f, -0.0021898826f},{-8.226336e-06f, -0.0017081676f, -0.0019880133f, 0.00905419f, -0.00905419f, 0.0019880133f, 0.0017081676f, 8.226336e-06f},{-8.226336e-06f, -0.0017081676f, -0.0019880133f, 0.00905419f, -0.00905419f, 0.0019880133f, 0.0017081676f, 8.226336e-06f},{0.0021898826f, 0.0026148036f, -0.0049596485f, 0.0014183391f, -0.0014183391f, 0.0049596485f, -0.0026148036f, -0.0021898826f},{-0.0022647344f, 0.0040123835f, -0.00065332133f, -0.0026185294f, 0.0026185294f, 0.00065332133f, -0.0040123835f, 0.0022647344f},{-0.003539544f, 0.0007324007f, 0.0027822223f, -0.004437316f, 0.004437316f, -0.0027822223f, -0.0007324007f, 0.003539544f},},{{0.00090304605f, 0.0013050764f, -0.0030322191f, 0.0038285288f, -0.0038285288f, 0.0030322191f, -0.0013050764f, -0.00090304605f},{0.0023033572f, -0.0035182312f, 0.002921089f, -0.0018089708f, 0.0018089708f, -0.002921089f, 0.0035182312f, -0.0023033572f},{-0.0021234332f, 0.0006157486f, 0.002907515f, -0.005889289f, 0.005889289f, -0.002907515f, -0.0006157486f, 0.0021234332f},{0.00055184186f, 0.0007596671f, -0.0029283604f, 0.004611725f, -0.004611725f, 0.0029283604f, -0.0007596671f, -0.00055184186f},{0.00055184186f, 0.0007596671f, -0.0029283604f, 0.004611725f, -0.004611725f, 0.0029283604f, -0.0007596671f, -0.00055184186f},{-0.0021234332f, 0.0006157486f, 0.002907515f, -0.005889289f, 0.005889289f, -0.002907515f, -0.0006157486f, 0.0021234332f},{0.0023033572f, -0.0035182312f, 0.002921089f, -0.0018089708f, 0.0018089708f, -0.002921089f, 0.0035182312f, -0.0023033572f},{0.00090304605f, 0.0013050764f, -0.0030322191f, 0.0038285288f, -0.0038285288f, 0.0030322191f, -0.0013050764f, -0.00090304605f}}};

    @Test
    public void testSignAlternation() {
        MriEllipsoidDatasetSimulator simulator = new MriEllipsoidDatasetSimulator();
        simulator.getAcquisitionParameters().get(MriDefaultParams.ACQUISITION_MATRIX_DIMENSION_1D.name()).setValue(8);
        simulator.getAcquisitionParameters().get(MriDefaultParams.ACQUISITION_MATRIX_DIMENSION_2D.name()).setValue(8);
        simulator.getAcquisitionParameters().get(MriDefaultParams.ACQUISITION_MATRIX_DIMENSION_3D.name()).setValue(8);
        DataSetInterface dataset = simulator.simulate();

        SignAlternation process = new SignAlternation(dataset);
        process.execute(process.getParam());

        float[][][] real = dataset.getData(0).getRealPart()[0];
        float[][][] imag = dataset.getData(0).getImaginaryPart()[0];

        Assert.assertEquals("Test direct sign alternation process number of slices.",
                SignAlternationTest.expectedSignAlternationReal.length, real.length);
        Assert.assertEquals("Test direct sign alternation process number of lines.",
                SignAlternationTest.expectedSignAlternationReal[0][0].length, real[0].length);
        Assert.assertEquals("Test direct sign alternation process number of elements.",
                SignAlternationTest.expectedSignAlternationReal[0][0].length, real[0][0].length);

        for (int k = 0; k < real.length; k++) {
            for (int j = 0; j < real[0].length; j++) {
                Assert.assertArrayEquals(String.format("Test direct sign alternation process for real part (slice %d, line %d)", k, j),
                        SignAlternationTest.expectedSignAlternationReal[k][j], real[k][j], 1e-2f);
                Assert.assertArrayEquals(String.format("Test direct sign alternation process for imaginary part (slice %d, line %d)", k, j),
                        SignAlternationTest.expectedSignAlternationImag[k][j], imag[k][j], 1e-2f);
            }
        }
    }

    @Test
    public void testSignAlternationWithMultiPlanarData() {
        MriEllipsoidDatasetSimulator simulator = new MriEllipsoidDatasetSimulator();
        simulator.getAcquisitionParameters().get(MriDefaultParams.ACQUISITION_MATRIX_DIMENSION_1D.name()).setValue(8);
        simulator.getAcquisitionParameters().get(MriDefaultParams.ACQUISITION_MATRIX_DIMENSION_2D.name()).setValue(8);
        simulator.getAcquisitionParameters().get(MriDefaultParams.ACQUISITION_MATRIX_DIMENSION_3D.name()).setValue(8);
        simulator.getAcquisitionParameters().put(MriDefaultParams.MULTI_PLANAR_EXCITATION.name(),
                new BooleanParam(MriDefaultParams.MULTI_PLANAR_EXCITATION.name(), true, ""));
        DataSetInterface dataset = simulator.simulate();

        SignAlternation process = new SignAlternation(dataset);
        process.execute(process.getParam());

        float[][][] real = dataset.getData(0).getRealPart()[0];
        float[][][] imag = dataset.getData(0).getImaginaryPart()[0];

        Assert.assertEquals("Test direct sign alternation process number of slices.",
                SignAlternationTest.expectedSignAlternationMultiPlanarReal.length, real.length);
        Assert.assertEquals("Test direct sign alternation process number of lines.",
                SignAlternationTest.expectedSignAlternationMultiPlanarReal[0][0].length, real[0].length);
        Assert.assertEquals("Test direct sign alternation process number of elements.",
                SignAlternationTest.expectedSignAlternationMultiPlanarReal[0][0].length, real[0][0].length);

        for (int k = 0; k < real.length; k++) {
            for (int j = 0; j < real[0].length; j++) {
                Assert.assertArrayEquals(String.format("Test direct sign alternation process for real part (slice %d, line %d)", k, j),
                        SignAlternationTest.expectedSignAlternationMultiPlanarReal[k][j], real[k][j], 1e-2f);
                Assert.assertArrayEquals(String.format("Test direct sign alternation process for imaginary part (slice %d, line %d)", k, j),
                        SignAlternationTest.expectedSignAlternationMultiPlanarImag[k][j], imag[k][j], 1e-2f);
            }
        }
    }
}