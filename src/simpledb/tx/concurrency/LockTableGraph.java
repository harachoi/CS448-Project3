//package simpledb.tx.concurrency;
//
//import simpledb.tx.Transaction;
//
//import java.util.*;
//
//public class LockTableGraph extends LockTable {
//
//    class WaitForGraph {
//        class Vertex {
//            LockEntry data;
//            boolean visited = false;
//            boolean inCycle = false;
//            List<Vertex> neighbors = new ArrayList<>();
//
//            Vertex(LockEntry data) {
//                this.data = data;
//            }
//
//            void reset() {
//                visited = false;
//                inCycle = false;
//            }
//        }
//
//        HashMap<LockEntry, Vertex> adjacencyList = new HashMap<>();
//
//        void waitFor(LockEntry waiting, List<LockEntry> holding) {
//            if (!adjacencyList.containsKey(waiting))
//                adjacencyList.put(waiting, new Vertex(waiting));
//            for (LockEntry holdingEntry : holding) {
//                if (!adjacencyList.containsKey(holdingEntry))
//                    adjacencyList.put(holdingEntry, new Vertex(holdingEntry));
//                adjacencyList.get(waiting).neighbors.add(adjacencyList.get(holdingEntry));
//            }
//        }
//
//        boolean inCycle(Vertex source) {
//            source.inCycle = true;
//            for (Vertex neighbor : source.neighbors) {
//                if (neighbor.inCycle)
//                    return true;
//                else if (!neighbor.visited && inCycle(neighbor))
//                    return true;
//            }
//
//            source.inCycle = false;
//            source.visited = true;
//            return false;
//        }
//
//        boolean hasCycle() {
//            for (Vertex vertex : adjacencyList.values())
//                vertex.reset();
//            for (Vertex vertex : adjacencyList.values())
//                if (!vertex.visited && inCycle(vertex))
//                    return true;
//            return false;
//        }
//
//        void remove(LockEntry data) {
//            Vertex vertex = adjacencyList.
//        }
//    }
//
//    @Override
//    void initialize(Object... dataItems) {
//
//    }
//
//    @Override
//    void handleIncompatible(Transaction waiting, List<LockEntry> holding, LockEntry entry) throws InterruptedException {
//
//    }
//
//    @Override
//    void handleUnlock(Transaction transaction) {
//
//    }
//}
