package jext2.exceptions;

import java.util.logging.Logger;

import jext2.Filesystem;

public class JExt2Exception extends Exception {
    
    int errno = 0;
    
    private static final long serialVersionUID = -7429088074385678308L;

    public JExt2Exception(int errno) {
        this.errno = errno;
        log("");
    }
    
    protected JExt2Exception(String msg) {
        log(msg);
    }

    private void log(String msg) {
        Logger logger = Filesystem.getLogger();
        if (logger == null) return;
        
        StackTraceElement[] stack = getStackTrace();
        
        StringBuffer log = new StringBuffer(80); 
        
        log.append(this.getClass().getSimpleName());
        log.append(" IN ");
        log.append(stack[0].getClassName());
        log.append("->");
        log.append(stack[0].getMethodName());
        log.append(" ");
        log.append(msg);
        
        logger.fine(log.toString());
    }
    
    
    public int getErrno() {
        return errno;
    }
}
