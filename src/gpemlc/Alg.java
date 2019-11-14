package gpemlc;

import org.apache.commons.configuration.Configuration;

import gpemlc.mutator.Mutator;
import gpemlc.recombinator.Crossover;
import mulan.classifier.InvalidDataException;
import mulan.classifier.MultiLabelLearner;
import mulan.classifier.MultiLabelOutput;
import mulan.classifier.transformation.LabelPowerset2;
import mulan.core.MulanException;
import mulan.data.MultiLabelInstances;
import net.sf.jclec.algorithm.classic.SGE;
import net.sf.jclec.stringtree.StringTreeCreator;
import net.sf.jclec.util.random.IRandGen;
import weka.classifiers.trees.J48;
import weka.core.Instances;

public class Alg extends SGE {

	/**
	 * 
	 */
	private static final long serialVersionUID = -790335501425435317L;

	/**
	 * Max number of children at each node
	 */
	int maxChildren = 2;
	
	/**
	 * Max depth of the tree
	 */
	int maxDepth = 4;
	
	/**
	 * Full training dataset
	 */
	MultiLabelInstances fullTrainData;
	
	/**
	 * Training datasets
	 */
	MultiLabelInstances [] trainData;
	
	/**
	 * Ratio of instances sampled at each train data
	 */
	double sampleRatio;
	
	/**
	 * Test dataset
	 */
	MultiLabelInstances testData;
	
	/**
	 * Number of MLC
	 */
	int nMLC = 10;
	
	/**
	 * Array of classifiers
	 */
	MultiLabelLearner[] classifiers;
	
	@Override
	public void configure(Configuration configuration) {
		super.configure(configuration);
		
		String datasetTrainFileName = configuration.getString("dataset.train-dataset");
		String datasetTestFileName = configuration.getString("dataset.test-dataset");
		String datasetXMLFileName = configuration.getString("dataset.xml");
		
		sampleRatio = configuration.getDouble("sampling-ratio");
		
		IRandGen randgen = randGenFactory.createRandGen();
		
		fullTrainData = null;
		testData = null;
		try {
			fullTrainData = new MultiLabelInstances(datasetTrainFileName, datasetXMLFileName);
			Instances evalData = fullTrainData.getDataSet();
			testData = new MultiLabelInstances(datasetTestFileName, datasetXMLFileName);
			
			trainData = new MultiLabelInstances[nMLC];
			classifiers = new MultiLabelLearner[nMLC];
			for(int p=0; p<nMLC; p++) {
				trainData[p] = MulanUtils.sampleData(fullTrainData, sampleRatio, randgen);
				classifiers[p] = new LabelPowerset2(new J48());
				((LabelPowerset2)classifiers[p]).setSeed(1);
				classifiers[p].build(trainData[p]);				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		((StringTreeCreator)provider).setMaxChildren(maxChildren);
		((StringTreeCreator)provider).setMaxDepth(maxDepth);
		((StringTreeCreator)provider).setnMax(nMLC);
		
		((Mutator)mutator.getDecorated()).setMaxTreeDepth(maxDepth);
		((Mutator)mutator.getDecorated()).setnChilds(maxChildren);
		((Mutator)mutator.getDecorated()).setnMax(nMLC);
		
		((Crossover)recombinator.getDecorated()).setMaxTreeDepth(maxDepth);
		
		((Evaluator)evaluator).setClassifiers(classifiers);
		((Evaluator)evaluator).setFullTrainData(fullTrainData);
	}
	
	@Override
	protected void doInit() {
		super.doInit();
		
	}
	
}
