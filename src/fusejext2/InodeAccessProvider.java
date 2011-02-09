package fusejext2;

import java.util.concurrent.ConcurrentSkipListMap;

import jext2.Inode;
import jext2.InodeAccess;
import jext2.exceptions.InvalidArgument;
import jext2.exceptions.IoError;
import jext2.exceptions.NoSuchFileOrDirectory;

/**
 * Kind of inode table, that enshures that there are no inodes in memory
 * with the same number. 
 */
public class InodeAccessProvider {
    private ConcurrentSkipListMap<Long,Inode> openInodes = new ConcurrentSkipListMap<Long,Inode>();
    
    Inode getOpen(long ino)  {
        if (!openInodes.containsKey(ino))
            return null;
        else
            return openInodes.get(ino);
    }
    
    Inode get(long ino) throws IoError, NoSuchFileOrDirectory, InvalidArgument { 
        if (openInodes.containsKey(ino)) {
            Inode inode = openInodes.get(ino);
            if (inode.isDeleted()) {
                openInodes.remove(ino);
                inode = InodeAccess.readByIno(ino);
                openInodes.put(ino, inode);
            }                
            
            return openInodes.get(new Long(ino));
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
