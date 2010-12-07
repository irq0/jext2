package fusejext2;

import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

import fuse.Fuse;
import fuse.SWIGTYPE_p_fuse_chan;
import fuse.SWIGTYPE_p_fuse_session;
import fuse.Session;

import jlowfuse.JLowFuse;
import jlowfuse.JLowFuseArgs;

public class FuseJExt2 {
	private static RandomAccessFile blockDevFile;
	private static FileChannel blockDev;

	private static SWIGTYPE_p_fuse_chan chan = null;
	private static SWIGTYPE_p_fuse_session sess = null;

	private static String mountpoint;
	private static String filename;
	
	static class FuseShutdownHook extends Thread {
		public void run() {
			Session.removeChan(chan);
			
			Session.destroy(sess);
			Session.exit(sess);
			
			Fuse.unmount(mountpoint, chan);
		}
	}	
	
	private static void usage() {
		System.err.println("USAGE: [cmd] <device> <mountpoint>");
	}
	
	public static void main(String[] args) {
		if (args.length != 2) {
    		usage();
    		System.exit(1);
    	} 
    	
        JLowFuseArgs fuseArgs = JLowFuseArgs.parseCommandline(
        			new String[] {"-osubtype=bla", "-d"});
		filename = args[0];
		mountpoint = args[1];
                
        try {
    		blockDevFile = new RandomAccessFile(filename, "rw");
    		blockDev = blockDevFile.getChannel();		
        } catch (FileNotFoundException e) {
        	System.out.println("Can't open block device / file");
        	System.exit(1);
        }

        try {
        	chan = Fuse.mount(mountpoint, fuseArgs);
		if (chan == null) {
			System.out.println("Can't mount on " + mountpoint);
			System.exit(1);
		}

		sess = JLowFuse.lowlevelNew(fuseArgs, new JExt2Ops(blockDev));
        
        Session.addChan(sess, chan);
        
        FuseShutdownHook hook = new FuseShutdownHook();
        Runtime.getRuntime().addShutdownHook(hook);
        
        Session.loopSingle(sess);
        // Session.loopMulti(sess);
    }
}
