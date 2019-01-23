package cwts.networkanalysis;

import cwts.util.Arrays;
import java.util.Random;
import java.util.LinkedList;

/**
 * Fast local moving algorithm.
 *
 * <p>
 * The fast local moving algorithm first adds all nodes in a network to a
 * queue. It then removes a node from the queue. The node is moved to the
 * cluster that results in the largest increase in the quality function. If the
 * current cluster assignment of the node is already optimal, the node is not
 * moved. If the node is moved to a different cluster, the neighbors of the
 * node that do not belong to the node's new cluster and that are not yet in
 * the queue are added to the queue. The algorithm continues removing nodes
 * from the queue until the queue is empty.
 * </p>
 *
 * <p>
 * The fast local moving algorithm provides a fast variant of the {@link
 * StandardLocalMovingAlgorithm}.
 * </p>
 *
 * @author Ludo Waltman
 * @author Nees Jan van Eck
 * @author Vincent Traag
 */
public class FastLocalMovingAlgorithmParallel extends IterativeCPMClusteringAlgorithm
{
    /**
     * Random number generator.
     */
    protected Random random;

    /**
     * Constructs a fast local moving algorithm.
     */
    public FastLocalMovingAlgorithmParallel()
    {
        this(new Random());
    }

    /**
     * Constructs a fast local moving algorithm.
     *
     * @param random Random number generator
     */
    public FastLocalMovingAlgorithmParallel(Random random)
    {
        this(DEFAULT_RESOLUTION, DEFAULT_N_ITERATIONS, random);
    }

    /**
     * Constructs a fast local moving algorithm for a specified resolution
     * parameter and number of iterations.
     *
     * @param resolution  Resolution parameter
     * @param nIterations Number of iterations
     * @param random      Random number generator
     */
    public FastLocalMovingAlgorithmParallel(double resolution, int nIterations, Random random)
    {
        super(resolution, nIterations);

        this.random = random;
    }

    /**
     * Improves a clustering by performing one iteration of the fast local
     * moving algorithm.
     *
     * <p>
     * The fast local moving algorithm first adds all nodes in a network to a
     * queue. It then removes a node from the queue. The node is moved to the
     * cluster that results in the largest increase in the quality function. If
     * the current cluster assignment of the node is already optimal, the node
     * is not moved. If the node is moved to a different cluster, the neighbors
     * of the node that do not belong to the node's new cluster and that are
     * not yet in the queue are added to the queue. The algorithm continues
     * removing nodes from the queue until the queue is empty.
     * </p>
     *
     * @param network    Network
     * @param clustering Clustering
     *
     * @return Boolean indicating whether the clustering has been improved
     */
    protected boolean improveClusteringOneIteration(Network network, Clustering clustering)
    {
        int numberOfWorkers = 7;

        if (network.nNodes == 1)
            return false;

        Runnable[] taskList = new Runnable[network.nNodes];
        LinkedList<Integer> taskQueue = new LinkedList<Integer>();

        ClusterDataManager clusterDataManager = new ClusterDataManager(network, clustering, taskList, taskQueue);

        double[] clusterWeights = clusterDataManager.getClusterWeights();
        int[] nNodesPerCluster = clusterDataManager.getnNodesPerCluster();

        for (int i = 0; i < network.nNodes; i++) {
            taskList[i] = new MoveNodeTask (network, clustering, clusterDataManager, clusterWeights, nNodesPerCluster, resolution, i);
        }

        int[] nodeOrder = Arrays.generateRandomPermutation(network.nNodes, random);
        for (int i = 0; i < nodeOrder.length; i++) {
            taskQueue.add(nodeOrder[i]);
        }

        WorkerThread[] workers = new WorkerThread[numberOfWorkers];
        for (WorkerThread worker : workers) {
            worker = new WorkerThread(taskList, taskQueue);
            worker.start();
        }

        // for (Worker worker : workers) {
        //     try {
        //         if(null != worker) worker.join();
        //     }catch (InterruptedException ex) {
        //         for (Worker workerThatShouldStop : workers) {
        //             workerThatShouldStop.interrupt();
        //         }
        //     }
        // }

        while (!taskQueue.isEmpty()) {
            synchronized (taskQueue) {
                try {
                    taskQueue.wait(500);
                } catch (InterruptedException ex) {
                    System.out.println(ex);
                }
            }
        }

        boolean update = clusterDataManager.getSomeThingChanged();

        if (update)
            clustering.removeEmptyClusters();

        return update;
    }
}