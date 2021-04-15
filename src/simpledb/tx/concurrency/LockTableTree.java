package simpledb.tx.concurrency;

import simpledb.tx.Transaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LockTableTree extends LockTable {
    public static int MAX_CHILDREN = 3;
    public static int MIN_CHILDREN = 2;

    class TreeNode {
        Object parent;
        Object dataItem;
        List<TreeNode> children;

        public TreeNode(Object parent, Object dataItem) {
            this.parent = parent;
            this.dataItem = dataItem;
            this.children = new ArrayList<>();
        }
    }
    public TreeNode root;

    @Override
    void initialize(Object... dataItems) {
        List<Object> remaining = new ArrayList<>();
        for (Object dataItem : dataItems) {
            remaining.add(dataItem);
        }
        Collections.shuffle(remaining);
        root = new TreeNode(null, remaining.remove(0));
        assignChildren(root, remaining);
    }

    void assignChildren(TreeNode node, List<Object> remaining) {
        if (remaining.size() == 0)
            return;
        int nChildren = MIN_CHILDREN + (int) (Math.random() * (MAX_CHILDREN - MIN_CHILDREN + 1));
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < nChildren && remaining.size() > 0; i++) {
            order.add(i);
            node.children.add(new TreeNode(node, remaining.remove(0)));
        }
        Collections.shuffle(order);
        for (int index : order) {
            assignChildren(node.children.get(index), remaining);
        }
    }

    @Override
    void handleIncompatible(Transaction waiting, Transaction holding, LockEntry entry) throws InterruptedException {

    }

    @Override
    void handleUnlock(Transaction transaction) {

    }
}
