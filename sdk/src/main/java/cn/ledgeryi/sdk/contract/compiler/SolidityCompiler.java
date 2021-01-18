package cn.ledgeryi.sdk.contract.compiler;

import cn.ledgeryi.sdk.contract.compiler.entity.Result;
import com.google.common.base.Joiner;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class SolidityCompiler {

    public enum Options {
        AST("ast"),
        BIN("bin"),
        INTERFACE("interface"),
        ABI("abi");

        private String name;

        Options(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static class ParallelReader extends Thread {

        private InputStream stream;
        private StringBuilder content = new StringBuilder();

        ParallelReader(InputStream stream) {
            this.stream = stream;
        }

        public String getContent() {
            return getContent(true);
        }

        public synchronized String getContent(boolean waitForComplete) {
            if (waitForComplete) {
                while(stream != null) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            return content.toString();
        }

        public void run() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            } finally {
                synchronized (this) {
                    stream = null;
                    notifyAll();
                }
            }
        }
    }

    public static Result compile(byte[] source, boolean combinedJson, Options... options) throws IOException {
        List<String> commandParts = new ArrayList<>();
        commandParts.add(Solc.INSTANCE.getExecutable().getCanonicalPath());
        if (combinedJson) {
            commandParts.add("--combined-json");
            commandParts.add(Joiner.on(',').join(options));
            commandParts.add("-");
        } else {
            for (Options option : options) {
                commandParts.add("--" + option.getName());
                commandParts.add("-");
            }
        }

        ProcessBuilder processBuilder = new ProcessBuilder(commandParts)
                .directory(Solc.INSTANCE.getExecutable().getParentFile());
        processBuilder.environment().put("LD_LIBRARY_PATH",
                Solc.INSTANCE.getExecutable().getParentFile().getCanonicalPath());

        Process process = processBuilder.start();

        try (BufferedOutputStream stream = new BufferedOutputStream(process.getOutputStream())) {
            stream.write(source);
        }

        ParallelReader error = new ParallelReader(process.getErrorStream());
        ParallelReader output = new ParallelReader(process.getInputStream());
        error.start();
        output.start();

        try {
            process.waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return new Result(error.getContent(), output.getContent());
    }
}
