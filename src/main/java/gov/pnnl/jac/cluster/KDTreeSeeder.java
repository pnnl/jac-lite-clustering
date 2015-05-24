/*
 * KDTreeSeeder.java
 * 
 * JAC: Java Analytic Components
 * 
 * For information contact Randall Scarberry, randall.scarberry@pnl.gov
 * 
 * Notice: This computer software was prepared by Battelle Memorial Institute, 
 * hereinafter the Contractor, under Contract No. DE-AC05-76RL0 1830 with the 
 * Department of Energy (DOE).  All rights in the computer software are 
 * reserved by DOE on behalf of the United States Government and the Contractor
 * as provided in the Contract.  You are authorized to use this computer 
 * software for Governmental purposes but it is not to be released or 
 * distributed to the public.  NEITHER THE GOVERNMENT NOR THE CONTRACTOR MAKES 
 * ANY WARRANTY, EXPRESS OR IMPLIED, OR ASSUMES ANY LIABILITY FOR THE USE OF 
 * THIS SOFTWARE.  This notice including this sentence must appear on any 
 * copies of this computer software.
 */
package gov.pnnl.jac.cluster;

import java.util.*;

import cern.colt.map.HashFunctions;

import gov.pnnl.jac.geom.*;

public class KDTreeSeeder implements ClusterSeeder {

    private long mSeed;

    // The maximum number of coordinates randomly sampled to initialize
    // cluster centers at the beginning of clustering.
    private int mInitCentersSamplingLimit = 100000;

    public KDTreeSeeder(long seed) {
        mSeed = seed;
    }

    public int hashCode() {
        return HashFunctions.hash(mSeed);
    }

    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (o instanceof KDTreeSeeder) {
            KDTreeSeeder other = (KDTreeSeeder) o;
            return this.mSeed == other.mSeed;
        }
        return false;
    }

    public CoordinateList generateSeeds(final CoordinateList coords,
            final int numSeeds) {

        if (numSeeds <= 0) {
            throw new IllegalArgumentException();
        }

        int coordCount = coords.getCoordinateCount();
        int coordLen = coords.getDimensionCount();

        int samplingLimit = Math.min(coordCount, mInitCentersSamplingLimit);

        int[] indices = new int[coordCount];
        for (int i = 0; i < coordCount; i++) {
            indices[i] = i;
        }

        long seed = mSeed != 0L ? mSeed : System.currentTimeMillis();

        Random random = new Random(seed);

        if (samplingLimit < coordCount) {
            for (int i = 0, m = coordCount; m > 0; i++, m--) {
                int j = i + random.nextInt(m);
                if (i != j) {
                    indices[i] ^= indices[j];
                    indices[j] ^= indices[i];
                    indices[i] ^= indices[j];
                }
            }
            int[] tmpIndices = new int[samplingLimit];
            System.arraycopy(indices, 0, tmpIndices, 0, samplingLimit);
            indices = tmpIndices;
        }

        // The number of splits necessary to generate a sufficient number of
        // bottom nodes for seeding the protoclusters. The formala assumes
        // the tree doesn't run out of unique coordinates before splitting
        // down
        // to the level chosen. That is, it assumes no leaf nodes are
        // generated.
        // Leaf nodes cannot be split.
        int splits = (int) Math.ceil(Math.log((double) numSeeds)
                / Math.log(2.0));

        // Create a multi-res kd-tree split down to the necessary level.
        MultiResKDTreeNode root = MultiResKDTreeNode.createKDTree(coords,
                indices, splits);

        // Get the nodes at the bottom level.
        MultiResKDTreeNode[] nodes = root.getNodesAtLevel(splits);
        int numNodes = nodes.length;

        // If the number of nodes is insufficient, it's because duplicate
        // coordinates caused leaf nodes to be formed before the split level
        // was reached, so the number of nodes at the split level was less
        // than expected.
        if (numNodes < numSeeds) {

            List<MultiResKDTreeNode> nodeList = new ArrayList<MultiResKDTreeNode>(
                    numSeeds);
            for (int i = 0; i < numNodes; i++) {
                nodeList.add(nodes[i]);
            }

            // Get the leaf nodes above the split level.
            MultiResKDTreeNode[] leafNodes = root.getLeafNodes(splits - 1);
            int numLeafNodes = leafNodes.length;
            for (int i = 0; i < numLeafNodes; i++) {
                nodeList.add(leafNodes[i]);
            }

            leafNodes = null; // Done with it.

            // Keep splitting another level until we have enough nodes or
            // there are no more nodes to split.
            while (nodeList.size() < numSeeds) {
                // Split another level. This will recurse down to the
                // unsplit
                // non-leaf nodes and split them.
                root.split(++splits);
                MultiResKDTreeNode[] tmp = root.getNodesAtLevel(splits);
                if (tmp.length == 0) {
                    // Nothing could be split because all nodes at the
                    // bottom
                    // were leaves.
                    // Break out of the while or will be stuck in an
                    // infinite loop.
                    break;
                }
                for (int i = 0; i < tmp.length; i++) {
                    nodeList.add(tmp[i]);
                }
            }

            numNodes = nodeList.size();
            nodes = new MultiResKDTreeNode[numNodes];
            nodeList.toArray(nodes);
        }

        if (numSeeds < numNodes) {
            // Not all nodes can be used to seed the clusters. Need to
            // shuffle so the ones used will be random.
            for (int i = numNodes - 1; i > 0; i--) {
                int j = random.nextInt(i + 1);
                if (i != j) {
                    MultiResKDTreeNode tmp = nodes[i];
                    nodes[i] = nodes[j];
                    nodes[j] = tmp;
                }
            }
        }
        
        int seedsFound = Math.min(numSeeds, numNodes);

        CoordinateList seeds = new SimpleCoordinateList(coordLen, seedsFound);
        double[] buffer = new double[coordLen];
        
        for (int i = 0; i < seedsFound; i++) {
            seeds.setCoordinates(i, nodes[i].getCenter(buffer));
        }

        return seeds;
    }

    public long getRandomSeed() {
        return mSeed;
    }
}
