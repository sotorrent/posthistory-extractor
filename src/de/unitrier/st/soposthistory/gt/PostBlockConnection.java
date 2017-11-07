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

    public static boolean equals(Set<PostBlockConnection> set1, Set<PostBlockConnection> set2) {
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

    public static Set<PostBlockConnection> union(Set<PostBlockConnection> set1, Set<PostBlockConnection> set2) {
        // TODO: add test case for this method
        Set<PostBlockConnection> union = new HashSet<>(set1);
        for (PostBlockConnection current : set2) {
            if (!contains(union, current)) {
                union.add(current);
            }
        }
        return union;
    }

    public static boolean contains(Set<PostBlockConnection> set, PostBlockConnection connection) {
        for (PostBlockConnection current : set) {
            if (current.equals(connection)) {
                return true;
            }
        }
        return false;
    }

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
