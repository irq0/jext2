package fusejext2;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Constructor;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Logger;

import jext2.Filesystem;
import jlowfuse.JLowFuse;
import jlowfuse.JLowFuseArgs;
import jlowfuse.async.AsyncLowlevelOps;
import jlowfuse.async.DefaultTaskImplementations;
import jlowfuse.async.TaskImplementations;
import jlowfuse.async.tasks.JLowFuseTask;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import fuse.Fuse;
import fuse.SWIGTYPE_p_fuse_chan;
import fuse.SWIGTYPE_p_fuse_session;
import fuse.Session;

public class FuseJExt2 {
	private static FileChannel blockDev;

	private static SWIGTYPE_p_fuse_chan chan = null;
	private static SWIGTYPE_p_fuse_session sess = null;

	private static String mountpoint;
	private static String filename;

	private static boolean daemon = false;
	private static String fuseCommandline = "-o foo,subtype=jext2";

	private static TaskImplementations<Jext2Context> impls;
	private static Jext2Context context;

	private static JextThreadPoolExecutor service;
	

	static class FuseShutdownHook extends Thread {
		private Logger logger;

		private void submitDestroyTask() {
				Class<? extends JLowFuseTask<Jext2Context>> impl = impls.destroyImpl;
		    	Constructor<? extends JLowFuseTask<Jext2Context>> c = TaskImplementations.getTaskConstructor(impl);
		    	JLowFuseTask<Jext2Context> task = TaskImplementations.instantiateTask(c);
		    	task.initContext(context);
		    	logger.info("Running DESTROY Task");
		    	task.run();
		}
		
		private void shutdownBlockDev() {
	    	logger.info("Shutting down access to block device");

			try {
				blockDev.force(false);
				blockDev.close();
			} catch (IOException e) {
				System.err.println(Arrays.toString(e.getStackTrace()));
				System.err.println(e.getLocalizedMessage());
			}
		}
		
		private void shutdownThreadPool() {
	    	logger.info("Shutting down thread pool");

			service.shutdown();
			
			try {
		    	logger.info("Waiting for "+ service.getTaskCount() + " tasks to finish");
				System.out.println("Awaiting Termination of: " + service.getQueue());
				service.awaitTermination(120, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				System.err.println("Thread pool shutdown interrupted!");
				System.err.println(Arrays.toString(e.getStackTrace()));
			}
		}

		private void shutdownFuse() {
	    	logger.info("Fuse shutdown, Unmounting..");
			Session.removeChan(chan);
			Session.exit(sess);
			Fuse.unmount(mountpoint, chan);
		}
		
		private void flushLog() {
			for (Handler h : Filesystem.getLogger().getHandlers()) {
				h.flush();
				h.close();
			}
		}
			
		@Override 
		public void run() {
			logger = Filesystem.getLogger();
			System.out.println("Shutdown.. ");

			/* this should not be nesseccrry but fuse/jlowfuse does not call
			 * DESTROY
			 */
			// TODO integrate this into jlowfuse
			
			shutdownThreadPool();
			submitDestroyTask();
			shutdownFuse();
			shutdownBlockDev();
			flushLog();
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
		return new File(filename);
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
			RandomAccessFile blockDevFile = new RandomAccessFile(filename, "rw");
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

		FuseShutdownHook hook = new FuseShutdownHook();
		Runtime.getRuntime().addShutdownHook(hook);

		context = new Jext2Context(blockDev);
		impls =	new DefaultTaskImplementations<Jext2Context>();

		// for i in *.java; do n=${i%.*}; c=${n,*}; echo impls.${c}Impl = TaskImplementations.getImpl\(\"fusejext2.tasks.$n\"\)\;>
		impls.accessImpl = TaskImplementations.getImpl("fusejext2.tasks.Access");
		impls.destroyImpl = TaskImplementations.getImpl("fusejext2.tasks.Destroy");
		impls.forgetImpl = TaskImplementations.getImpl("fusejext2.tasks.Forget");
		impls.fsyncImpl = TaskImplementations.getImpl("fusejext2.tasks.Fsync");
		impls.fsyncdirImpl = TaskImplementations.getImpl("fusejext2.tasks.Fsyncdir");
		impls.getattrImpl = TaskImplementations.getImpl("fusejext2.tasks.Getattr");
		impls.initImpl = TaskImplementations.getImpl("fusejext2.tasks.Init");
		impls.linkImpl = TaskImplementations.getImpl("fusejext2.tasks.Link");
		impls.lookupImpl = TaskImplementations.getImpl("fusejext2.tasks.Lookup");
		impls.mkdirImpl = TaskImplementations.getImpl("fusejext2.tasks.Mkdir");
		impls.mknodImpl = TaskImplementations.getImpl("fusejext2.tasks.Mknod");
		impls.openImpl = TaskImplementations.getImpl("fusejext2.tasks.Open");
		impls.opendirImpl = TaskImplementations.getImpl("fusejext2.tasks.Opendir");
		impls.readImpl = TaskImplementations.getImpl("fusejext2.tasks.Read");
		impls.readdirImpl = TaskImplementations.getImpl("fusejext2.tasks.Readdir");
		impls.readlinkImpl = TaskImplementations.getImpl("fusejext2.tasks.Readlink");
		impls.releaseImpl = TaskImplementations.getImpl("fusejext2.tasks.Release");
		impls.releasedirImpl = TaskImplementations.getImpl("fusejext2.tasks.Releasedir");
		impls.renameImpl = TaskImplementations.getImpl("fusejext2.tasks.Rename");
		impls.rmdirImpl = TaskImplementations.getImpl("fusejext2.tasks.Rmdir");
		impls.setattrImpl = TaskImplementations.getImpl("fusejext2.tasks.Setattr");
		impls.statfsImpl = TaskImplementations.getImpl("fusejext2.tasks.Statfs");
		impls.symlinkImpl = TaskImplementations.getImpl("fusejext2.tasks.Symlink");
		impls.unlinkImpl = TaskImplementations.getImpl("fusejext2.tasks.Unlink");
		impls.writeImpl = TaskImplementations.getImpl("fusejext2.tasks.Write");

		service = new JextThreadPoolExecutor(10);

		sess = JLowFuse.asyncTasksNew(fuseArgs, impls,
				service, context);

		Session.addChan(sess, chan);
		Session.loopSingle(sess);
	}
}
