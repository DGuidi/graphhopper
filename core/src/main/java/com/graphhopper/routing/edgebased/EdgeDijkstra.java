/*
 *  Licensed to Peter Karich under one or more contributor license 
 *  agreements. See the NOTICE file distributed with this work for 
 *  additional information regarding copyright ownership.
 * 
 *  Peter Karich licenses this file to you under the Apache License, 
 *  Version 2.0 (the "License"); you may not use this file except 
 *  in compliance with the License. You may obtain a copy of the 
 *  License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.graphhopper.routing.edgebased;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.PriorityQueue;

import com.graphhopper.routing.Path;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.WeightCalculation;
import com.graphhopper.storage.EdgeEntry;
import com.graphhopper.storage.Graph;
import com.graphhopper.util.EdgeExplorer;
import com.graphhopper.util.EdgeIterator;

/**
 * An edge-based version of Dijkstras Algorithms. End link costs will be stored for each edge
 * instead of for each node. This is necessary when considering turn costs, but will be around three
 * times slower than classic Dijkstra.
 * 
 * @see http://www.easts.info/on-line/journal_06/1426.pdf
 * 
 *      TODO we better should reuse the code of Dijkstra instead instead of copying it. should be
 *      done later
 * 
 * @author Karl Hübner
 */
public class EdgeDijkstra extends AbstractEdgeBasedRoutingAlgorithm
{

    protected TIntObjectMap<EdgeEntry> map = new TIntObjectHashMap<EdgeEntry>();
    protected PriorityQueue<EdgeEntry> heap = new PriorityQueue<EdgeEntry>();
    protected boolean alreadyRun;
    protected int visitedNodes;

    public EdgeDijkstra( Graph g, FlagEncoder encoder, WeightCalculation type )
    {
        super(g, encoder, type);
    }
    
    @Override
    public Path calcPath( int from, int to )
    {
        if ( alreadyRun )
            throw new IllegalStateException("Create a new instance per call");
        alreadyRun = true;
        EdgeEntry fromEdge = new EdgeEntry(EdgeIterator.NO_EDGE, from, 0d);
        EdgeEntry currEdge = calcEdgeEntry(fromEdge, to);
        if ( currEdge == null || currEdge.endNode != to )
            return new Path(graph, flagEncoder);

        return extractPath(currEdge);
    }

    public EdgeEntry calcEdgeEntry( EdgeEntry currEdge, int to )
    {
        EdgeExplorer explorer = outEdgeExplorer;
        while ( true )
        {
            visitedNodes++;
            if ( finished(currEdge, to) )
                break;

            int neighborNode = currEdge.endNode;
            explorer.setBaseNode(neighborNode);
            while ( explorer.next() )
            {
                if ( !accept(explorer, currEdge) )
                    continue;

                //we need to distinguish between backward and forward direction when storing end weights
                int key = createIterKey(explorer, false);

                int tmpNode = explorer.getAdjNode();
                double tmpWeight = weightCalc.getWeight(explorer.getDistance(), explorer.getFlags())
                        + currEdge.weight
                        + turnCostCalc.getTurnCosts(neighborNode, currEdge.edge, explorer.getEdge());
                EdgeEntry nEdge = map.get(key);
                if ( nEdge == null )
                {
                    nEdge = new EdgeEntry(explorer.getEdge(), tmpNode, tmpWeight);
                    nEdge.parent = currEdge;
                    map.put(key, nEdge);
                    heap.add(nEdge);
                } else if ( nEdge.weight > tmpWeight )
                {
                    heap.remove(nEdge);
                    nEdge.edge = explorer.getEdge();
                    nEdge.weight = tmpWeight;
                    nEdge.parent = currEdge;
                    heap.add(nEdge);
                }

                updateShortest(nEdge, neighborNode);
            }

            if ( heap.isEmpty() )
                return null;
            currEdge = heap.poll();
            if ( currEdge == null )
                throw new AssertionError("cannot happen?");
        }
        return currEdge;
    }

    protected boolean finished( EdgeEntry currEdge, int to )
    {
        return currEdge.endNode == to;
    }

    public Path extractPath( EdgeEntry goalEdge )
    {
        return new Path(graph, flagEncoder).setEdgeEntry(goalEdge).extract();
    }

    @Override
    public String getName()
    {
        return "edge-based dijkstra";
    }

    @Override
    public int getVisitedNodes()
    {
        return visitedNodes;
    }
}
