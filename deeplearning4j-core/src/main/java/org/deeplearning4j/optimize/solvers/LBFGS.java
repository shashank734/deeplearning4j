/*
 *
 *  * Copyright 2015 Skymind,Inc.
 *  *
 *  *    Licensed under the Apache License, Version 2.0 (the "License");
 *  *    you may not use this file except in compliance with the License.
 *  *    You may obtain a copy of the License at
 *  *
 *  *        http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *    Unless required by applicable law or agreed to in writing, software
 *  *    distributed under the License is distributed on an "AS IS" BASIS,
 *  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *    See the License for the specific language governing permissions and
 *  *    limitations under the License.
 *
 */

package org.deeplearning4j.optimize.solvers;

import org.deeplearning4j.berkeley.Pair;
import org.deeplearning4j.nn.api.Model;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.gradient.Gradient;
import org.deeplearning4j.optimize.api.IterationListener;
import org.deeplearning4j.optimize.api.StepFunction;
import org.deeplearning4j.optimize.api.TerminationCondition;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;


import java.util.Collection;
import java.util.LinkedList;

/**
 * LBFGS
 * @author Adam Gibson
 */
public class LBFGS extends BaseOptimizer {

    private int m = 4;

    public LBFGS(NeuralNetConfiguration conf, StepFunction stepFunction, Collection<IterationListener> iterationListeners, Model model) {
        super(conf, stepFunction, iterationListeners, model);
    }

    public LBFGS(NeuralNetConfiguration conf, StepFunction stepFunction, Collection<IterationListener> iterationListeners, Collection<TerminationCondition> terminationConditions, Model model) {
        super(conf, stepFunction, iterationListeners, terminationConditions, model);
    }

    @Override
    public void setupSearchState(Pair<Gradient, Double> pair) {
        super.setupSearchState(pair);
        INDArray gradient = (INDArray) searchState.get(GRADIENT_KEY);
        INDArray params = (INDArray) searchState.get(PARAMS_KEY);
        searchState.put("s", new LinkedList<>()); // holds parameters differences
        searchState.put("y", new LinkedList<>()); // holds gradients differences
        searchState.put("rho", new LinkedList());
        searchState.put("alpha", Nd4j.create(m));
        searchState.put("oldparams", params.dup());
        searchState.put("oldgradient", gradient.dup());

        //initial direction should be normal
        searchState.put(SEARCH_DIR, gradient.dup().mul(Nd4j.norm2(gradient).rdivi(1.0).getDouble(0)).negi());
    }

    @Override
    protected void postFirstStep(INDArray gradient) {
        super.postFirstStep(gradient);
        if(step == 0.0) {
            log.debug("Unable to step in that direction...resetting");
            setupSearchState(model.gradientAndScore());
            step = 1.0;
        }

    }

    // Numerical Optimization section 7.2
    // s = parameters differences (old & current)
    // y = gradient differences (old & current)
    // gamma = Hessian approximation
    @Override
    public void preProcessLine(INDArray gradient) {
        INDArray oldParameters = (INDArray) searchState.get("oldparams");
        INDArray params = model.params();
        oldParameters.assign(params.sub(oldParameters));

        INDArray oldGradient = (INDArray) searchState.get("oldgradient");
        oldGradient.subi(gradient);

        LinkedList<Double> rho = (LinkedList<Double>) searchState.get("rho");
        LinkedList<INDArray> s = (LinkedList<INDArray>) searchState.get("s");
        LinkedList<INDArray> y = (LinkedList<INDArray>) searchState.get("y");

        double sy = Nd4j.getBlasWrapper().dot(oldParameters,oldGradient.transpose()) + Nd4j.EPS_THRESHOLD;
        double yy = Nd4j.getBlasWrapper().dot(oldGradient.transpose(), oldGradient) + Nd4j.EPS_THRESHOLD;

        rho.add(1.0 / sy);
        s.add(oldParameters);
        y.add(oldGradient);

        if(s.size() != y.size())
            throw new IllegalStateException("Gradient and parameter sizes are not equal");

        INDArray alpha = (INDArray) searchState.get("alpha");
        // First work backwards, from the most recent difference vectors
        for (int i = s.size() - 1; i >= 0; i--) {
            if(s.get(i).length() != gradient.length())
                throw new IllegalStateException("Gradients and parameters length not equal");
            if(i >= alpha.length())
                break;
            if(i > rho.size())
                throw new IllegalStateException("Parameter size is greater than  searchDirection size");
            alpha.putScalar(i, rho.get(i) * Nd4j.getBlasWrapper().dot(gradient, s.get(i).transpose()));
            Nd4j.getBlasWrapper().level1().axpy(gradient.length(),-1.0f * alpha.getFloat(i), gradient, y.get(i));

        }

        double gamma = sy / yy;
        INDArray searchDir = gradient.dup().muli(gamma);

        // Now work forwards, from the oldest to the newest difference vectors
        for (int i = 0; i < y.size(); i++) {
            if(i >= alpha.length())
                break;
            double beta = rho.get(i) * Nd4j.getBlasWrapper().dot(y.get(i).transpose(), searchDir);
        }

        oldParameters.assign(params);
        oldGradient.assign(gradient);

        INDArray searchDirection = (INDArray) searchState.get(SEARCH_DIR);
        searchDirection.assign(searchDir.negi());

    }

    @Override
    public void postStep() {

    }
}
