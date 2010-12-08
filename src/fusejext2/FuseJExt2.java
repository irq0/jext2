package fusejext2;

import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.LinkedList;

import fuse.Fuse;
import fuse.SWIGTYPE_p_fuse_chan;
import fuse.SWIGTYPE_p_fuse_session;
import fuse.Session;

import jlowfuse.JLowFuse;
import jlowfuse.JLowFuseArgs;

import org.apache.commons.cli.*;

public class FuseJExt2 {
	private static RandomAccessFile blockDevFile;
	private static FileChannel blockDev;

	private static SWIGTYPE_p_fuse_chan chan = null;
	private static SWIGTYPE_p_fuse_session sess = null;

	private static String mountpoint;
	private static String filename;

	private static boolean daemon = true;
	private static String fuseCommandline = "-o foo,subtype=jext2";
	
	static class FuseShutdownHook extends Thread {
		public void run() {
			Session.removeChan(chan);
			Session.exit(sess);
			Fuse.unmount(mountpoint, chan);
		}
	}	
	
	public static void parseCommandline(String[] args) {
		LinkedList<String> fuseCommandList = new LinkedList<String>();
		CommandLineParser parser = new PosixParser();
		
		Options options = new Options();
		options.addOption("f", "foreground", false, "do not daemonize");
		options.addOption("h", "help", false, "print this usage text");
		options.addOption(OptionBuilder.withDescription("options passed directly to FUSE")
		                  .hasArg()
		                  .withArgName("FUSE_OPTIONS")
		                  .create("o"));
		try {
			CommandLine cmd = parser.parse(options, args);

			if (cmd.hasOption("f")) {
				daemon = false;
			}
			if (cmd.hasOption("h")) {
				throw new ParseException("");
			}

			String[] leftover = cmd.getArgs();
			if (leftover.length != 2) {
				throw new ParseException("No <block device> and/or <mountpoint> given!");
			} else {
				filename = leftover[0];
				mountpoint = leftover[1];
			}

			if (cmd.hasOption("o")) {
				fuseCommandline += "," + cmd.getOptionValue("o");
			}
			
		} catch (ParseException e) {
			HelpFormatter usage = new HelpFormatter();
			usage.printHelp("<jext2 java commandline> [OPTIONS] <block device> <mountpoint>",
			                "jext2 - java ext2 file system implementation",
			                options,
			                e.getMessage());
			System.exit(1);
		}
	}

	public static void main(String[] args) {
		parseCommandline(args);
		
		JLowFuseArgs fuseArgs = JLowFuseArgs.parseCommandline(new String[] {fuseCommandline});
		
        try {
    		blockDevFile = new RandomAccessFile(filename, "rw");
    		blockDev = blockDevFile.getChannel();		
        } catch (FileNotFoundException e) {
        	System.out.println("Can't open block device / file");
        	System.exit(1);
        }

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
