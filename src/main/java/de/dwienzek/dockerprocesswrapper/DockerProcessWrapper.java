package de.dwienzek.dockerprocesswrapper;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import sun.misc.Signal;

import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DockerProcessWrapper {

    public static void main(String[] args) throws IOException {
        OptionParser optionParser = new OptionParser();
        optionParser.accepts("help", "Show the help");
        optionParser.accepts("execute", "The command to execute").withRequiredArg();
        optionParser.accepts("directory", "The execute directory").withOptionalArg();
        optionParser.accepts("shutdown-command", "The shutdown command sent to the process").withRequiredArg();

        OptionSet optionSet = optionParser.parse(args);

        if (optionSet.has("help")) {
            optionParser.printHelpOn(System.out);
            return;
        }

        if (!optionSet.has("execute") || !optionSet.has("shutdown-command")) {
            System.out.println("Please enter the correct arguments.");
            System.out.println("Example: DockerProcessWrapper.jar --execute <command> --shutdown-command <shutdown-command> (--directory <directory>)");
            System.out.println("Use --help for help.");
            return;
        }

        String commandString = (String) optionSet.valueOf("execute");
        List<String> command = new ArrayList<>();
        if (commandString.contains(" ")) {
            command.addAll(Arrays.asList(commandString.split(" ")));
        } else {
            command.add(commandString);
        }

        Process process;
        ProcessBuilder processBuilder = new ProcessBuilder().redirectOutput(ProcessBuilder.Redirect.INHERIT).redirectError(ProcessBuilder.Redirect.INHERIT).command(command);
        if (optionSet.has("directory")) {
            processBuilder.directory(new File((String) optionSet.valueOf("directory")));
        }
        process = processBuilder.start();

        Thread thread = new Thread() {
            @Override
            public void run() {
                Console console = System.console();
                String line;
                while (!isInterrupted() && (line = console.readLine()) != null) {
                    try {
                        process.getOutputStream().write((line + "\n").getBytes());
                        process.getOutputStream().flush();
                    } catch (IOException exception) {
                        exception.printStackTrace();
                    }
                }
            }
        };
        thread.setDaemon(true);
        thread.start();

        Signal.handle(new Signal("TERM"), signal -> {
            try {
                process.getOutputStream().write((optionSet.valueOf("shutdown-command") + "\n").getBytes());
                process.getOutputStream().flush();
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        });

        try {
            process.waitFor();
        } catch (InterruptedException exception) {
            exception.printStackTrace();
        }
    }

}
