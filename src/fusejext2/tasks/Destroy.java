package fusejext2.tasks;

import java.util.Date;

import jext2.exceptions.IoError;
import fusejext2.Jext2Context;

public class Destroy extends jlowfuse.async.tasks.Destroy<Jext2Context> {
	@Override
	public void run() {
        try {
            context.superblock.setLastWrite(new Date());
            context.superblock.sync();
            context.blockGroups.syncDescriptors();

            context.blocks.sync();
        } catch (IoError e) {
            System.out.println("IoError on final superblock/block group descr sync");
        }
	}
}
