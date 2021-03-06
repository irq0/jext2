/*
 * Copyright (c) 2011 Marcel Lauhoff.
 * 
 * This file is part of jext2.
 * 
 * jext2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * jext2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with jext2.  If not, see <http://www.gnu.org/licenses/>.
 */

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
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Logger;

import jext2.Filesystem;
import jlowfuse.JLowFuse;
import jlowfuse.JLowFuseArgs;
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
	private static boolean logExecutorStatus = false;
	private static int logExecutorStatusIntervallInMillis = 1000;
	private static int queueLength = 50;
	private static String fuseCommandline = "-o foo,subtype=jext2";
	private static JLowFuseArgs fuseArgs;

	private static TaskImplementations<Jext2Context> impls;
	private static Jext2Context context;

	private static JextThreadPoolExecutor service;
	public static int numberOfThreads = -1; /* XXX datastructureaccessprovider accesses this, mach es besser! */

	private static CommandLineParser parser;
	private static Options options;

	static class FuseShutdownHook extends Thread {
		private Logger logger;

		private void runDestroyTask() {
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
		    	logger.info("Waiting for "+ (service.getActiveCount()+service.getQueue().size()) + " tasks to finish");
				System.out.println("Awaiting Termination... Queued: " + service.getQueue()
														  +" Running: " + service.getActiveCount());
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
			// TODO add code to sync all the stuff in the data structure access provider
			shutdownThreadPool();
			runDestroyTask();
			shutdownFuse();
			shutdownBlockDev();
			flushLog();
		}
	}

	@SuppressWarnings("static-access")
	private static void initializeCommandLineParser() {
		parser = new PosixParser();

		options = new Options();
		options.addOption(OptionBuilder
				.withDescription("Activate daemon mode in jext2. Don't use directly, " +
						"use jext2_daemon.sh instead")
				.withLongOpt("daemon")
				.create("D"));
		options.addOption(OptionBuilder
				.withDescription("Print usage")
				.withLongOpt("help")
				.create("h"));
		options.addOption(OptionBuilder
				.withDescription("Options forwarded to FUSE")
				.withLongOpt("fuse-options")
				.withType(new String(""))
				.hasArg()
				.withArgName("FUSE_OPTIONS")
				.create("o"));
		options.addOption(OptionBuilder
				.withDescription("Charset for file system string conversion")
				.withLongOpt("charset")
				.withType(new String(""))
				.hasArg()
				.withArgName("CHARSET")
				.create("c"));
		options.addOption(OptionBuilder
				.withDescription("Log to file, ")
				.withLongOpt("log")
				.withType(new String(""))
				.hasArg()
				.withArgName("FILENAME")
				.create("l"));
		options.addOption(OptionBuilder
				.withDescription("Number of threads to execute jext2 tasks. Default: #CPU + 1")
				.withLongOpt("threads")
				.withType(new Integer(0))
				.hasArg()
				.withArgName("NTHREADS")
				.create("n"));
		options.addOption(OptionBuilder
				.withDescription("Length of the queue the executer uses to schedule tasks from. Default: " + queueLength)
				.withType(new Integer(0))
				.withLongOpt("queue-length")
				.hasArg()
				.withArgName("QUEUE_LENGTH")
				.create("Q"));
		options.addOption(OptionBuilder
				.withDescription("Periodically log executor status (Loglevel INFO)")
				.withLongOpt("log-executor")
				.withType(new Integer(0))
				.hasArg()
				.withArgName("TIME_IN_MILLIS")
				.create("E"));
		options.addOption(OptionBuilder
				.withDescription("Debug output, possible values:\n" +
						"SEVERE (highest value)\n" +
						"WARNING\n" +
						"INFO\n" +
						"CONFIG\n" +
						"FINE\n" +
						"FINER\n" +
						"FINEST (lowest value)\n" +
						"Default: FINE" )
				.hasOptionalArg()
				.withArgName("LEVEL")
				.withLongOpt("debug")
				.create("d"));
		options.addOption(OptionBuilder
				.withDescription("Verbose output (same as --debug INFO)")
				.withLongOpt("verbose")
				.create("v"));
	}

	public static void parseCommandline(String[] args) {
		try {
			CommandLine cmd = parser.parse(options, args);

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

			if (cmd.hasOption("Q")) {
				queueLength = Integer.parseInt(cmd.getOptionValue("Q"));
			}

			if (cmd.hasOption("v")) {
				Filesystem.setLogLevel("INFO");
			}

			if (cmd.hasOption("d")) {
				Filesystem.setLogLevel(cmd.getOptionValue("d", "FINE"));
			}

			if (cmd.hasOption("D")) {
				daemon = true;
			}

			if (cmd.hasOption("E")) {
				logExecutorStatus = true;
				logExecutorStatusIntervallInMillis = Integer.parseInt(cmd.getOptionValue("E","5000"));
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

			if (cmd.hasOption("n")) {
				try {
					int n = Integer.parseInt(cmd.getOptionValue("n"));

					if (n < 1)
						throw new ParseException("Number of threads must be positive");
					else
						numberOfThreads = n;

				} catch (NumberFormatException e) {
					throw new ParseException("Number of threads must be numeric");
				}
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

	private static void setupTaskImplementations() {
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
	}

	private static void setupBlockDevice() {
		try {
			RandomAccessFile blockDevFile = new RandomAccessFile(filename, "rw");
			blockDev = blockDevFile.getChannel();
		} catch (FileNotFoundException e) {
			System.out.println("Can't open block device or file " + filename);
			System.out.println(e.getMessage());
			System.exit(1);
		}
	}

	private static void mount() {
		chan = Fuse.mount(mountpoint, fuseArgs);
		if (chan == null) {
			System.out.println("Can't mount on " + mountpoint);
			System.exit(1);
		}
	}

	private static void setupShutdownHook() {
		FuseShutdownHook hook = new FuseShutdownHook();
		Runtime.getRuntime().addShutdownHook(hook);
	}

	private static void setupFuseSession() {
		Filesystem.getLogger().info("Arguments passed to FUSE: " + fuseCommandline);

		sess = JLowFuse.asyncTasksNew(fuseArgs, impls,
				service, context);

		Session.addChan(sess, chan);
	}

	private static void setupExecutor() {
		if (numberOfThreads < 0)
			numberOfThreads = Runtime.getRuntime().availableProcessors() + 1;
		service = new JextThreadPoolExecutor(numberOfThreads, queueLength);

		if (logExecutorStatus)
			service.activateStatusDump(logExecutorStatusIntervallInMillis);
	}

	private static void setupTaskContext() {
		context = new Jext2Context(blockDev);
	}

	public static void main(String[] args) {
		Filesystem.initializeLogging();

		initializeCommandLineParser();
		parseCommandline(args);
		fuseArgs = JLowFuseArgs.parseCommandline(new String[] {fuseCommandline});

		setupBlockDevice();
		mount();

		if (daemon) {
			daemonize();
		} else {
			Filesystem.initializeLoggingToConsole();
		}

		setupShutdownHook();

		setupTaskContext();
		setupTaskImplementations();
		setupExecutor();
		
		setupFuseSession();
		Session.loopSingle(sess);
	}
}
