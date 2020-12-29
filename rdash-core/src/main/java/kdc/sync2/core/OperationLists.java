/*
 * I, 20kdc, release this code into the public domain.
 * I make no guarantees or provide any warranty,
 *  implied or otherwise, with this code.
 */

package kdc.sync2.core;

import java.util.HashMap;
import java.util.LinkedList;

public class OperationLists {
    public LinkedList<Operation> correct = new LinkedList<Operation>();
    public LinkedList<Operation> download = new LinkedList<Operation>();
    public LinkedList<Operation> upload = new LinkedList<Operation>();
    public LinkedList<Operation> cleanup = new LinkedList<Operation>();

    public String[] stages = new String[] {"correct", "download", "upload", "cleanup"};

    public LinkedList<Operation> getStage(String nt) {
        try {
            return (LinkedList<Operation>) OperationLists.class.getField(nt).get(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
