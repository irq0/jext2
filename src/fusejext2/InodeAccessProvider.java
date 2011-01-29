package fusejext2;

import java.io.IOException;
import java.util.Hashtable;

import jext2.Inode;
import jext2.InodeAccess;

/**
 * Kind of inode table, that enshures that there are no inodes in memory
 * with the same number. 
 */
public class InodeAccessProvider {
    private Hashtable<Long,Inode> openInodes = new Hashtable<Long,Inode>();
    
    class InodeNotOpenException extends Exception {
        public InodeNotOpenException(String msg) { 
            super(msg);
        }               
    }

    Inode getOpen(long ino) throws InodeNotOpenException {
        if (!openInodes.containsKey(ino))
            throw new InodeNotOpenException("Inode must be opened before use " + ino);
        else
            return openInodes.get(ino);
    }
    
    Inode get(long ino) throws IOException { 
        if (openInodes.containsKey(ino)) {
            return openInodes.get(ino);
        } else {
            Inode inode = InodeAccess.readByIno(ino);
            openInodes.put(ino, inode);
            return inode;
        }
    }
        
    void close(long ino) {
        openInodes.remove(ino);
    }
    
}
