package fusejext2;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.io.File;

import jext2.Filesystem;

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

	private static boolean daemon = false;
	private static String fuseCommandline = "-o foo,subtype=jext2";
	
	static class FuseShutdownHook extends Thread {
		public void run() {
			Session.removeChan(chan);
			Session.exit(sess);
			Fuse.unmount(mountpoint, chan);
		}
	}	
	
	@SuppressWarnings("static-access")
    public static void parseCommandline(String[] args) {
		CommandLineParser parser = new PosixParser();
		
		Options options = new Options();
		options.addOption("d", "daemonize", false, "java side of daemonzation - use jext_daemon.sh");
		options.addOption("h", "help", false, "print this usage text");
		options.addOption(OptionBuilder.withDescription("options passed directly to FUSE")
		                  .hasArg()
		                  .withArgName("FUSE_OPTIONS")
		                  .create("o"));
		options.addOption(OptionBuilder.withDescription("charset used for file system string conversion")
		                  .hasArg()
		                  .withArgName("CHARSET")
		                  .create("c"));
		options.addOption(OptionBuilder.withDescription("log to file")
		                  .hasArg()
		                  .withArgName("FILENAME")
		                  .create("l"));
		try {
			CommandLine cmd = parser.parse(options, args);

			if (cmd.hasOption("f")) {
				daemon = false;
			}
			if (cmd.hasOption("h")) {
				throw new ParseException("");
			}
			if (cmd.hasOption("c")) {
			    String option = cmd.getOptionValue("c");
			    try {
			        Filesystem.setCharset(Charset.forName(option));
			    } catch (UnsupportedCharsetException e) {
			        throw new ParseException("Unknown charset: " + option +
			                "\n Supported charsets:\n\n" + Charset.availableCharsets());
			    }
			}
			if (cmd.hasOption("l")) {
			    String filename = cmd.getOptionValue("l");
			    try {
			        Filesystem.initializeLoggingToFile(filename);
			    } catch (IOException e) {
			        throw new ParseException("Can't open file for logging");
			    }
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


	private static File getPidFile() {
		String filename = System.getProperty("daemon.pidfile");
		File pidfile = new File(filename);

		return pidfile;
	}
	
	private static void daemonize() {
		getPidFile().deleteOnExit();
		System.out.close();
		System.err.close();
	}

	public static void main(String[] args) {
		parseCommandline(args);
		
		JLowFuseArgs fuseArgs = JLowFuseArgs.parseCommandline(new String[] {fuseCommandline});

		if (daemon)
			daemonize();
			
		
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
