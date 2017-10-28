package de.unitrier.st.soposthistory.gt;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PostBlockConnection {
    private PostBlockLifeSpanVersion left;
    private PostBlockLifeSpanVersion right;

    public PostBlockConnection(PostBlockLifeSpanVersion left, PostBlockLifeSpanVersion right) {
        this.left = left;
        this.right = right;
    }

    public static Set<PostBlockConnection> extractFromPostBlockLifeSpan(PostBlockLifeSpan lifeSpan) {
        Set<PostBlockConnection> postBlockConnections = new HashSet<>();
        // in a life span, all versions except the first one have predecessors
        for (int i=1; i<lifeSpan.size(); i++) {
            postBlockConnections.add(new PostBlockConnection(lifeSpan.get(i-1), lifeSpan.get(i)));
        }
        return postBlockConnections;
    }

    public static Set<PostBlockConnection> extractFromPostBlockLifeSpan(List<PostBlockLifeSpan> lifeSpans) {
        Set<PostBlockConnection> postBlockConnections = new HashSet<>();
        for (PostBlockLifeSpan lifeSpan : lifeSpans) {
            postBlockConnections.addAll(extractFromPostBlockLifeSpan(lifeSpan));
        }
        return postBlockConnections;
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

    public boolean equals(PostBlockConnection other) {
        return (this.left.equals(other.left) && this.right.equals(other.right));
    }
}
