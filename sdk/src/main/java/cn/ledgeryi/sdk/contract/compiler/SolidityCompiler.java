package cn.ledgeryi.sdk.contract.compiler;

import cn.ledgeryi.sdk.contract.compiler.entity.Library;
import cn.ledgeryi.sdk.contract.compiler.entity.Result;
import com.google.common.base.Joiner;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class SolidityCompiler {

    public static Result compile(byte[] source, boolean combinedJson, Option... options) throws IOException {
        List<String> commandParts = new ArrayList<>();
        commandParts.add(Solc.getInstance().getExecutable().getCanonicalPath());
        if (combinedJson) {
            commandParts.add("--combined-json");
            commandParts.add(Joiner.on(',').join(options));
            commandParts.add("-");
        } else {
            for (Option option : options) {
                commandParts.add("--" + option.getName());
                commandParts.add("-");
            }
        }
        ProcessBuilder processBuilder = new ProcessBuilder(commandParts).directory(Solc.getInstance().getExecutable().getParentFile());
        processBuilder.environment().put("LD_LIBRARY_PATH", Solc.getInstance().getExecutable().getParentFile().getCanonicalPath());
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
        boolean success = process.exitValue() == 0;
        return new Result(error.getContent(), output.getContent(), success);
    }

    public static Result compileSrc(File source, boolean isNeedLibrary, Library library, Option... options) throws IOException {
        if (isNeedLibrary && library == null){
            throw new RuntimeException("need library, but library param is null");
        }
        List<String> commandParts = prepareCommandOptions(true, true, options);
        commandParts.add(source.getAbsolutePath().replace("\\","/"));
        if (isNeedLibrary){
            commandParts.add("--" + Options.LIBRARIES.getName());
            commandParts.add(library.toString());
        }
        if (!source.exists()) {
            throw new RuntimeException("contract file not exist in current path: " + source.getAbsolutePath());
        }
        ProcessBuilder processBuilder = new ProcessBuilder(commandParts).directory(Solc.getInstance().getExecutable().getParentFile());
        processBuilder.environment().put("LD_LIBRARY_PATH", Solc.getInstance().getExecutable().getParentFile().getCanonicalPath());
        Process process = processBuilder.start();
        ParallelReader error = new ParallelReader(process.getErrorStream());
        ParallelReader output = new ParallelReader(process.getInputStream());
        error.start();
        output.start();
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        boolean success = process.exitValue() == 0;

        return new Result(error.getContent(), output.getContent(), success);
    }

    private static List<String> prepareCommandOptions(boolean optimize, boolean combinedJson, Option... options) throws IOException {
        List<String> commandParts = new ArrayList<>();
        commandParts.add(Solc.getInstance().getExecutable().getCanonicalPath());
        if (optimize) {
            commandParts.add("--" + Options.OPTIMIZE.getName());
        }
        if (combinedJson) {
            Option combinedJsonOption = new Options.CombinedJson(getElementsOf(OutputOption.class, options));
            commandParts.add("--" + combinedJsonOption.getName());
            commandParts.add(combinedJsonOption.getValue());
        } else {
            for (Option option : getElementsOf(OutputOption.class, options)) {
                commandParts.add("--" + option.getName());
            }
        }
        for (Option option : getElementsOf(ListOption.class, options)) {
            commandParts.add("--" + option.getName());
            commandParts.add(option.getValue());
        }
        for (Option option : getElementsOf(CustomOption.class, options)) {
            commandParts.add("--" + option.getName());
            if (option.getValue() != null) {
                commandParts.add(option.getValue());
            }
        }

        return commandParts;
    }

    private static <T> List<T> getElementsOf(Class<T> clazz, Option... options) {
        return Arrays.stream(options).filter(clazz::isInstance).map(clazz::cast).collect(toList());
    }

    /**
     * This class is mainly here for backwards compatibility;
     * however we are now reusing it making it the solely public interface listing all the supported options.
     */
    public static final class Options {
        public static final OutputOption AST = OutputOption.AST;
        public static final OutputOption BIN = OutputOption.BIN;
        public static final OutputOption INTERFACE = OutputOption.INTERFACE;
        public static final OutputOption ABI = OutputOption.ABI;
        public static final OutputOption METADATA = OutputOption.METADATA;
        public static final OutputOption ASTJSON = OutputOption.ASTJSON;

        private static final NameOnlyOption OPTIMIZE = NameOnlyOption.OPTIMIZE;
        private static final NameOnlyOption VERSION = NameOnlyOption.VERSION;
        private static final NameOnlyOption LIBRARIES = NameOnlyOption.LIBRARIES;

        private static class CombinedJson extends ListOption {
            private CombinedJson(List values) {
                super("combined-json", values);
            }
        }
        public static class AllowPaths extends ListOption {
            public AllowPaths(List values) {
                super("allow-paths", values);
            }
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

    public interface Option extends Serializable {
        String getValue();
        String getName();
    }

    private static class ListOption implements Option {
        private String name;
        private List values;

        private ListOption(String name, List values) {
            this.name = name;
            this.values = values;
        }

        @Override
        public String getValue() {
            StringBuilder result = new StringBuilder();
            for (Object value : values) {
                if (OutputOption.class.isAssignableFrom(value.getClass())) {
                    result.append((result.length() == 0) ? ((OutputOption) value).getName() : ',' + ((OutputOption) value).getName());
                } else if (Path.class.isAssignableFrom(value.getClass())) {
                    result.append((result.length() == 0) ? ((Path) value).toAbsolutePath().toString() : ',' + ((Path) value).toAbsolutePath().toString());
                } else if (File.class.isAssignableFrom(value.getClass())) {
                    result.append((result.length() == 0) ? ((File) value).getAbsolutePath() : ',' + ((File) value).getAbsolutePath());
                } else if (String.class.isAssignableFrom(value.getClass())) {
                    result.append((result.length() == 0) ? value : "," + value);
                } else {
                    throw new UnsupportedOperationException("Unexpected type, value '" + value + "' cannot be retrieved.");
                }
            }
            return result.toString();
        }
        @Override
        public String getName() { return name; }
        @Override
        public String toString() { return name; }
    }

    private enum NameOnlyOption implements Option {
        OPTIMIZE("optimize"),
        VERSION("version"),
        LIBRARIES("libraries");

        private String name;

        NameOnlyOption(String name) {
            this.name = name;
        }

        @Override
        public String getValue() { return ""; }
        @Override
        public String getName() { return name; }
        @Override
        public String toString() {
            return name;
        }
    }

    private enum OutputOption implements Option {
        AST("ast"),
        BIN("bin"),
        INTERFACE("interface"),
        ABI("abi"),
        METADATA("metadata"),
        ASTJSON("ast-json");

        private String name;

        OutputOption(String name) {
            this.name = name;
        }

        @Override
        public String getValue() { return ""; }
        @Override
        public String getName() { return name; }
        @Override
        public String toString() {
            return name;
        }
    }

    public static class CustomOption implements Option {
        private String name;
        private String value;

        public CustomOption(String name) {
            if (name.startsWith("--")) {
                this.name = name.substring(2);
            } else {
                this.name = name;
            }
        }

        public CustomOption(String name, String value) {
            this(name);
            this.value = value;
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public String getName() {
            return name;
        }
    }

}
