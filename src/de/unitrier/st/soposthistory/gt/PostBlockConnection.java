package de.unitrier.st.soposthistory.gt;

import java.util.HashSet;
import java.util.Set;

public class PostBlockConnection {
    private PostBlockLifeSpanVersion left;
    private PostBlockLifeSpanVersion right;

    public PostBlockConnection(PostBlockLifeSpanVersion left, PostBlockLifeSpanVersion right) {
        this.left = left;
        this.right = right;
    }

    /**
     * Checks if the elements in the two sets of PostBlockConnections are equal according to PostBlockConnection.equals.
     * @param set1 First set of PostBlockConnections
     * @param set2 Second set of PostBlockConnections
     * @return True, if set1 and set2 are equal according to the above definition
     */
    public static boolean equals(Set<PostBlockConnection> set1, Set<PostBlockConnection> set2) {
        if (set1.size() != set2.size()) {
            return false;
        }
        for (PostBlockConnection current : set1) {
            boolean equal = false;
            for (PostBlockConnection other : set2) {
                if (current.equals(other)) {
                    equal = true;
                    break;
                }
            }
            if (!equal) {
                return false;
            }
        }
        return true;
    }

    /**
     * Intersection of two sets of PostBlockConnections.
     * @param set1 First set of PostBlockConnections
     * @param set2 Second set of PostBlockConnections
     * @return Intersection of set1 and set2
     */
    public static Set<PostBlockConnection> intersection(Set<PostBlockConnection> set1, Set<PostBlockConnection> set2) {
        Set<PostBlockConnection> intersection = new HashSet<>();
        for (PostBlockConnection current : set1) {
            for (PostBlockConnection other : set2) {
                if (current.equals(other)) {
                    intersection.add(current);
                    break;
                }
            }
        }
        return intersection;
    }

    /**
     * Difference of two sets of PostBlockConnections, i.e. elements that are in the first set, but not in second.
     * @param set1 First set of PostBlockConnections
     * @param set2 Second set of PostBlockConnections
     * @return Difference of set1 and set2
     */
    public static Set<PostBlockConnection> difference(Set<PostBlockConnection> set1, Set<PostBlockConnection> set2) {
        Set<PostBlockConnection> difference = new HashSet<>(set1);
        Set<PostBlockConnection> markedForRemoval = new HashSet<>();
        for (PostBlockConnection current : difference) {
            for (PostBlockConnection other : set2) {
                if (current.equals(other)) {
                    markedForRemoval.add(current);
                    break;
                }
            }
        }
        for (PostBlockConnection current : markedForRemoval) {
            difference.remove(current);
        }
        return difference;
    }

    /**
     * Union of the sets of PostBlockConnections.
     * @param set1 First set of PostBlockConnections
     * @param set2 Second set of PostBlockConnections
     * @return Union of set1 and set2
     */
    public static Set<PostBlockConnection> union(Set<PostBlockConnection> set1, Set<PostBlockConnection> set2) {
        Set<PostBlockConnection> union = new HashSet<>(set1);
        for (PostBlockConnection current : set2) {
            if (!contains(union, current)) {
                union.add(current);
            }
        }
        return union;
    }

    /**
     * Check if a set contains a connection that is equal according to PostBlockConnection.equals
     * @param set Set of PostBlockConnections
     * @param connection The PostBlockConnection to check
     * @return True, if the set contains the connection
     */
    public static boolean contains(Set<PostBlockConnection> set, PostBlockConnection connection) {
        for (PostBlockConnection current : set) {
            if (current.equals(connection)) {
                return true;
            }
        }
        return false;
    }

    public static Set<PostBlockConnection> getTruePositives(Set<PostBlockConnection> postBlockConnections,
                                                            Set<PostBlockConnection> postBlockConnectionsGT) {
        return PostBlockConnection.intersection(postBlockConnectionsGT, postBlockConnections);
    }

    public static Set<PostBlockConnection> getFalsePositives(Set<PostBlockConnection> postBlockConnections,
                                                            Set<PostBlockConnection> postBlockConnectionsGT) {
        return PostBlockConnection.difference(postBlockConnections, postBlockConnectionsGT);
    }

    public static int getTrueNegatives(Set<PostBlockConnection> postBlockConnections,
                                       Set<PostBlockConnection> postBlockConnectionsGT,
                                       int possibleConnectionsGT) {
        return possibleConnectionsGT - (PostBlockConnection.union(postBlockConnectionsGT, postBlockConnections).size());
    }

    public static Set<PostBlockConnection> getFalseNegatives(Set<PostBlockConnection> postBlockConnections,
                                                             Set<PostBlockConnection> postBlockConnectionsGT) {
        return PostBlockConnection.difference(postBlockConnectionsGT, postBlockConnections);
    }

    /**
     * Two PostBlockConnections are equal of their left and right PostBlockLifeSpanVersions are equal, i.e. they have
     * the same postId, postHistoryId, postBlockTypeId, and localId..
     * @param other other PostBlockConnection to compare this one to
     * @return True, if the PostBlockConnections are equal according to the above definition
     */
    public boolean equals(PostBlockConnection other) {
        return (this.left.equals(other.left) && this.right.equals(other.right));
    }

    public PostBlockLifeSpanVersion getLeft() {
        return left;
    }

    public PostBlockLifeSpanVersion getRight() {
        return right;
    }
}
