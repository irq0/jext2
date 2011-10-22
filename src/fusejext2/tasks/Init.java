package fusejext2.tasks;

import java.util.Date;

import fusejext2.InodeAccessProvider;
import fusejext2.Jext2Context;

import jext2.BlockAccess;
import jext2.BlockGroupAccess;
import jext2.Constants;
import jext2.Feature;
import jext2.Superblock;
import jext2.exceptions.IoError;

public class Init extends jlowfuse.async.tasks.Init<Jext2Context> {

	
	private void performBasicFilesystemChecks()
	{
    	checkExt2Features();
        checkExt2Magic();
        checkExt2Revision();
        checkExt2MountState();	
	}
	
	private void checkExt2Features() {
        if (Feature.incompatUnsupported() || Feature.roCompatUnsupported()) {
            System.out.println("Featureset incompatible with JExt2 :(");
            System.exit(23);
        }
	}
	
	private void checkExt2Magic() {
        if (context.superblock.getMagic() != 0xEF53) {
            System.out.println("Wrong magic -> no ext2");
            System.exit(23);
        }
	}
	
	private void checkExt2Revision() {
        /* ext2_setup_super */
        if (context.superblock.getRevLevel() > Constants.JEXT2_MAX_SUPP_REV) {
            System.out.println("Error: Revision level too high, exiting");
            System.exit(23);
        }
	}
	
	private void checkExt2MountState() { 
		if ((context.superblock.getState() & Constants.EXT2_VALID_FS) == 0)
			System.out.println("Mounting uncheckt fs");	        
		else if ((context.superblock.getState() & Constants.EXT2_ERROR_FS) > 0) 
			System.out.println("Mounting fs with errors");
		else if ((context.superblock.getMaxMountCount() >= 0) &&
				(context.superblock.getMountCount() >= context.superblock.getMaxMountCount()))
			System.out.println("Maximal mount count reached");

		if (context.superblock.getMaxMountCount() == 0)
			context.superblock.setMaxMountCount(Constants.EXT2_DFL_MAX_MNT_COUNT);
	}
	
	private void markExt2AsMounted() {
        context.superblock.setMountCount(context.superblock.getMountCount() + 1);
        context.superblock.setLastMount(new Date());
        context.superblock.setLastMounted("jext2");
	}
	
    public void run() {
	    super.run();
	    
	    context.inodes = new InodeAccessProvider();
		
	    try {
	    	context.blocks = new BlockAccess(context.blockDev);
	    	context.superblock = Superblock.fromBlockAccess(context.blocks);
	    	context.blocks.initialize(context.superblock);

	    	performBasicFilesystemChecks();        	        
	        markExt2AsMounted();        
	        // TODO set times, volume name, etc in  superblock
	        
	        context.blockGroups = new BlockGroupAccess();
	        context.blockGroups.readDescriptors();
		} catch (IoError e) {
			System.out.println("init() failed :(");
			System.out.println(23);
		}
    }
}