/*
 * I, 20kdc, release this code into the public domain.
 * I make no guarantees or provide any warranty,
 *  implied or otherwise, with this code.
 */

package kdc.sync2.se;

import kdc.sync2.core.Operation;
import kdc.sync2.core.OperationFeedback;
import kdc.sync2.core.OperationLists;
import kdc.sync2.core.ServerLayout;
import kdc.sync2.core.Synchronizer;
import kdc.sync2.se.hmr.HMRFrame;
import kdc.sync2.se.mdk.RequestHostnameState;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Interface to the program.
 * Here's the sub-packages, named arbitrarily:
 *  hmr: Swing wrapper, protects mdk from horrific code
 *  mdk: GUI-mode states, uses hmr
 */
public class Main {
    /*
     * I'd write notes for potential options here, but none are needed in theory.
     * Maybe abstract ServerLayout so that cases like "index on server, storage separate" are possible,
     *  but really that can be handled by only mounting the indexes folder.
     * Not sure what to do, as failing anything else, most of the system can be managed by scripts and symlinks (or copies),
     *  apart from the core "is this older than this" and "should I upload/download" stuff, which is here.
     */
    public static void main(String[] args) throws IOException {
        int syncMode = 0;
        boolean noHost = false;
        for (String a : args) {
            // Accept some common "help" syntax.
            if (a.equalsIgnoreCase("help") || a.equalsIgnoreCase("--help") || a.equalsIgnoreCase("-h") || a.equalsIgnoreCase("-?") || a.equalsIgnoreCase("/?")) {
                printHelp();
                System.exit(1);
                return;
            } else if (a.equalsIgnoreCase("example")) {
                printExample();
                return;
            } else if (a.equalsIgnoreCase("noHost")) {
                noHost = true;
            } else if (a.equalsIgnoreCase("standard")) {
                syncMode = 1;
            } else if (a.equalsIgnoreCase("gui")) {
                syncMode = 2;
            } else {
                System.err.println("Unknown option '" + a + "'.");
                printHelp();
                System.exit(1);
                return;
            }
        }
        if (syncMode == 0) {
            System.err.println("No sync mode passed.");
            printHelp();
            System.exit(1);
            return;
        }
        if (syncMode == 1) {
            // Only line read from stdin: hostname. (This is written in such a way that it could be an input or a status)
            System.err.println("Hostname...");
            String host = new BufferedReader(new InputStreamReader(System.in)).readLine();
            Synchronizer s = new Synchronizer(new ServerLayout(host));
            OperationLists llo = new OperationLists();
            OperationFeedback of = new OperationFeedback() {
                @Override
                public void showFeedback(String text, double operationProgress) {
                    System.out.println("# " + text);
                    System.out.println(operationProgress * 100);
                }
            };
            s.prepareSync(noHost, llo).execute(of);
            for (String st : llo.stages)
                new Operation.GroupOperation(llo.getStage(st).toArray(new Operation[0])).execute(of);
        } else if (syncMode == 2) {
            HMRFrame frame = new HMRFrame();
            frame.reset(new RequestHostnameState(frame, noHost));
        } else {
            System.err.println("Unrecognized synchronization mode, internal error.");
        }
    }

    private static void printHelp() {
        System.err.println("Usage:");
        System.err.println(" java -jar Sync2.jar <options...>");
        System.err.println("NOTE! This program is only to be run when all computers with control of the indexes are trustworthy.");
        System.err.println("      Indexes are not validated for potentially bad sequences.");
        System.err.println("      '/../../.gnupg/gpg.conf' with a modified date in the near future could wipe your config,");
        System.err.println("       then copy it into the Sync2 folder once you fix it (and a further entry could move it into the server folder). Be careful.");
        System.err.println("You then have to input your sync-wide hostname.");
        System.err.println("Note that synchronization works by having a common medium, be it a mounted SFTP server or a pen drive.");
        System.err.println("It uses modification dates locally, but on the medium they are not needed.");
        System.err.println("The following directories must exist:");
        System.err.println("server: This folder is actually kept empty, and exists to be used as, say, a mountpoint for your storage, or a symlink.");
        System.err.println("server/index: Server-side indexes, used to determine if files were deleted locally and which files to transfer.");
        System.err.println("server/host.[hostname]: Remote uploaded data.");
        System.err.println("[hostname]: Local data.");
        System.err.println("Where [hostname] is the hostname you gave.");
        System.err.println("The following options are available:");
        System.err.println("help,--help,-h,-?,/?: Cause this text to be output on standard error output, then exit.");
        System.err.println("example: Show a basic usage example with descriptions to show what the program does, then exit.");
        System.err.println("standard: Standard console-based sync. You need this to make the program do anything.");
        System.err.println("noHost: Do not host any files.");
        System.err.println("        Useful if you want to update the indexes on the server, but download them so you can re-run sync and host on a drive.");
        System.err.println("gui: Allows precise control via a Swing GUI.");
    }

    private static void printExample() {
        Random r = new Random();
        String machineA = generateFIOName(r);
        String machineB = generateFIOName(r);
        System.err.println("Ok, so, say we have two directories on different machines we want to synchronize, ('" + machineA + "' and '" + machineB + "')");
        System.err.println(" and a shared but small and slow 'server' directory.");
        System.err.println("The two directories start off empty, and are filled up over time by synchronizations smaller than the server's capacity.");
        System.err.println("The server directory could be a mountpoint, or a symlink to somewhere in a mountpoint,");
        System.err.println(" or some other complicated situation. It's generally mountpoint-related, though.");
        System.err.println("The mountpoint could be a memory pen that you carry with you, or a real remote server via SFTP.");
        System.err.println("In any case, it's a medium on which files can stay for a while.");
        System.err.println("Firstly, let's set things up. (It's assumed you've already got the 'server' directory connected up.)");
        System.err.println(machineA + "$ mkdir " + machineA);
        System.err.println(machineA + "$ hostname | java -jar Sync2.jar Standard");
        System.err.println("<...>");
        System.err.println(machineB + "$ mkdir " + machineB);
        System.err.println(machineB + "$ hostname | java -jar Sync2.jar Standard");
        System.err.println("<...>");
        System.err.println("Ok, now the indexes have been initialized on both sides, when a sync occurs on a computer, it knows of the other computer.");
        System.err.println("Time to send a file!");
        System.err.println(machineA + "$ echo hello world > " + machineA + "/hello");
        System.err.println(machineA + "$ hostname | java -jar Sync2.jar Standard");
        System.err.println("<...>");
        System.err.println(machineB + "$ hostname | java -jar Sync2.jar Standard");
        System.err.println("<...>");
        System.err.println(machineB + "$ cat " + machineB + "/hello");
        System.err.println("hello world");
        System.err.println("... Pretty simple, right? Just remember one thing:");
        System.err.println("Never run the sync software on two computers on the same medium at the same time.");
        System.err.println("This *should* limit itself to destroying your indexes and maybe causing a mass upload.");
        System.err.println("That's if the safeties work, as if they don't, data loss is possible.");
    }

    private static String generateFIOName(Random r) {
        String[] parts = {
                "apple"   , "flutter", "banana", "cherry",  "lain", "light", "dark", "twilight", "vampire", "sparkle",
                "princess",     "shy",   "jack", "sparks", "roast", " fowl", "luna",     "moon",     "lua", "artemis"
        };
        return generateFIOPart(parts, r) + "-" + generateFIOPart(parts, r);
    }

    private static String generateFIOPart(String[] parts, Random r) {
        return parts[r.nextInt(parts.length)];
    }
}
