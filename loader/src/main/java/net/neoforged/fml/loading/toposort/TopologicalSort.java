/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.loading.toposort;

import com.google.common.base.Preconditions;
import com.google.common.graph.Graph;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

/**
 * Provides a topological sort algorithm.
 *
 * <p>While this algorithm is used for mod loading in forge, it can be
 * utilized in other fashions, e.g. topology-based registry loading, prioritization
 * for renderers, and even mod module loading.
 */
public final class TopologicalSort<T> {
    // TODO: this comment here needs updating, in particular the complexity is now O(VE) I believe
    /**
     * A breath-first-search based topological sort.
     *
     * <p>Compared to the depth-first-search version, it does not reverse the graph
     * and supports custom secondary ordering specified by a comparator. It also utilizes the
     * recently introduced Guava Graph API, which is more straightforward than the old directed
     * graph.
     *
     * <p>The graph to sort must be directed, must not allow self loops, and must not contain
     * cycles. {@link IllegalArgumentException} will be thrown otherwise.
     *
     * <p>When {@code null} is used for the comparator and multiple nodes have no
     * prerequisites, the order depends on the iteration order of the set returned by the
     * {@link Graph#successors(Object)} call, which is random by default.
     *
     * <p>Given the number of edges {@code E} and the number of vertexes {@code V},
     * the time complexity of a sort without a secondary comparator is {@code O(E + V)}.
     * With a secondary comparator of time complexity {@code O(T)}, the overall time
     * complexity would be {@code O(E + TV log(V))}. As a result, the comparator should
     * be as efficient as possible.
     *
     * <p>Examples of topological sort usage can be found in Forge test code.
     *
     * @param graph      the graph to sort
     * @param comparator the secondary comparator, may be null
     * @param <T>        the node type of the graph
     * @return the ordered nodes from the graph
     * @throws IllegalArgumentException if the graph is undirected or allows self loops
     * @throws CyclePresentException    if the graph contains cycles
     */
    public static <T> List<T> topologicalSort(Graph<T> graph, @Nullable Comparator<? super T> comparator) throws IllegalArgumentException {
        Preconditions.checkArgument(graph.isDirected(), "Cannot topologically sort an undirected graph!");
        Preconditions.checkArgument(!graph.allowsSelfLoops(), "Cannot topologically sort a graph with self loops!");

        // Check for cycles
        Set<Set<T>> components = new StronglyConnectedComponentDetector<>(graph).getComponents();
        components.removeIf(set -> set.size() < 2);
        if (!components.isEmpty()) {
            //noinspection unchecked
            throw new CyclePresentException((Set<Set<?>>) (Set<?>) components);
        }

        var toposort = new TopologicalSort<>(graph, comparator);
        toposort.resolveSubgraph(new ArrayList<>(graph.nodes()));
        return toposort.result;
    }

    private final Graph<T> graph;
    @Nullable
    private final Comparator<? super T> comparator;
    private final Set<T> taken = new HashSet<>(); // nodes that have been added to `result` already
    private final List<T> result = new ArrayList<>();

    private TopologicalSort(Graph<T> graph, @Nullable Comparator<? super T> comparator) {
        this.graph = graph;
        this.comparator = comparator;
    }

    private void resolveSubgraph(List<T> nodes) {
        while (!nodes.isEmpty()) {
            var min = removeMin(nodes);
            if (!taken.contains(min)) {
                // Process all dependencies of `min`
                var subGraph = new ArrayList<T>();
                collectChildren(min, subGraph);
                resolveSubgraph(subGraph);
                // Add `min`
                result.add(min);
                taken.add(min);
            }
        }
    }

    private T removeMin(List<T> nodes) {
        if (this.comparator == null) {
            return nodes.removeFirst();
        }

        int minIndex = 0;
        for (int i = 1; i < nodes.size(); ++i) {
            if (this.comparator.compare(nodes.get(i), nodes.get(minIndex)) < 0) {
                minIndex = i;
            }
        }
        return nodes.remove(minIndex);
    }

    private void collectChildren(T node, List<T> out) {
        for (var child : this.graph.predecessors(node)) {
            if (!this.taken.contains(child)) {
                out.add(child);
                collectChildren(child, out);
            }
        }
    }
}
