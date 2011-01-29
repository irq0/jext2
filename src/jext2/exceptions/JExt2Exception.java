package jext2.exceptions;

public class JExt2Exception extends Exception {
    int errno = 0;
    
    private static final long serialVersionUID = -7429088074385678308L;

    public JExt2Exception(int errno) {
        this.errno = errno;
    }
    
    public int getErrno() {
        return errno;
    }
}
