/*
 *    SketchBasedNaiveBayes.java
 *    Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 *    @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program. If not, see <http://www.gnu.org/licenses/>.
 *    
 */
package moa.classifiers.bayes.TestCMS2;
import CountMinSketch2.*;


import com.github.javacliparser.FloatOption;

import moa.classifiers.AbstractClassifier;
import moa.classifiers.MultiClassClassifier;
import moa.core.Measurement;
import com.yahoo.labs.samoa.instances.Instance;
import java.util.HashMap;
import moa.classifiers.core.attributeclassobservers.AttributeClassObserver;
import moa.classifiers.core.attributeclassobservers.GaussianNumericAttributeClassObserver;
import moa.classifiers.core.attributeclassobservers.NominalAttributeClassObserver;
import moa.core.AutoExpandVector;
/*
deals with nominal attribute values, stream LED
*/

public class CMNaiveBayesProb2 extends AbstractClassifier implements MultiClassClassifier {

   
	private static final long serialVersionUID = 1L;

	public FloatOption deltaOption = new FloatOption("deltaFraction",
			'd',
			"delta.",
			0.1, 0.0, 1.0);

	public FloatOption epsilonOption = new FloatOption("epsilonFraction",
			'e',
			"epsilon.",
			0.01, 0.0, 1.0);

	@SuppressWarnings("hiding")
	public static final String classifierPurposeString = "Naive Bayes classifier: performs classic bayesian prediction while making naive assumption that all inputs are independent.";

	protected double[] observedClassDistribution;
	
	protected double[][] attributeObserversSum;

	protected boolean start = true;
	
	protected double observedClassSum = 0;

	protected int numClasses;
	
	private final int NUMVALUESATTRIBUTE = 5;

	protected CountMinSketch2 cmsketch; 
	protected HashMap <String, Integer> list;
       
        protected double error;
        protected int NumAtt ;
        protected double diffprob;
        protected AutoExpandVector<AttributeClassObserver> attributeObservers;
	@Override
	public void resetLearningImpl() {
		//this.observedClassDistribution = new DoubleVector();
		//this.attributeObservers = new AutoExpandVector<AttributeClassObserver>();
		start = true;
	}

	@Override
	public void trainOnInstanceImpl(Instance inst) {
		
		if (start == true) {
			 this.observedClassDistribution = new double[inst.numClasses()]; 
			 //this.attributeObservers = new double[inst.numAttributes()][inst.numClasses()][NUMVALUESATTRIBUTE];
			 this.attributeObserversSum = new double[inst.numAttributes()][inst.numClasses()];
			 this.observedClassSum  = 0.0;
			 this.start = false;
                         this.error = 0.0;
                         this.diffprob=0.0;
                         this.numClasses = inst.numClasses();
                         this.cmsketch = new CountMinSketch2(this.epsilonOption.getValue(),
                                 this.deltaOption.getValue(),1);
			 this.NumAtt = inst.numAttributes();
                         this.list = new HashMap<>();
                         this.attributeObservers = new AutoExpandVector<AttributeClassObserver>();
		}
                this.observedClassDistribution[(int) inst.classValue()] += inst.weight();
		this.observedClassSum +=inst.weight();
                
		for (int i = 0; i < inst.numAttributes() - 1; i++) {
                    AttributeClassObserver obs = this.attributeObservers.get(i);
                    if (obs == null) {
                        obs = inst.attribute(i).isNominal() ? newNominalClassObserver()
                                    : newNumericClassObserver();
                        this.attributeObservers.set(i, obs);
                        }
               obs.observeAttributeClass(inst.value(i), (int) inst.classValue(), inst.weight());
                
                    String updateS = Integer.toString(i)+
                                     Double.toString(inst.value(i))+
                                     Double.toString(inst.classValue());
			this.cmsketch.add(updateS,1); //? int weight
			this.attributeObserversSum[i][(int)inst.classValue()] +=inst.weight();
                        if (!this.list.containsKey(updateS))
                             this.list.put(updateS, 1);
                        else {
                             int val = (int) this.list.get(updateS);
                             this.list.put(updateS, (val+1));
              
                         }          
		}
	}
                
		
        protected AttributeClassObserver newNominalClassObserver() {
        return new NominalAttributeClassObserver();
        }

        protected AttributeClassObserver newNumericClassObserver() {
        return new GaussianNumericAttributeClassObserver();
        }

	
        @Override
        public double[] getVotesForInstance(Instance inst) {
		return doNaiveBayesPrediction(inst);
	}

	@Override
	protected Measurement[] getModelMeasurementsImpl() {
		return null;
	}

	@Override
	public void getModelDescription(StringBuilder out, int indent) {

	}

        @Override
	public boolean isRandomizable() {
		return false;
	}


	public double[] doNaiveBayesPrediction(Instance inst) {
		double[] votes = new double[inst.numClasses()];
                double[] votesTrue = new double[inst.numClasses()]; //Prediction for incremental NaiveBayes
		if (this.start == true)
				return votes; 
		for (int classIndex = 0; classIndex < votes.length; classIndex++) {
                    votes[classIndex] = this.observedClassDistribution[classIndex]/ this.observedClassSum;
                    votesTrue[classIndex] = this.observedClassDistribution[classIndex]/this.observedClassSum;
                    for (int attIndex = 0; attIndex < inst.numAttributes() - 1; attIndex++) {
                        AttributeClassObserver obs = this.attributeObservers.get(attIndex);
                             if ((obs != null) && !inst.isMissing(attIndex)) 
                    votesTrue[classIndex] *= obs.probabilityOfAttributeValueGivenClass(inst.value(attIndex), classIndex);
                                String chaine = Integer.toString(attIndex)+
                                         Double.toString(inst.value(attIndex))+
                                         Double.toString(classIndex);
                    votes[classIndex] *= ((double) this.cmsketch.estimateCount(chaine))
                                                    /this.attributeObserversSum[attIndex][classIndex];
				 int count = (int) this.cmsketch.estimateCount(chaine);	
                                 Integer countlst;
                                     if (!this.list.containsKey(chaine)) 
                                         countlst= 0;
                                     else 
                                         countlst =  this.list.get(chaine);
                                     // this.list.put(chaine, (countlst-1));
                                this.error+= Math.abs((double)count - countlst.doubleValue());
                                 //  System.out.println("ici c'est sketch"+count);
                                  // System.out.println("ici c'est liste"+countlst);
		    }
                          System.out.println("fractional error"+(this.error/(this.observedClassSum*(this.NumAtt-1)*votes.length)));
                         
               
                }
	      if (indexVal(votesTrue,maxTable(votesTrue)) == indexVal(votes,maxTable(votes)))
                    this.diffprob += Math.abs(maxTable(votesTrue)-maxTable(votes)); 
                else 
                    this.diffprob+= 1;
               System.out.println("Global diff prob"+this.diffprob/this.observedClassSum);
		// TODO: need logic to prevent underflow?
		return votes;
	}
        
        
        public int indexVal (double [] arr, double value){
            int k=0;
            for(int i=0;i<arr.length;i++){
                if(arr[i]==value){
                     k=i;
                     break;
                }
            }
        return k;
        }
        
        public double maxTable(double [] Tab ){
            double max =0.0;
            for (int i=0 ; i<Tab.length ; i++) {
                if(Tab[i] > max) 
                    max = Tab [i];
            }
                
            return max;
        }		
		
         // 
    
        
        
	

	public void manageMemory(int currentByteSize, int maxByteSize) {
		// TODO Auto-generated method stub

	}

}